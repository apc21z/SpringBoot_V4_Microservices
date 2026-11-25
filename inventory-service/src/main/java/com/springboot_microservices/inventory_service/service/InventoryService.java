package com.springboot_microservices.inventory_service.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.springboot_microservices.inventory_service.model.Inventory;
import com.springboot_microservices.inventory_service.model.dto.InventoryRequestDTO;
import com.springboot_microservices.inventory_service.model.dto.InventoryResponseDTO;
import com.springboot_microservices.inventory_service.model.mapper.InventoryMapper;
import com.springboot_microservices.inventory_service.repository.InventoryRepository;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    public List<InventoryResponseDTO> getInventories() {
        return inventoryRepository.findAll()
                .stream()
                .map(inventoryMapper::toInventoryResponseDTO)
                .toList();
    }

    public InventoryResponseDTO getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inventario no encontrado"));
        return inventoryMapper.toInventoryResponseDTO(inventory);
    }

    public InventoryResponseDTO getInventoryBySkuCode(String skuCode) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode);
        if (inventory == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventario no encontrado");
        }
        return inventoryMapper.toInventoryResponseDTO(inventory);
    }

    @Transactional
    public InventoryResponseDTO save(InventoryRequestDTO inventoryRequestDTO) {
        if (inventoryRepository.existsBySkuCode(inventoryRequestDTO.skuCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El SKU ya existe");
        }
        Inventory inventory = inventoryMapper.toInventory(inventoryRequestDTO);
        Inventory savedInventory = inventoryRepository.save(inventory);
        return inventoryMapper.toInventoryResponseDTO(savedInventory);
    }

    @Transactional
    public InventoryResponseDTO update(Long id, InventoryRequestDTO inventoryRequestDTO) {
        Inventory inventory = inventoryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inventario no encontrado"));

        if (!inventory.getSkuCode().equals(inventoryRequestDTO.skuCode())
                && inventoryRepository.existsBySkuCodeAndIdNot(inventoryRequestDTO.skuCode(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El SKU ya existe");
        }
        inventory.setSkuCode(inventoryRequestDTO.skuCode());
        inventory.setQuantity(inventoryRequestDTO.quantity());
        inventory.setIsInStock(inventoryRequestDTO.isInStock());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return inventoryMapper.toInventoryResponseDTO(updatedInventory);
    }

    @Transactional
    public void delete(Long id) {
        Inventory inventory = inventoryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Inventario no encontrado"));
        inventoryRepository.delete(inventory);
    }

    @Transactional
    public void reserve(String skuCode, Integer quantity) {
        validateReservationQuantity(quantity);
        Inventory inventory = inventoryRepository.findBySkuCodeForUpdate(skuCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SKU no encontrado: " + skuCode));

        if (!Boolean.TRUE.equals(inventory.getIsInStock()) || inventory.getQuantity() < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Stock insuficiente para el SKU: " + skuCode);
        }

        int remaining = inventory.getQuantity() - quantity;
        inventory.setQuantity(remaining);
        inventory.setIsInStock(remaining > 0);
    }

    @Transactional
    public void release(String skuCode, Integer quantity) {
        validateReservationQuantity(quantity);
        Inventory inventory = inventoryRepository.findBySkuCodeForUpdate(skuCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SKU no encontrado: " + skuCode));

        int restored;
        try {
            restored = Math.addExact(inventory.getQuantity(), quantity);
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La cantidad restaurada excede el límite permitido", ex);
        }
        inventory.setQuantity(restored);
        inventory.setIsInStock(restored > 0);
    }

    private void validateReservationQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor que cero");
        }
    }
}
