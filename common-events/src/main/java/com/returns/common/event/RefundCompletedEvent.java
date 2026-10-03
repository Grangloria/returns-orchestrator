package com.returns.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundCompletedEvent(
        String returnId,
        String customerEmail,
        BigDecimal amountRefunded,
        String transactionId,
        Instant processedAt
) {}