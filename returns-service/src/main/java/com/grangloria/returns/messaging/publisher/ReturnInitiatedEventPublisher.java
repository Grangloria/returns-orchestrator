package com.grangloria.returns.messaging.publisher;

import com.grangloria.returns.event.ReturnInitiatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReturnInitiatedEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ReturnInitiatedEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.return-initiated}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishReturnInitiatedEvent(ReturnInitiatedEvent event) {
        kafkaTemplate.send(topic, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[KAFKA-PRODUCER] Successfully published ReturnLabelReadyEvent to topic [{}] for Order ID: [{}]",
                                topic, event.orderId());
                    } else {
                        log.error("[KAFKA-PRODUCER-ERROR] Failed to publish ReturnInitiatedEvent for Order ID: [{}]",
                                event.orderId(), ex);
                    }
                });
    }
}