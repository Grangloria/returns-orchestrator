package com.grangloria.returns.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
        @NotBlank(message = "OrderId is required") String orderId,
        @NotBlank(message = "FileName is required") String fileName,
        @NotBlank(message = "ContentType is required") String contentType
) {}