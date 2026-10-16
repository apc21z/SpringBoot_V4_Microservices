package com.springboot_microservices.inventory_service.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.springboot_microservices.inventory_service.model.Inventory;
import com.springboot_microservices.inventory_service.model.dto.InventoryRequestDTO;
import com.springboot_microservices.inventory_service.model.dto.InventoryResponseDTO;

@Mapper (componentModel = "spring")
public interface InventoryMapper {
    
    @Mapping (target="id", ignore = true)
    Inventory toInventory (InventoryRequestDTO inventoryRequestDTO);

    InventoryResponseDTO toInventoryResponseDTO (Inventory inventory);

}
