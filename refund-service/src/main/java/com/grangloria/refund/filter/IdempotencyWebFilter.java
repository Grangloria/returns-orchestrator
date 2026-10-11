package com.grangloria.refund.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyWebFilter implements WebFilter {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String REDIS_PREFIX = "idempotency:refund:";
    private static final Duration TTL = Duration.ofHours(24);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String idempotencyKey = exchange.getRequest().getHeaders().getFirst(IDEMPOTENCY_HEADER);

        // Bypass filter if no Idempotency key is supplied or if method is read-only (GET/DELETE)
        if (idempotencyKey == null || idempotencyKey.isBlank() || HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String cacheKey = REDIS_PREFIX + idempotencyKey;

        return redisTemplate.opsForValue().get(cacheKey)
                .flatMap(cachedResponseJson -> {
                    log.info("[IDEMPOTENCY-FILTER] Cache HIT for key [{}]. Returning stored payload.", idempotencyKey);
                    return writeCachedResponse(exchange.getResponse(), cachedResponseJson);
                })
                .switchIfEmpty(Mono.defer(() -> executeAndCache(exchange, chain, cacheKey)));
    }

    private Mono<Void> executeAndCache(ServerWebExchange exchange, WebFilterChain chain, String cacheKey) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            @NonNull
            public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                return super.writeWith(
                        Flux.from(body)
                                .buffer()
                                .flatMap(dataBuffers -> {
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    dataBuffers.forEach(buffer -> {
                                        byte[] bytes = new byte[buffer.readableByteCount()];
                                        buffer.asByteBuffer().get(bytes);
                                        outputStream.write(bytes, 0, bytes.length);
                                    });

                                    byte[] content = outputStream.toByteArray();
                                    String responseBody = new String(content, StandardCharsets.UTF_8);
                                    HttpStatusCode statusCode = getStatusCode();

                                    Mono<Void> cacheMono = Mono.empty();
                                    if (statusCode != null && statusCode.is2xxSuccessful()) {
                                        CachedResponse record = new CachedResponse(statusCode.value(), responseBody);
                                        try {
                                            String json = objectMapper.writeValueAsString(record);
                                            cacheMono = redisTemplate.opsForValue().set(cacheKey, json, TTL)
                                                    .doOnSuccess(v -> log.info("[IDEMPOTENCY-FILTER] Cached execution result for key [{}]", cacheKey))
                                                    .then();
                                        } catch (JsonProcessingException e) {
                                            log.error("[IDEMPOTENCY-FILTER] Serialization failed for key [{}]", cacheKey, e);
                                        }
                                    }

                                    DataBuffer wrappedBuffer = bufferFactory.wrap(content);
                                    return cacheMono.then(Mono.just(wrappedBuffer));
                                })
                );
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private Mono<Void> writeCachedResponse(ServerHttpResponse response, String cachedResponseJson) {
        try {
            CachedResponse cached = objectMapper.readValue(cachedResponseJson, CachedResponse.class);
            response.setStatusCode(HttpStatus.valueOf(cached.statusCode()));
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            DataBuffer buffer = response.bufferFactory().wrap(cached.body().getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("[IDEMPOTENCY-FILTER] Failed to deserialize cached JSON payload", e);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response.setComplete();
        }
    }

    public record CachedResponse(int statusCode, String body) {}
}