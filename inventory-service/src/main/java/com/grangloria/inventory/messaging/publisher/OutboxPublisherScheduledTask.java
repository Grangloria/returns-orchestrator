package com.grangloria.inventory.messaging.publisher;

import com.grangloria.inventory.entity.OutboxEvent;
import com.grangloria.inventory.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisherScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduledTask.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.inventory-restocked:returns.inventory.restocked.v1}")
    private String topic;

    public OutboxPublisherScheduledTask(OutboxRepository outboxRepository,
                                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void processPendingOutboxMessages() {
        List<OutboxEvent> pendingMessages = outboxRepository
                .findTop50ByStatusOrderByCreatedAtAsc("PENDING")
                .collectList()
                .block();

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        for (OutboxEvent message : pendingMessages) {
            try {
                kafkaTemplate.send(topic, message.getAggregateId(), message.getPayload()).get();
                message.setStatus("PROCESSED");
                outboxRepository.save(message).block();
                log.info("[INVENTORY-OUTBOX] Successfully published outbox event [{}] to topic [{}]", message.getId(), topic);
            } catch (Exception e) {
                log.error("[INVENTORY-OUTBOX] Failed to publish outbox event [{}]. Halting batch processing.", message.getId(), e);
                break; // Stop execution on failure to preserve strict event sequencing
            }
        }
    }
}