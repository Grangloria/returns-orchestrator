package com.grangloria.inventory.controller;

import com.grangloria.inventory.entity.InventoryItem;
import com.grangloria.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/items/{sku}")
    public Mono<InventoryItem> getItemBySku(@PathVariable String sku) {
        return inventoryService.getItemBySku(sku);
    }
}