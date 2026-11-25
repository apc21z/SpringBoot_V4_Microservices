package com.springboot_microservices.order_service.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.springboot_microservices.order_service.dto.InventoryReservationRequestDTO;

@Component
public class InventoryClient {

    private final WebClient webClient;

    public InventoryClient(WebClient inventoryWebClient) {
        this.webClient = inventoryWebClient;
    }

    public void reserve(String skuCode, int quantity) {
        postReservation(skuCode, quantity, false);
    }

    public void release(String skuCode, int quantity) {
        postReservation(skuCode, quantity, true);
    }

    private void postReservation(String skuCode, int quantity, boolean release) {
        String path = release
                ? "/api/inventory/{skuCode}/reservations/release"
                : "/api/inventory/{skuCode}/reservations";

        try {
            webClient.post()
                    .uri(path, skuCode)
                    .bodyValue(new InventoryReservationRequestDTO(quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
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
