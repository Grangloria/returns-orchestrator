// FedexProperties.java
package com.grangloria.carrier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carrier.integration.fedex")
public record FedexProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {}