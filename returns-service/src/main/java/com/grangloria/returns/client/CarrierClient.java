package com.grangloria.returns.client;

import com.grangloria.returns.dto.request.LabelRequest;
import com.grangloria.returns.dto.response.LabelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class CarrierClient {

    private final WebClient webClient;

    public CarrierClient(@Qualifier("carrierWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> requestLabel(LabelRequest request) {
        return webClient.post()
                .uri("/api/v1/mock-carrier/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LabelResponse.class)
                .timeout(Duration.ofSeconds(3))
                .map(response -> {
                    if (response != null && response.mockLabelUrl() != null && !response.mockLabelUrl().isBlank()) {
                        return response.mockLabelUrl();
                    }
                    return "PENDING_GENERATION";
                })
                .defaultIfEmpty("PENDING_GENERATION")
                .onErrorResume(e -> {
                    log.error("[CARRIER-CLIENT] Label request failed for tracking number: [{}]. Reason: {}",
                            request.orderId(), e.getMessage());
                    return Mono.just("PENDING_GENERATION");
                });
    }
}