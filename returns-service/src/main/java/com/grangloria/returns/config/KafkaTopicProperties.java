package com.grangloria.returns.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {
    private String returnInitiated;
    private String returnCompleted;
    private String returnInventory;
    private String returnLabelGenerated;
    private String returnLabelReady;
}