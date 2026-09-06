package com.grangloria.carrier.dto.response;

import java.time.Instant;

public record LabelResponse(

        String mockTrackingNumber,
        String mockLabelUrl,
        String company,
        Instant time
) {}