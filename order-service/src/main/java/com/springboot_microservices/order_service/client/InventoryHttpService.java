package com.springboot_microservices.order_service.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PutExchange;

import com.springboot_microservices.order_service.dto.InventoryReservationRequestDTO;

import reactor.core.publisher.Mono;

public interface InventoryHttpService {

    @PutExchange("/api/inventory/{skuCode}/reservations")
    Mono<Void> reserve(
            @PathVariable("skuCode") String skuCode,
            @RequestBody InventoryReservationRequestDTO request);

    @PutExchange("/api/inventory/{skuCode}/reservations/release")
    Mono<Void> release(
            @PathVariable("skuCode") String skuCode,
            @RequestBody InventoryReservationRequestDTO request);
}
