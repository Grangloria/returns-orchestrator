package com.grangloria.inventory.dto;

import java.util.List;

public record ErrorResponseDto(
        String message,
        int status,
        long timestamp,
        List<String> details
) {}