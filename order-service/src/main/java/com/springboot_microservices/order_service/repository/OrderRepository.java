package com.springboot_microservices.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springboot_microservices.order_service.model.CustomerOrder;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
}
