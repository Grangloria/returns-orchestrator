package com.grangloria.carrier.dto.response;

import java.time.Instant;

public record LabelResponse(
        String customerEmail,
        String mockTrackingNumber,
        String mockLabelUrl,
        String company,
        Instant time
) {}