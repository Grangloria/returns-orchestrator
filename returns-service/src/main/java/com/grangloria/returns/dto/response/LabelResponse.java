package com.grangloria.returns.dto.response;

import java.time.Instant;

public record LabelResponse(
        String customerName,
        String mockTrackingNumber,
        String mockLabelUrl,
        String company,
        Instant time
) {}