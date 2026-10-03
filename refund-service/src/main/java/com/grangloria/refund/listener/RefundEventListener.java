package com.grangloria.refund.listener;

import com.returns.common.event.InventoryRestockedEvent;
import com.returns.common.event.RefundCompletedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class RefundEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String refundCompletedTopic;

    public RefundEventListener(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.refund-completed}") String refundCompletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.refundCompletedTopic = refundCompletedTopic;
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-restocked}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void processRefund(InventoryRestockedEvent event) {
        System.out.printf("[REFUND SERVICE] Initiating payout for Return ID: %s (Amount: $%.2f)...%n",
                event.returnId(), event.refundAmount());

        // Simulate payment gateway API transaction
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RefundCompletedEvent refundEvent = new RefundCompletedEvent(
                event.returnId(),
                event.customerEmail(),
                event.refundAmount(),
                transactionId,
                Instant.now()
        );

        kafkaTemplate.send(refundCompletedTopic, event.returnId(), refundEvent);
        System.out.printf("[REFUND SERVICE] Payout successful. Transaction ID: %s. Emitted 'refund-completed'%n", transactionId);
    }
}