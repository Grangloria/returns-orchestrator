package com.returns.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundCompletedEvent(
        @JsonProperty("returnId") String returnId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("completedAt") Instant completedAt
) {}