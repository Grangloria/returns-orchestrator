package com.grangloria.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties
{
    private String returnInitiated;
    private String returnLabelReady;
}