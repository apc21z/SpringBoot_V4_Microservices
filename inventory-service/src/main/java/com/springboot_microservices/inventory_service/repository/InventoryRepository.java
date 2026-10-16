package com.springboot_microservices.inventory_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springboot_microservices.inventory_service.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Inventory findBySkuCode(String skuCode);
    
}
