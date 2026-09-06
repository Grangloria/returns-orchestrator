package com.grangloria.carrier.service;

import com.grangloria.carrier.dto.request.LabelRequest;
import com.grangloria.carrier.dto.response.LabelResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

@Service
public class CarrierService {

    private final String carrierName;

    public CarrierService(@Value("${carrier.name:Grangloria Freight}") String carrierName) {
        this.carrierName = carrierName;
    }

    public Mono<LabelResponse> generateLabel(LabelRequest request) {
        String mockTrackingNumber = "TRACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String mockLabelUrl = "https://mock-carrier.grangloria.internal/labels/" + mockTrackingNumber + ".pdf";

        return Mono.just(new LabelResponse(
                mockTrackingNumber,
                mockLabelUrl,
                carrierName,
                Instant.now()
        ));
    }
}