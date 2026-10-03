package com.returns.common.event;

import java.time.Instant;

public record ReturnLabelReadyEvent(
        String returnId,
        String orderId,
        String customerEmail,
        String trackingNumber,
        String labelUrl,
        String carrierName,
        String sku,
        int quantity,
        Instant createdAt
) {}