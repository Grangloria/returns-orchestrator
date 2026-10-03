package com.grangloria.inventory.repository;

import com.grangloria.inventory.entity.InventoryItem;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends ReactiveCrudRepository<InventoryItem, Long> {

    Mono<InventoryItem> findBySku(String sku);
}