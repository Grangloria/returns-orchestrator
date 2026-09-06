package com.grangloria.returns.strategy;

import com.grangloria.returns.event.ReturnRequest;
import com.grangloria.returns.event.ReturnResponse;
import reactor.core.publisher.Mono;

public interface TriageStrategy {
    boolean isApplicable(ReturnRequest request);
    Mono<ReturnResponse> execute(ReturnRequest request);
}