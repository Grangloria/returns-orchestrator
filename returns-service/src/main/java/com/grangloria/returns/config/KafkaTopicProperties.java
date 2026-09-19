package com.grangloria.returns.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {
    private String returnInitiated;
    private String returnCompleted;
    private String returnInventory;
    private String returnLabelGenerated;
    private String returnLabelReady;
}