package com.grangloria.returns.service;

import com.grangloria.returns.event.ReturnRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ReturnService {
    public Mono<ReturnRequest> validate(ReturnRequest request) {
        if (request.sku().startsWith("HAYE")) {
            return Mono.just(request);
        }
        return Mono.error(new IllegalArgumentException("Unauthorized SKU: Only HAYE products allowed."));
    }
}