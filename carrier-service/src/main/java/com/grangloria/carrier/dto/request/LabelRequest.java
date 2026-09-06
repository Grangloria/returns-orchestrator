package com.grangloria.carrier.dto.request;

public record LabelRequest(
        String originalTrackingNumber,
        String customerAddress,
        double weightLbs
) {}