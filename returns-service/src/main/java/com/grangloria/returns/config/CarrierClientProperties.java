package com.grangloria.returns.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "services.carrier-service")
public class CarrierClientProperties
{
    // Getters and setters required
    private String baseUrl;
    private int connectTimeoutMs;
    private int readTimeoutMs;

}