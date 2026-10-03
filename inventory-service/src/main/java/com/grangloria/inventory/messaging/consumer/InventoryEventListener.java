package com.grangloria.inventory.messaging.consumer;

import com.grangloria.inventory.service.InventoryService;
import com.returns.common.event.InventoryRestockedEvent;
import com.returns.common.event.PackageReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.inventory-restocked:returns.inventory.restocked.v1}")
    private String inventoryRestockedTopic;

    @KafkaListener(
            topics = "${kafka.topics.package-received:returns.package.received.v1}",
            groupId = "${spring.kafka.consumer.group-id:inventory-service-group}",
            properties = {"spring.json.value.default.type=com.returns.common.event.PackageReceivedEvent"}
    )
    public void handlePackageReceived(PackageReceivedEvent event) {
        if (event == null || event.sku() == null) {
            log.warn("[INVENTORY-EVENT] Received null or un-parsable PackageReceivedEvent envelope.");
            return;
        }

        log.info("[INVENTORY-EVENT] Consumed PackageReceivedEvent for Return ID: [{}], Order ID: [{}], SKU: [{}]",
                event.returnId(), event.orderId(), event.sku());

        inventoryService.restockItem(event.sku(), event.quantity())
                .flatMap(updatedItem -> {
                    double calculatedRefund = 29.99 * event.quantity();
                    String customerEmail = "customer@example.com";

                    // Aligns with the 6-argument constructor:
                    // (returnId, orderId, newStockQuantity, refundAmount, customerEmail, restockedAt)
                    InventoryRestockedEvent restockedEvent = new InventoryRestockedEvent(
                            event.returnId() != null ? event.returnId() : "RET-" + event.orderId(), // 1. returnId
                            event.orderId(),                                                       // 2. orderId
                            customerEmail,                                                         // 3. customerEmail
                            event.sku(),                                                           // 4. sku
                            updatedItem.getQuantity(),                                             // 5. quantity / newStockQuantity
                            calculatedRefund,                                                      // 6. refundAmount
                            Instant.now()                                                          // 7. restockedAt
                    );

                    log.info("[INVENTORY-EVENT] SKU [{}] restocked to [{}]. Publishing event to topic [{}]",
                            updatedItem.getSku(), updatedItem.getQuantity(), inventoryRestockedTopic);

                    return Mono.fromFuture(kafkaTemplate.send(inventoryRestockedTopic, event.returnId(), restockedEvent));
                })
                .subscribe(
                        sendResult -> log.info("[INVENTORY-EVENT] Successfully published InventoryRestockedEvent for Return ID: [{}]", event.returnId()),
                        error -> log.error("[INVENTORY-EVENT] Error processing package received event for Return ID: [{}]", event.returnId(), error)
                );
    }
}