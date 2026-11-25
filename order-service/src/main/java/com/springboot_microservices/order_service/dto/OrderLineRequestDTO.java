package com.springboot_microservices.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequestDTO(
        @NotBlank String skuCode,
        @NotNull @Positive Integer quantity) {
}
