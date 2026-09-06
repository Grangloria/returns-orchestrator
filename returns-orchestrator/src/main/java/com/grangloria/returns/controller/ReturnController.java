package com.grangloria.returns.controller;

import com.grangloria.returns.event.ReturnRequest;
import com.grangloria.returns.event.ReturnResponse;
import com.grangloria.returns.messaging.publisher.ReturnEventPublisher;
import com.grangloria.returns.service.ReturnOrchestrator;
import com.grangloria.returns.entity.ReturnManifest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/returns")
public class ReturnController {

    private final ReturnOrchestrator orchestrator;

    // Inject the Kafka Publisher alongside your Orchestrator
    public ReturnController(ReturnOrchestrator orchestrator, ReturnEventPublisher eventPublisher) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/{trackingId}")
    public Mono<ResponseEntity<ReturnManifest>> getReturn(@PathVariable String trackingId) {
        return orchestrator.getReturnStatus(trackingId)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ReturnResponse> createReturn(@Valid @RequestBody ReturnRequest request) {
        // 1. Generate the unique ID for this request's lifecycle
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        System.out.println("[" + traceId + "] API: Received return request for SKU: " + request.sku());

        // 2. Pass the work to the Orchestrator.
        // The Orchestrator will handle the DB save AND firing the Kafka event.
        return orchestrator.processReturn(request, traceId);
    }
}