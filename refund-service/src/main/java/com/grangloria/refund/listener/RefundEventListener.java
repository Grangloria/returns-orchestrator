package com.grangloria.refund.listener;

import com.returns.common.event.InventoryRestockedEvent;
import com.returns.common.event.RefundCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.refund-completed:returns.refund.completed.v1}")
    private String refundCompletedTopic;

    @KafkaListener(
            topics = "${kafka.topics.inventory-restocked:returns.inventory.restocked.v1}",
            groupId = "${spring.kafka.consumer.group-id:refund-service-group}",
            properties = {"spring.json.value.default.type=com.returns.common.event.InventoryRestockedEvent"}
    )
    public void processRefund(InventoryRestockedEvent event) {
        if (event == null) {
            log.warn("[REFUND-SERVICE] Received null or un-parsable InventoryRestockedEvent envelope.");
            return;
        }

        log.info("[REFUND-SERVICE] Consumed InventoryRestockedEvent for Return ID: [{}], Order ID: [{}]. Initiating payout ($%.2f)...",
                event.returnId(), event.orderId(), event.refundAmount());

        // Simulate payment gateway transaction ID
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RefundCompletedEvent refundEvent = new RefundCompletedEvent(
                event.returnId(),
                event.orderId(),
                event.customerEmail(),
                event.refundAmount(),
                transactionId,
                Instant.now()
        );

        kafkaTemplate.send(refundCompletedTopic, event.orderId(), refundEvent);

        log.info("[REFUND-SERVICE] Payout successful for Order ID: [{}]. Transaction ID: [{}]. Emitted 'refund-completed' event.",
                event.orderId(), transactionId);
    }
}