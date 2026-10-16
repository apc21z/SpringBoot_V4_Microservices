package com.springboot_microservices.product_service.service;

import java.util.List;

import com.springboot_microservices.product_service.dto.ProductRequestDTO;
import com.springboot_microservices.product_service.dto.ProductResponseDTO;
import com.springboot_microservices.product_service.dto.mapper.ProductMapper;
import com.springboot_microservices.product_service.model.Product;
import com.springboot_microservices.product_service.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(
            ProductRepository productRepository,
            ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponseDTO> getProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponseDTO)
                .toList();
    }

    public ProductResponseDTO getProductById(String id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        return productMapper.toResponseDTO(product);
    }

    public ProductResponseDTO save(ProductRequestDTO productRequestDTO) {
        Product product = productMapper.toProduct(productRequestDTO);

        Product savedProduct = productRepository.save(product);

        return productMapper.toResponseDTO(savedProduct);
    }

    public ProductResponseDTO update(
            String id,
            ProductRequestDTO productRequestDTO) {

        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        product.setName(productRequestDTO.name());
        product.setDescription(productRequestDTO.description());
        product.setPrice(productRequestDTO.price());

        Product updatedProduct = productRepository.save(product);

        return productMapper.toResponseDTO(updatedProduct);
    }

    public void delete(String id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        productRepository.delete(product);
    }
}