package com.grangloria.returns.controller;

import com.grangloria.returns.dto.request.ReturnRequest;
import com.grangloria.returns.dto.response.ReturnResponse;
import com.grangloria.returns.entity.ReturnManifest;
import com.grangloria.returns.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @GetMapping("/{orderId}")
    public Mono<ResponseEntity<ReturnManifest>> getReturn(@PathVariable String orderId) {
        log.info("[API-RETURNS] Status query received for Order ID: [{}]", orderId);

        return returnService.getReturnStatus(orderId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<ReturnResponse>> createReturn(@Valid @RequestBody ReturnRequest request) {
        log.info("[API-RETURNS] Received return request for Order: [{}], SKU: [{}] from Customer: [{}]",
                request.orderId(), request.sku(), request.customerEmail());

        return returnService.processReturn(request)
                .map(response -> ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .location(URI.create("/api/v1/returns/"))
                        .body(response)
                );
    }
}