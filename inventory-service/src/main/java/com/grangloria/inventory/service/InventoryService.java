package com.grangloria.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grangloria.inventory.entity.InventoryItem;
import com.grangloria.inventory.entity.OutboxEvent;
import com.grangloria.inventory.exception.ItemNotFoundException;
import com.grangloria.inventory.repository.InventoryRepository;
import com.grangloria.inventory.repository.OutboxRepository;
import com.returns.common.event.InventoryRestockedEvent;
import com.returns.common.event.PackageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public InventoryService(InventoryRepository inventoryRepository,
                            OutboxRepository outboxRepository,
                            ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Handles package reception: restocks database inventory and persists an OutboxMessage
     * within a single reactive database transaction to avoid dual-writes.
     */
    @Transactional
    public Mono<Void> restockAndEmitInventoryRestocked(PackageReceivedEvent event) {
        return inventoryRepository.findBySku(event.sku())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new IllegalArgumentException("SKU not found in database: " + event.sku()))))
                .flatMap(item -> {
                    int previousQuantity = item.getQuantity();
                    item.setQuantity(previousQuantity + event.quantity());

                    log.info("[INVENTORY-SERVICE] Restocking SKU [{}]. Previous: [{}], Restocking: [{}], New Total: [{}]",
                            event.sku(), previousQuantity, event.quantity(), item.getQuantity());

                    return inventoryRepository.save(item);
                })
                .flatMap(savedItem -> {
                    BigDecimal refundAmount = BigDecimal.valueOf(29.99).multiply(BigDecimal.valueOf(event.quantity()));

                    InventoryRestockedEvent restockedEvent = new InventoryRestockedEvent(
                            event.returnId() != null ? event.returnId() : "RET-" + event.orderId(),
                            event.orderId(),
                            "customer@example.com",
                            event.sku(),
                            event.quantity(),
                            refundAmount,
                            Instant.now()
                    );

                    try {
                        String payloadJson = objectMapper.writeValueAsString(restockedEvent);

                        OutboxEvent outbox = new OutboxEvent();
                        outbox.setAggregateType("InventoryItem");
                        outbox.setAggregateId(event.sku());
                        outbox.setType("INVENTORY_RESTOCKED");
                        outbox.setPayload(payloadJson);
                        outbox.setStatus("PENDING");
                        outbox.setCreatedAt(LocalDateTime.now());

                        return outboxRepository.save(outbox);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Failed to serialize Outbox event payload", e));
                    }
                })
                .doOnSuccess(outboxRecord -> log.info("[INVENTORY-SERVICE] Stock updated & Outbox message persisted for Order ID: [{}]", event.orderId()))
                .then();
    }

    public Mono<InventoryItem> restockItem(String sku, Integer quantityToRestock) {
        return inventoryRepository.findBySku(sku)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new IllegalArgumentException("SKU not found in database: " + sku))))
                .flatMap(item -> {
                    int previousQuantity = item.getQuantity();
                    item.setQuantity(previousQuantity + quantityToRestock);

                    log.info("[INVENTORY-SERVICE] Restocking SKU [{}]. Previous: [{}], Restocking: [{}], New Total: [{}]",
                            sku, previousQuantity, quantityToRestock, item.getQuantity());

                    return inventoryRepository.save(item);
                });
    }

    public Mono<InventoryItem> updateQuantity(String sku, Integer newQuantity) {
        return inventoryRepository.findBySku(sku)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new IllegalArgumentException("SKU not found in database: " + sku))))
                .flatMap(item -> {
                    item.setQuantity(newQuantity);
                    return inventoryRepository.save(item);
                });
    }

    public Mono<InventoryItem> getItemBySku(String sku) {
        return inventoryRepository.findBySku(sku)
                .switchIfEmpty(Mono.error(new ItemNotFoundException("Inventory item not found for SKU: " + sku)));
    }
}