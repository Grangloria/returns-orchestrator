package com.grangloria.gateway.service;

import com.grangloria.gateway.client.CarrierClient;
import com.grangloria.gateway.dto.request.LabelRequest;
import com.grangloria.gateway.messaging.producer.ReturnLabelReadyEventPublisher;
import com.returns.common.event.ReturnLabelReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierGatewayService {

    private final CarrierClient carrierClient;
    private final ReturnLabelReadyEventPublisher labelGeneratedPublisher;

    public Mono<Void> processCarrierLabelCreation(LabelRequest request) {
        log.info("[CARRIER-GATEWAY-SERVICE] Requesting 3PL carrier label for Order: [{}]", request.orderId());

        return carrierClient.requestLabel(request)
                .flatMap(labelUrl -> {
                    log.info("[CARRIER-GATEWAY-SERVICE] Carrier label generated! Order ID: [{}]", request.orderId());

                    String returnId = "RET-" + request.orderId();
                    String trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                    ReturnLabelReadyEvent generatedEvent = new ReturnLabelReadyEvent(
                            returnId,
                            request.orderId(),
                            request.customerEmail(),
                            trackingNumber,
                            labelUrl,
                            "MOCK-CARRIER",
                            request.sku(),
                            request.quantity(),
                            Instant.now()
                    );

                    labelGeneratedPublisher.publishReturnLabelReadyEvent(generatedEvent);
                    return Mono.empty();
                });
    }
}