package com.springboot_microservices.inventory_service.model.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryRequestDTO(
    @NotBlank 
    String skuCode,

    @NotNull 
    @PositiveOrZero 
    Integer quantity,

    @NotNull
    Boolean isInStock

) {
}