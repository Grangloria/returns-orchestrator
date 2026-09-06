package com.grangloria.carrier.controller;

import com.grangloria.carrier.dto.request.LabelRequest;
import com.grangloria.carrier.dto.response.LabelResponse;
import com.grangloria.carrier.service.CarrierService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/carriers")
public class CarrierController {

    private final CarrierService carrierService;

    public CarrierController(CarrierService carrierService) {
        this.carrierService = carrierService;
    }

    @PostMapping("/labels")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<LabelResponse> generateLabel(@RequestBody LabelRequest labelRequest) {
        return carrierService.generateLabel(labelRequest);
    }
}