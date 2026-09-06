package com.grangloria.inventory.service;

import com.grangloria.inventory.entity.InventoryItem;
import com.grangloria.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Mono<InventoryItem> updateQuantity(String itemName, Integer newQuantity) {
        return inventoryRepository.findById(itemName)
                .flatMap(item -> {
                    item.setQuantity(newQuantity);
                    return inventoryRepository.save(item);
                });
    }
}