package com.springboot_microservices.order_service.dto;

import java.util.List;

public record OrderResponseDTO(
        Long id,
        String customerEmail,
        List<OrderLineResponseDTO> items,
        String status) {
}
