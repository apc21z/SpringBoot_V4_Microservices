package com.springboot_microservices.order_service.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.server.ResponseStatusException;

import com.springboot_microservices.order_service.dto.InventoryReservationRequestDTO;

@Component
public class InventoryClient {

    private final InventoryHttpService inventoryHttpService;

    public InventoryClient(WebClient inventoryWebClient) {
        this.inventoryHttpService = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(inventoryWebClient))
                .build()
                .createClient(InventoryHttpService.class);
    }

    public void reserve(String skuCode, int quantity) {
        putReservation(skuCode, quantity, false);
    }

    public void release(String skuCode, int quantity) {
        putReservation(skuCode, quantity, true);
    }

    private void putReservation(String skuCode, int quantity, boolean release) {
        try {
            InventoryReservationRequestDTO request = new InventoryReservationRequestDTO(quantity);
            if (release) {
                inventoryHttpService.release(skuCode, request).block();
            } else {
                inventoryHttpService.reserve(skuCode, request).block();
            }
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(
                        ex.getStatusCode(), "Inventory rechazó la operación para el SKU " + skuCode);
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Inventory no pudo procesar la operación", ex);
        } catch (WebClientRequestException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "No se pudo contactar con Inventory Service", ex);
        }
    }
}
