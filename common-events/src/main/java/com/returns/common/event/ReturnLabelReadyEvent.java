package com.returns.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ReturnLabelReadyEvent(
        @JsonProperty("returnId") String returnId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("trackingNumber") String trackingNumber,
        @JsonProperty("labelUrl") String labelUrl,
        @JsonProperty("carrierName") String carrierName,
        @JsonProperty("sku") String sku,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("createdAt") Instant createdAt
) {}