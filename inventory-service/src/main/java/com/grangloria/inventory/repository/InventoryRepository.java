package com.grangloria.inventory.repository;

import com.grangloria.inventory.entity.InventoryItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends R2dbcRepository<InventoryItem, String> {

    /**
     * Find an item by its exact item_name (Primary Key).
     */
    Mono<InventoryItem> findByItemName(String itemName);
}