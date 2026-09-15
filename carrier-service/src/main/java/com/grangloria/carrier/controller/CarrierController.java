package com.grangloria.carrier.controller;

import com.grangloria.carrier.dto.request.LabelRequest;
import com.grangloria.carrier.dto.response.LabelResponse;
import com.grangloria.carrier.service.CarrierService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/mock-carrier")
public class CarrierController {

    private static final Logger log = LoggerFactory.getLogger(CarrierController.class);
    private final CarrierService carrierService;

    public CarrierController(CarrierService carrierService) {
        this.carrierService = carrierService;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<LabelResponse> generateLabel(@Valid @RequestBody LabelRequest labelRequest) {
        log.info("Received request to generate label for customer: {}", labelRequest.customerEmail());
        return carrierService.generateLabel(labelRequest);
    }
}