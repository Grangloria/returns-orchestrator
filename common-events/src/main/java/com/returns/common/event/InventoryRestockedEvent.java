package com.returns.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryRestockedEvent(
        @JsonProperty("returnId") String returnId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("sku") String sku,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("refundAmount") BigDecimal refundAmount,
        @JsonProperty("restockedAt") Instant restockedAt
) {}