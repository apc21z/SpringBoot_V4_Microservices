package com.springboot_microservices.order_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderLine {

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(nullable = false)
    private Integer quantity;
}
