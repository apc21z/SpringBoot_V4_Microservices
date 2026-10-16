package com.springboot_microservices.product_service.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.springboot_microservices.product_service.dto.ProductRequestDTO;
import com.springboot_microservices.product_service.dto.ProductResponseDTO;
import com.springboot_microservices.product_service.model.Product;

@Mapper(componentModel = "spring") 
public interface ProductMapper {

    @Mapping (target = "id", ignore = true)
    Product toProduct(ProductRequestDTO productRequestDTO);

    ProductResponseDTO toResponseDTO(Product product);
}
