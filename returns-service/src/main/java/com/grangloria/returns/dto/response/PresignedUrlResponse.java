package com.grangloria.returns.dto.response;

public record PresignedUrlResponse(
        String uploadUrl,
        String s3Key,
        long expiresInSeconds
) {}