package com.returns.common.event;

import java.time.Instant;

public record PackageReceivedEvent(
        String returnId,
        String orderId,
        String sku,
        int quantity,
        Instant scannedAt
) {}