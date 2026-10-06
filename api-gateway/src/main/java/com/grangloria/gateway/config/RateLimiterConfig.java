package com.grangloria.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            // 1. Resolve X-API-Key Header
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            if (apiKey != null && !apiKey.isBlank()) {
                return Mono.just(apiKey);
            }

            // 2. Fallback to Authenticated Principal Name (JWT Sub)
            return exchange.getPrincipal()
                    .map(principal -> "user:" + principal.getName())
                    // 3. Fallback to Remote IP Address
                    .switchIfEmpty(Mono.just(
                            Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                                    .map(addr -> "ip:" + addr.getAddress().getHostAddress())
                                    .orElse("anonymous")
                    ));
        };
    }
}