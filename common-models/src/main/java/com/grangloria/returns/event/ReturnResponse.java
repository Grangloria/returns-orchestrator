package com.grangloria.returns.eventvent;

// Response DTO for request acknowledgement
public record ReturnResponse(
        String trackingId,
        String status,
        String labelUrl,
        Long timestamp
) {}