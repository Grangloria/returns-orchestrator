package com.grangloria.gateway.messaging.consumer;

import com.grangloria.gateway.dto.request.LabelRequest;
import com.returns.common.event.ReturnInitiatedEvent;
import com.grangloria.gateway.service.CarrierGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnInitiatedConsumer {

    private final CarrierGatewayService carrierGatewayService;

    @KafkaListener(
            topics = "${kafka.topics.return-initiated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ReturnInitiatedEvent event) {
        log.info("[CARRIER-GATEWAY-CONSUMER] Consumed ReturnInitiatedEvent for Order: [{}]", event.orderId());

        LabelRequest labelRequest = new LabelRequest(
                event.customerEmail(),
                event.orderId(),
                event.zipCode(),
                event.quantity(),
                event.sku()
        );

        // .block() propagates Mono.error() to Spring Kafka's CommonErrorHandler
        carrierGatewayService.processCarrierLabelCreation(labelRequest).block();

        log.info("[CARRIER-GATEWAY-CONSUMER] Successfully created return label for Order: [{}]", event.orderId());
    }
}