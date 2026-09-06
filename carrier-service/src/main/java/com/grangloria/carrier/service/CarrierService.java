package com.grangloria.carrier.service;

import com.grangloria.carrier.dto.request.LabelRequest;
import com.grangloria.carrier.dto.response.LabelResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CarrierService {

    public Mono<LabelResponse> generateLabel(LabelRequest labelRequest) {
        // Business logic and client routing will go here
        return Mono.empty();
    }
}