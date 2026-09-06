package com.grangloria.inventory.exception;


import java.util.List;

public record ErrorResponse(
        String message,
        int code,
        long timestamp,
        List<String> details // For showing multiple validation errors at once
) {}