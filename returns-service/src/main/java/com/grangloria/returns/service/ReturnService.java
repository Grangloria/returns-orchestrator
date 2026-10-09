package com.grangloria.returns.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grangloria.returns.dto.request.ReturnRequest;
import com.grangloria.returns.dto.response.ReturnResponse;
import com.grangloria.returns.entity.OutboxEvent;
import com.grangloria.returns.entity.ReturnManifest;
import com.grangloria.returns.entity.ReturnState;
import com.grangloria.returns.repository.OutboxRepository;
import com.returns.common.event.PackageReceivedEvent;
import com.returns.common.event.ReturnInitiatedEvent;
import com.returns.common.event.ReturnLabelReadyEvent;
import com.grangloria.returns.exception.ReturnNotFoundException;
import com.grangloria.returns.repository.ManifestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ManifestRepository repository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Mono<ReturnResponse> processReturn(ReturnRequest request) {
        log.info("[RETURN-SERVICE-API] Ingesting incoming return request for Order: [{}], SKU: [{}]",
                request.orderId(), request.sku());

        ReturnManifest manifest = ReturnManifest.builder()
                .orderId(request.orderId())
                .sku(request.sku())
                .item(request.item())
                .quantity(request.quantity())
                .customerEmail(request.customerEmail())
                .zipCode(request.zipCode())
                .reason(request.reason())
                .status(ReturnState.INITIATED)
                .createdAt(LocalDateTime.now())
                .isNewEntity(true)
                .build();

        ReturnInitiatedEvent eventPayload = new ReturnInitiatedEvent(
                request.orderId(),
                request.sku(),
                request.item(),
                request.customerEmail(),
                request.quantity(),
                request.zipCode(),
                request.reason()
        );

        return repository.save(manifest)
                .flatMap(saved -> {
                    log.info("[RETURN-SERVICE-DATABASE] Manifest audit record permanently persisted for Order ID: [{}]",
                            saved.getOrderId());

                    try {
                        String payloadJson = objectMapper.writeValueAsString(eventPayload);
                        OutboxEvent outboxEvent = OutboxEvent.create(
                                "RETURN",
                                saved.getOrderId(),
                                "RETURN_INITIATED",
                                payloadJson
                        );

                        return outboxRepository.save(outboxEvent)
                                .doOnSuccess(savedOutbox ->
                                        log.info("[RETURN-SERVICE-OUTBOX] Outbox event [{}] staged atomically for Order ID: [{}]",
                                                savedOutbox.id(), saved.getOrderId()))
                                .thenReturn(saved);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Failed to serialize ReturnInitiatedEvent for outbox", e));
                    }
                })
                .map(saved -> new ReturnResponse(
                        saved.getOrderId(),
                        saved.getStatus().name(),
                        System.currentTimeMillis()
                ))
                .doOnError(error -> log.error("[RETURN-SERVICE-ERROR] Operational pipeline friction encountered for Order: [{}]. Reason: {}",
            request.orderId(), error.getMessage(), error));
    }

    public Mono<Void> handleLabelGenerated(ReturnLabelReadyEvent event) {
        log.info("[RETURN-SERVICE-CONSUMER] Attempting database state transition for Order ID: [{}] with Label URL: [{}]",
                event.orderId(), event.labelUrl());

        return repository.findByOrderId(event.orderId())
                .map(ReturnManifest::markNotNew)
                .switchIfEmpty(Mono.error(new ReturnNotFoundException(event.orderId())))
                .flatMap(manifest -> {
                    log.info("[RETURN-SERVICE-CONSUMER] Found manifest for Order ID: [{}]. Current status: [{}]",
                            manifest.getOrderId(), manifest.getStatus());

                    // Idempotent Guard: If already in LABEL_READY state, complete silently
                    if (manifest.getStatus() == ReturnState.LABEL_READY) {
                        log.warn("[RETURN-SERVICE-CONSUMER] Order [{}] is already in LABEL_READY state. Skipping duplicate event.",
                                event.orderId());
                        return Mono.empty();
                    }

                    // State Machine Validation
                    if (!manifest.getStatus().canTransitionTo(ReturnState.LABEL_READY)) {
                        return Mono.error(new IllegalStateException(
                                String.format("Invalid state transition from %s to LABEL_READY for Order: [%s]",
                                        manifest.getStatus(), event.orderId())
                        ));
                    }

                    manifest.setStatus(ReturnState.LABEL_READY);
                    manifest.setLabelUrl(event.labelUrl());

                    return repository.save(manifest);
                })
                .doOnSuccess(updated -> {
                    if (updated != null) {
                        log.info("[RETURN-SERVICE-DATABASE] ✅ SUCCESS: Manifest state permanently updated to LABEL_READY for Order: [{}]",
                                updated.getOrderId());
                    }
                })
                .doOnError(err -> log.error("[RETURN-SERVICE-DATABASE] ❌ FAILED: Database update failed for Order ID: [{}]. Cause: ",
                        event.orderId(), err))
                .then();
    }

    public Mono<Void> handlePackageReceived(PackageReceivedEvent event) {
        log.info("[RETURN-SERVICE-CONSUMER] Attempting database state transition to RECEIVED for Order ID: [{}]",
                event.orderId());

        return repository.findByOrderId(event.orderId())
                .map(ReturnManifest::markNotNew)
                .switchIfEmpty(Mono.error(new ReturnNotFoundException(event.orderId())))
                .flatMap(manifest -> {
                    if (manifest.getStatus() == ReturnState.RECEIVED) {
                        log.info("[RETURN-SERVICE-CONSUMER] Order [{}] is already in RECEIVED state. Skipping duplicate event.",
                                event.orderId());
                        return Mono.empty();
                    }

                    if (!manifest.getStatus().canTransitionTo(ReturnState.RECEIVED)) {
                        return Mono.error(new IllegalStateException(
                                String.format("Invalid state transition from %s to RECEIVED for Order: [%s]",
                                        manifest.getStatus(), event.orderId())
                        ));
                    }

                    manifest.setStatus(ReturnState.RECEIVED);
                    return repository.save(manifest);
                })
                .doOnSuccess(updated -> {
                    if (updated != null) {
                        log.info("[RETURN-SERVICE-DATABASE] ✅ SUCCESS: Manifest state permanently updated to RECEIVED for Order: [{}]",
                                updated.getOrderId());
                    }
                })
                .doOnError(err -> log.error("[RETURN-SERVICE-DATABASE] ❌ FAILED: Database update failed for Order ID: [{}]. Cause: ",
                        event.orderId(), err))
                .then();
    }

    public Mono<ReturnManifest> getReturnStatus(String orderId) {
        log.info("[RETURN-SERVICE-LOOKUP] Executing operational trace for Order ID: [{}]", orderId);

        return repository.findByOrderId(orderId)
                .map(ReturnManifest::markNotNew)
                .doOnNext(manifest -> log.info("[RETURN-SERVICE-LOOKUP] Manifest record located. Active State: [{}]", manifest.getStatus()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[RETURN-SERVICE-LOOKUP-WARN] Database search yielded zero results for Order ID: [{}]", orderId);
                    return Mono.error(new ReturnNotFoundException(orderId));
                }));
    }
}