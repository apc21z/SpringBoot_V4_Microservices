package com.springboot_microservices.product_service.dto;

public record ProductResponseDTO(
    String id,
    String name,
    String description,
    double price
) {}