package com.grangloria.carrier.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "carrier")
@Validated
public record CarrierProperties(
        @NotBlank String name,
        SimulatorProperties simulator
) {
    public record SimulatorProperties(
            @Positive long transitDelaySeconds
    ) {}
}