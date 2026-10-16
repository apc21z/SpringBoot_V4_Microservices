package com.springboot_microservices.inventory_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service 
@Transactional (readOnly = true)
public class InventoryService {
    
}
