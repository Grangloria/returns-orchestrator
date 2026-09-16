package com.grangloria.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic returnLabelReadyDltTopic() {
        return TopicBuilder.name("returns.label.ready.v1.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}