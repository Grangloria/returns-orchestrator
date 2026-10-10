package com.grangloria.carrier.dto.request;

public record LabelRequest(
        String customerEmail,
        String orderId,
        String customerAddress,
        double weightLbs
) {}