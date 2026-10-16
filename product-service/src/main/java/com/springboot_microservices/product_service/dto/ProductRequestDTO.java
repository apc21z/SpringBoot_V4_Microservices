package com.springboot_microservices.product_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRequestDTO(
    @NotNull String name, 
    String description, 
    @NotNull @Positive double price
) {}
