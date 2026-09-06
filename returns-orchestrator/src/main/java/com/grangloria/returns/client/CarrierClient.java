package com.grangloria.returns.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class CarrierClient {
    private final WebClient webClient;
    private static final Logger log = LoggerFactory.getLogger(CarrierClient.class);

    public CarrierClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> requestLabel(String trackingId) {
        return webClient.post()
                .uri("/api/v1/mock-carrier/generate")
                .bodyValue(Map.of("trackingId", trackingId))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("labelUrl"))
                .timeout(Duration.ofSeconds(3)) // Senior Move: Never wait forever
                .onErrorResume(e -> {
                    log.error("Carrier API failed, falling back to empty label", e);
                    return Mono.just("PENDING_GENERATION");
                });
    }
}