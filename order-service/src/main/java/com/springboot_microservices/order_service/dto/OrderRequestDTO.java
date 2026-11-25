package com.springboot_microservices.order_service.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record OrderRequestDTO(
        @NotBlank @Email String customerEmail,
        @NotEmpty List<@Valid OrderLineRequestDTO> items,
        @NotBlank String status) {
}
