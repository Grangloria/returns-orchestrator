package com.grangloria.gateway.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReturnInitiatedEvent(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("sku") String sku,
        @JsonProperty("item") String item,
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("zipCode") String zipCode,
        @JsonProperty("reason") String reason
) {}