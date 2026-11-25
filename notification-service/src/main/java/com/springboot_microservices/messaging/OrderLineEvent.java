package com.springboot_microservices.messaging;

public record OrderLineEvent(
        String skuCode,
        Integer quantity) {
}
