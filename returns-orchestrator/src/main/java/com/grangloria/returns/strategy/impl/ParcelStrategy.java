package com.grangloria.returns.strategy.impl;

import com.grangloria.returns.event.ReturnRequest;
import com.grangloria.returns.event.ReturnResponse;
import com.grangloria.returns.strategy.TriageStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(1)
public class ParcelStrategy implements TriageStrategy {
    @Override
    public boolean isApplicable(ReturnRequest request) {
        return request.quantity() < 10; // Simple business rule
    }

    @Override
    public Mono<ReturnResponse> execute(ReturnRequest request) {
        return Mono.just(new ReturnResponse(
                "PRCL-" + UUID.randomUUID().toString().substring(0, 8),
                "PARCEL_READY",
                null,
                System.currentTimeMillis()
        ));
    }
}