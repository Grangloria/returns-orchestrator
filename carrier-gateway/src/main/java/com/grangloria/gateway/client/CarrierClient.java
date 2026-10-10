package com.grangloria.gateway.client;

import com.grangloria.gateway.dto.request.LabelRequest;
import com.grangloria.gateway.dto.response.LabelResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @Retry(name = "mockExternalCarrierApi", fallbackMethod = "requestLabelFallback")
    @CircuitBreaker(name = "mockExternalCarrierApi")
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
                    throw new RuntimeException("Empty label URL received from carrier API");
                });
    }

    @SuppressWarnings("unused")
    public Mono<String> requestLabelFallback(LabelRequest request, Throwable ex) {
        log.error("[CARRIER-CLIENT] Retries exhausted / Circuit OPEN for Order: [{}]. Error: {}",
                request.orderId(), ex.getMessage());
        // Emit Reactive error so doOnSuccess in service is bypassed and exception reaches Kafka Listener
        return Mono.error(new RuntimeException("External carrier API failure: " + ex.getMessage(), ex));
    }
}