package com.springboot_microservices.inventory_service.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryReservationRequestDTO(
        @NotNull @Positive Integer quantity) {
}
