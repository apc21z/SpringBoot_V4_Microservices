package com.springboot_microservices.product_service.controller;

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

import com.springboot_microservices.product_service.dto.ProductRequestDTO;
import com.springboot_microservices.product_service.dto.ProductResponseDTO;
import com.springboot_microservices.product_service.service.ProductService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET: Obtener todos los productos
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    // GET: Obtener un producto por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(
            @PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // POST: Crear un producto
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO productRequestDTO) {

        ProductResponseDTO product = productService.save(productRequestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(product);
    }

    // PUT: Actualizar un producto
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequestDTO productRequestDTO) {

        ProductResponseDTO product = productService.update(id, productRequestDTO);

        return ResponseEntity.ok(product);
    }

    // DELETE: Eliminar un producto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
