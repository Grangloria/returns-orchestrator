package com.grangloria.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "carrier.client")
public class CarrierClientProperties {

    private String baseUrl = "http://localhost:8003";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 3000;
}