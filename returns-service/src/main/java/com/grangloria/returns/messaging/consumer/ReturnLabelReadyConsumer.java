package com.grangloria.returns.messaging.consumer;

import com.grangloria.returns.event.ReturnLabelReadyEvent;
import com.grangloria.returns.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnLabelReadyConsumer {

    private final ReturnService returnService;

    @KafkaListener(
            topics = "${kafka.topics.return-label-ready:returns.label.ready.v1}",
            groupId = "${spring.kafka.consumer.group-id:return-service-group}"
    )
    public void consumeLabelReadyEvent(ReturnLabelReadyEvent event) {
        log.info("[RETURN-SERVICE-KAFKA] Consumed ReturnLabelReadyEvent for Order ID: [{}]", event.orderId());

        // Execute the reactive update pipeline
        returnService.handleLabelGenerated(event)
                .doOnSuccess(v -> log.info("[RETURN-SERVICE-KAFKA] Successfully processed ReturnLabelReadyEvent for Order ID: [{}]", event.orderId()))
                .doOnError(err -> log.error("[RETURN-SERVICE-KAFKA-ERROR] Failed to process ReturnLabelReadyEvent for Order ID: [{}]. Reason: {}",
                        event.orderId(), err.getMessage(), err))
                .block(); // Block here to ensure Spring Kafka commits offset after DB update completes
    }
}