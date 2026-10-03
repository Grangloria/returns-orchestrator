package com.grangloria.returns.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReturnLabelReadyEvent(
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerEmail") String customerEmail,
        @JsonProperty("labelUrl") String labelUrl
) {}