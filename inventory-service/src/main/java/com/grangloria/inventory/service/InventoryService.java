package com.grangloria.inventory.service;

import com.grangloria.inventory.entity.InventoryItem;
import com.grangloria.inventory.exception.InventoryException;
import com.grangloria.inventory.exception.ItemNotFoundException;
import com.grangloria.inventory.repository.InventoryRepository;
import com.returns.common.event.InventoryRestockedEvent;
import com.returns.common.event.PackageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.inventory-restocked:returns.inventory.restocked.v1}")
    private String inventoryRestockedTopic;

    public InventoryService(InventoryRepository inventoryRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Handles package reception: restocks database inventory and emits InventoryRestockedEvent downstream to refund-service.
     */
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
                .doOnSuccess(savedItem -> {
                    if (savedItem != null) {
                        double refundAmount = 29.99 * event.quantity(); // Unit cost calculation

                        InventoryRestockedEvent restockedEvent = new InventoryRestockedEvent(
                                event.returnId() != null ? event.returnId() : "RET-" + event.orderId(),
                                event.orderId(),
                                "customer@example.com",
                                event.sku(),
                                event.quantity(),
                                refundAmount,
                                Instant.now()
                        );

                        kafkaTemplate.send(inventoryRestockedTopic, event.orderId(), restockedEvent);
                        log.info("[INVENTORY-SERVICE] Stock updated & InventoryRestockedEvent emitted for Order ID: [{}]", event.orderId());
                    }
                })
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