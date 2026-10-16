package com.springboot_microservices.inventory_service.model.dto;

public record InventoryResponseDTO (
    Long id,
    String skuCode,
    Integer quantity,
    Boolean isInStock
){
    
}
