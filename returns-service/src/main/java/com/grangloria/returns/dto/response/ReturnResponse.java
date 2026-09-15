package com.grangloria.returns.dto.response;

public record ReturnResponse(
        String orderId,
        String status,
        Long timestamp
) {}