package com.grangloria.returns.service;

import com.grangloria.returns.dto.request.ReturnRequest;
import com.grangloria.returns.dto.response.ReturnResponse;
import com.grangloria.returns.entity.ReturnManifest;
import com.grangloria.returns.entity.ReturnState;
import com.grangloria.returns.event.ReturnInitiatedEvent;
import com.grangloria.returns.event.ReturnLabelReadyEvent;
import com.grangloria.returns.exception.ReturnNotFoundException;
import com.grangloria.returns.messaging.publisher.ReturnInitiatedEventPublisher;
import com.grangloria.returns.messaging.publisher.ReturnLabelReadyEventPublisher;
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
    private final ReturnInitiatedEventPublisher initiatedPublisher;
    private final ReturnLabelReadyEventPublisher labelReadyPublisher;

    public Mono<ReturnResponse> processReturn(ReturnRequest request) {
        log.info("[RETURN-SERVICE-API] Ingesting incoming return request for Order: [{}], SKU: [{}]",
                request.orderId(), request.sku());

        return saveToDatabase(request)
                .doOnSuccess(saved -> {
                    log.info("[RETURN-SERVICE-KAFKA] Broadcasting ReturnInitiatedEvent downstream for Order ID: [{}]", request.orderId());
                    initiatedPublisher.publishReturnInitiatedEvent(
                            new ReturnInitiatedEvent(
                                    request.orderId(),
                                    request.sku(),
                                    request.item(),
                                    request.customerEmail(),
                                    request.quantity(),
                                    request.zipCode(),
                                    request.reason()
                            )
                    );
                })
                .map(saved -> new ReturnResponse(
                        saved.getOrderId(),
                        saved.getStatus().name(),
                        System.currentTimeMillis()
                ))
                .doOnError(error -> log.error("[RETURN-SERVICE-ERROR] Operational pipeline friction encountered for Order: [{}]. Reason: {}",
                        request.orderId(), error.getMessage()));
    }

    /**
     * Handles incoming ReturnLabelReadyEvent from carrier-gateway
     */
    @Transactional
    public Mono<Void> handleLabelGenerated(ReturnLabelReadyEvent event) {
        log.info("[RETURN-SERVICE-CONSUMER] Processing ReturnLabelReadyEvent for Order ID: [{}]", event.orderId());

        return repository.findByOrderId(event.orderId())
                .switchIfEmpty(Mono.error(new ReturnNotFoundException(event.orderId())))
                .flatMap(manifest -> {
                    if (!manifest.getStatus().canTransitionTo(ReturnState.LABEL_READY)) {
                        return Mono.error(new IllegalStateException(
                                String.format("Invalid state transition from %s to LABEL_READY for Order: [%s]",
                                        manifest.getStatus(), event.orderId())
                        ));
                    }

                    manifest.setStatus(ReturnState.LABEL_READY);
                    manifest.setLabelUrl(event.labelUrl());
                    manifest.setNewEntity(false);

                    return repository.save(manifest);
                })
                .doOnSuccess(updated -> {
                    log.info("[RETURN-SERVICE-DATABASE] Manifest state updated to LABEL_READY for Order: [{}]", updated.getOrderId());

                    ReturnLabelReadyEvent notificationEvent = new ReturnLabelReadyEvent(
                            updated.getOrderId(),
                            updated.getCustomerEmail(),
                            event.labelUrl()
                    );
                    labelReadyPublisher.publishReturnLabelReadyEvent(notificationEvent);
                })
                .then();
    }

    private Mono<ReturnManifest> saveToDatabase(ReturnRequest request) {
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

        return repository.save(manifest)
                .doOnSuccess(savedManifest -> log.info("[RETURN-SERVICE-DATABASE] Manifest audit record permanently persisted for Order ID: [{}]",
                        savedManifest.getOrderId()));
    }

    public Mono<ReturnManifest> getReturnStatus(String orderId) {
        log.info("[RETURN-SERVICE-LOOKUP] Executing operational trace for Order ID: [{}]", orderId);

        return repository.findByOrderId(orderId)
                .doOnNext(manifest -> log.info("[RETURN-SERVICE-LOOKUP] Manifest record located. Active State: [{}]", manifest.getStatus()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[RETURN-SERVICE-LOOKUP-WARN] Database search yielded zero results for Order ID: [{}]", orderId);
                    return Mono.error(new ReturnNotFoundException(orderId));
                }));
    }
}