package com.grangloria.gateway.messaging.producer;

import com.grangloria.gateway.config.KafkaTopicProperties;
import com.grangloria.gateway.event.ReturnLabelReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnLabelReadyEventPublisher
{

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public void publishReturnLabelReadyEvent(ReturnLabelReadyEvent event) {
        String topic = topicProperties.getReturnLabelReady();

        // Micrometer Tracing automatically attaches traceparent headers to kafkaTemplate calls
        kafkaTemplate.send(topic, event.customerEmail(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[KAFKA-PRODUCER] Successfully published ReturnLabelReadyEvent to topic [{}] for Customer: [{}]",
                                topic, event.customerEmail());
                    } else {
                        log.error("[KAFKA-PRODUCER-ERROR] Failed to publish ReturnLabelReadyEvent for Customer: [{}]",
                                event.customerEmail(), ex);
                    }
                });
    }
}