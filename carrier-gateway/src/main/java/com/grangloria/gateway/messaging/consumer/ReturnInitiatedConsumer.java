package com.grangloria.gateway.messaging.consumer;

import com.grangloria.gateway.dto.request.LabelRequest;
import com.grangloria.gateway.event.ReturnInitiatedEvent;
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

        LabelRequest labelRequest = new LabelRequest(event.customerEmail(), event.orderId(), event.zipCode(), event.quantity());

        carrierGatewayService.processCarrierLabelCreation(labelRequest)
                .subscribe(
                        null,
                        error -> log.error("[CARRIER-GATEWAY-ERROR] Failed generating label for Order: [{}]. Reason: {}",
                                event.orderId(), error.getMessage())
                );
    }
}