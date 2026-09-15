package com.grangloria.returns.dto.request;

public record LabelRequest(
        String customerName,
        String orderId,
        String customerAddress,
        double weightLbs
) {}