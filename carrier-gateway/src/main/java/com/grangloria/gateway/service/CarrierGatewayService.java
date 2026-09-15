package com.grangloria.gateway.service;

import com.grangloria.gateway.client.CarrierClient;
import com.grangloria.gateway.dto.request.LabelRequest;
import com.grangloria.gateway.event.ReturnInitiatedEvent;
import com.grangloria.gateway.event.ReturnLabelReadyEvent;
import com.grangloria.gateway.messaging.producer.ReturnLabelReadyEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierGatewayService {

    private final CarrierClient carrierClient;
    private final ReturnLabelReadyEventPublisher labelGeneratedPublisher;

    public Mono<Void> processCarrierLabelCreation(LabelRequest request) {
        log.info("[CARRIER-GATEWAY-SERVICE] Requesting 3PL carrier label for Order: [{}]", request.orderId());

        return carrierClient.requestLabel(request)
                .doOnSuccess(carrierResponse -> {
                    log.info("[CARRIER-GATEWAY-SERVICE] Carrier label generated! Order ID: [{}]", request.orderId());

                    ReturnLabelReadyEvent generatedEvent = new ReturnLabelReadyEvent(
                            request.orderId(),
                            request.customerEmail(),
                            carrierResponse
                    );

                    labelGeneratedPublisher.publishReturnLabelReadyEvent(generatedEvent);
                })
                .then();
    }
}