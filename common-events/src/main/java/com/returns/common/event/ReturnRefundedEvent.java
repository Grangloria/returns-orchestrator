package com.returns.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ReturnRefundedEvent(
        @JsonProperty("returnId") String returnId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("sku") String sku,
        @JsonProperty("amount") double amount,
        @JsonProperty("refundedAt") Instant refundedAt
) {}