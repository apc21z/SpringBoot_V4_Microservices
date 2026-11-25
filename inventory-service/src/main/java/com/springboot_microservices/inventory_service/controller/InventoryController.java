package com.springboot_microservices.inventory_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springboot_microservices.inventory_service.model.dto.InventoryRequestDTO;
import com.springboot_microservices.inventory_service.model.dto.InventoryReservationRequestDTO;
import com.springboot_microservices.inventory_service.model.dto.InventoryResponseDTO;
import com.springboot_microservices.inventory_service.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponseDTO>> getInventories() {
        return ResponseEntity.ok(inventoryService.getInventories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryResponseDTO> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/sku/{skuCode}")
    public ResponseEntity<InventoryResponseDTO> getInventoryBySkuCode(@PathVariable String skuCode) {
        return ResponseEntity.ok(inventoryService.getInventoryBySkuCode(skuCode));
    }

    @PostMapping
    public ResponseEntity<InventoryResponseDTO> createInventory(
            @Valid @RequestBody InventoryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryResponseDTO> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRequestDTO request) {
        return ResponseEntity.ok(inventoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{skuCode}/reservations")
    public ResponseEntity<Void> reserve(
            @PathVariable String skuCode,
            @Valid @RequestBody InventoryReservationRequestDTO request) {
        inventoryService.reserve(skuCode, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{skuCode}/reservations/release")
    public ResponseEntity<Void> release(
            @PathVariable String skuCode,
            @Valid @RequestBody InventoryReservationRequestDTO request) {
        inventoryService.release(skuCode, request.quantity());
        return ResponseEntity.noContent().build();
    }
}
