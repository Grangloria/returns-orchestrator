package com.returns.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryRestockedEvent(
        String returnId,
        String sku,
        int newStockQuantity,
        BigDecimal refundAmount,
        String customerEmail,
        Instant restockedAt
) {}