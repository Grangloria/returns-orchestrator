package com.grangloria.returns.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReturnInitiatedEvent(
        @JsonProperty("returnId") String returnId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerId") String customerId, // Added this!
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("sku") String sku,
        @JsonProperty("traceId") String traceId
) {}