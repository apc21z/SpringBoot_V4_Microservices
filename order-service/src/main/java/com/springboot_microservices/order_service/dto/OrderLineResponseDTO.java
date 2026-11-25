package com.springboot_microservices.order_service.dto;

public record OrderLineResponseDTO(
        String skuCode,
        Integer quantity) {
}
