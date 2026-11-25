package com.springboot_microservices.notification_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.springboot_microservices.messaging.OrderCreatedEvent;
import com.springboot_microservices.notification_service.service.OrderEmailService;

@Component
public class OrderCreatedListener {

    private final OrderEmailService orderEmailService;

    public OrderCreatedListener(OrderEmailService orderEmailService) {
        this.orderEmailService = orderEmailService;
    }

    @RabbitListener(queues = "notification.order-created")
    public void onOrderCreated(OrderCreatedEvent event) {
        orderEmailService.sendOrderConfirmation(event);
    }
}
