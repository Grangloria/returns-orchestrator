package com.grangloria.carrier.dto.request;

public record LabelRequest(
        String carrier,
        String trackingNumber,
        String originAddress,
        String destinationAddress,
        double weightLbs
) {}