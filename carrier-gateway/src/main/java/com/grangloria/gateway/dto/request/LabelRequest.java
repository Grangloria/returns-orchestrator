package com.grangloria.gateway.dto.request;

public record LabelRequest(
        String customerEmail,
        String orderId,
        String customerZip,
        int quantity
) {}