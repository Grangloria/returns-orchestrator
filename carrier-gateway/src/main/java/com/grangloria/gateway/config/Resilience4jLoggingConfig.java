package com.grangloria.gateway.config;

import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class Resilience4jLoggingConfig {

    private final RetryRegistry retryRegistry;

    public Resilience4jLoggingConfig(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    public void postConstruct() {
        retryRegistry.retry("mockExternalCarrierApi")
                .getEventPublisher()
                .onRetry(event -> {
                    Throwable throwable = event.getLastThrowable();
                    String cause = (throwable != null && throwable.getMessage() != null)
                            ? throwable.getMessage()
                            : "No exception message available";

                    log.warn("[RESILIENCE4J-RETRY] Attempt #{} failed. Waiting {}ms before next try. Cause: {}",
                            event.getNumberOfRetryAttempts(),
                            event.getWaitInterval().toMillis(),
                            cause);
                });
    }
}