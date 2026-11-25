package com.springboot_microservices.order_service.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.springboot_microservices.order_service.client.InventoryClient;
import com.springboot_microservices.order_service.config.OrderMessagingConfiguration;
import com.springboot_microservices.messaging.OrderCreatedEvent;
import com.springboot_microservices.messaging.OrderLineEvent;
import com.springboot_microservices.order_service.dto.OrderLineRequestDTO;
import com.springboot_microservices.order_service.dto.OrderLineResponseDTO;
import com.springboot_microservices.order_service.dto.OrderRequestDTO;
import com.springboot_microservices.order_service.dto.OrderResponseDTO;
import com.springboot_microservices.order_service.model.CustomerOrder;
import com.springboot_microservices.order_service.model.OrderLine;
import com.springboot_microservices.order_service.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final TransactionTemplate transactionTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            TransactionTemplate transactionTemplate,
            RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.transactionTemplate = transactionTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        return toResponseDTO(findOrder(id));
    }

    public OrderResponseDTO save(OrderRequestDTO request) {
        List<StockAdjustment> appliedAdjustments = applyStockAdjustments(
                stockAdjustments(Map.of(), requestedQuantities(request)));
        OrderResponseDTO savedOrder;
        try {
            savedOrder = transactionTemplate.execute(status -> {
                CustomerOrder order = new CustomerOrder();
                applyRequest(order, request);
                return toResponseDTO(orderRepository.save(order));
            });
        } catch (RuntimeException ex) {
            compensate(appliedAdjustments, ex);
            throw ex;
        }

        try {
            CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
            rabbitTemplate.convertAndSend(
                    OrderMessagingConfiguration.ORDERS_EXCHANGE,
                    OrderMessagingConfiguration.ORDER_CREATED_ROUTING_KEY,
                    toOrderCreatedEvent(savedOrder),
                    correlationData);
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new AmqpException("RabbitMQ rechazó el evento: " + confirm.getReason());
            }
        } catch (AmqpException | ExecutionException | TimeoutException ex) {
            log.error("El pedido {} se guardó, pero no se pudo publicar su evento", savedOrder.id(), ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El pedido se guardó, pero no se pudo publicar la notificación",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Se interrumpió la publicación del evento del pedido {}", savedOrder.id(), ex);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Se interrumpió la publicación de la notificación",
                    ex);
        }
        return savedOrder;
    }

    public OrderResponseDTO update(Long id, OrderRequestDTO request) {
        OrderSnapshot before = getOrderSnapshot(id);
        List<StockAdjustment> appliedAdjustments = applyStockAdjustments(
                stockAdjustments(before.quantities(), requestedQuantities(request)));
        try {
            return transactionTemplate.execute(status -> {
                CustomerOrder order = findOrder(id);
                if (!Objects.equals(order.getVersion(), before.version())) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "El pedido cambió durante la actualización");
                }
                applyRequest(order, request);
                return toResponseDTO(orderRepository.save(order));
            });
        } catch (RuntimeException ex) {
            compensate(appliedAdjustments, ex);
            throw ex;
        }
    }

    public void delete(Long id) {
        OrderSnapshot before = getOrderSnapshot(id);
        List<StockAdjustment> appliedAdjustments = applyStockAdjustments(
                stockAdjustments(before.quantities(), Map.of()));
        try {
            transactionTemplate.executeWithoutResult(status -> {
                CustomerOrder order = findOrder(id);
                if (!Objects.equals(order.getVersion(), before.version())) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "El pedido cambió durante la eliminación");
                }
                orderRepository.delete(order);
            });
        } catch (RuntimeException ex) {
            compensate(appliedAdjustments, ex);
            throw ex;
        }
    }

    private OrderSnapshot getOrderSnapshot(Long id) {
        return transactionTemplate.execute(status -> {
            CustomerOrder order = findOrder(id);
            return new OrderSnapshot(order.getVersion(), quantities(order.getItems()));
        });
    }

    private CustomerOrder findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    private void applyRequest(CustomerOrder order, OrderRequestDTO request) {
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(request.status());
        order.setItems(request.items().stream()
                .map(this::toOrderLine)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
    }

    private Map<String, Integer> requestedQuantities(OrderRequestDTO request) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        request.items().forEach(item -> addQuantity(quantities, item.skuCode(), item.quantity()));
        return quantities;
    }

    private Map<String, Integer> quantities(List<OrderLine> items) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        items.forEach(item -> addQuantity(quantities, item.getSkuCode(), item.getQuantity()));
        return quantities;
    }

    private void addQuantity(Map<String, Integer> quantities, String skuCode, int quantity) {
        try {
            quantities.merge(skuCode, quantity, Math::addExact);
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La cantidad total para el SKU excede el límite permitido", ex);
        }
    }

    private List<StockAdjustment> stockAdjustments(
            Map<String, Integer> previous,
            Map<String, Integer> requested) {
        List<StockAdjustment> adjustments = new ArrayList<>();
        requested.forEach((skuCode, quantity) -> {
            int increase = quantity - previous.getOrDefault(skuCode, 0);
            if (increase > 0) {
                adjustments.add(new StockAdjustment(skuCode, increase, true));
            }
        });
        previous.forEach((skuCode, quantity) -> {
            int decrease = quantity - requested.getOrDefault(skuCode, 0);
            if (decrease > 0) {
                adjustments.add(new StockAdjustment(skuCode, decrease, false));
            }
        });
        return adjustments;
    }

    private List<StockAdjustment> applyStockAdjustments(List<StockAdjustment> adjustments) {
        List<StockAdjustment> applied = new ArrayList<>();
        try {
            for (StockAdjustment adjustment : adjustments) {
                if (adjustment.reserve()) {
                    inventoryClient.reserve(adjustment.skuCode(), adjustment.quantity());
                    applied.add(adjustment);
                }
            }
            for (StockAdjustment adjustment : adjustments) {
                if (!adjustment.reserve()) {
                    inventoryClient.release(adjustment.skuCode(), adjustment.quantity());
                    applied.add(adjustment);
                }
            }
        } catch (RuntimeException ex) {
            compensate(applied, ex);
            throw ex;
        }
        return applied;
    }

    private void compensate(List<StockAdjustment> adjustments, RuntimeException originalFailure) {
        boolean compensationFailed = false;
        for (int index = adjustments.size() - 1; index >= 0; index--) {
            StockAdjustment adjustment = adjustments.get(index);
            try {
                if (adjustment.reserve()) {
                    inventoryClient.release(adjustment.skuCode(), adjustment.quantity());
                } else {
                    inventoryClient.reserve(adjustment.skuCode(), adjustment.quantity());
                }
            } catch (RuntimeException compensationFailure) {
                compensationFailed = true;
                originalFailure.addSuppressed(compensationFailure);
                log.error("No se pudo compensar el inventario del SKU {}", adjustment.skuCode(),
                        compensationFailure);
            }
        }
        if (compensationFailed) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falló la compensación del inventario; es necesaria una revisión manual",
                    originalFailure);
        }
    }

    private OrderLine toOrderLine(OrderLineRequestDTO request) {
        return new OrderLine(request.skuCode(), request.quantity());
    }

    private OrderResponseDTO toResponseDTO(CustomerOrder order) {
        List<OrderLineResponseDTO> items = order.getItems().stream()
                .map(item -> new OrderLineResponseDTO(item.getSkuCode(), item.getQuantity()))
                .toList();
        return new OrderResponseDTO(order.getId(), order.getCustomerEmail(), items, order.getStatus());
    }

    private OrderCreatedEvent toOrderCreatedEvent(OrderResponseDTO order) {
        List<OrderLineEvent> items = order.items().stream()
                .map(item -> new OrderLineEvent(item.skuCode(), item.quantity()))
                .toList();
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "order.created",
                Instant.now().toString(),
                order.id(),
                order.customerEmail(),
                items);
    }

    private record OrderSnapshot(Long version, Map<String, Integer> quantities) {
    }

    private record StockAdjustment(String skuCode, int quantity, boolean reserve) {
    }
}
