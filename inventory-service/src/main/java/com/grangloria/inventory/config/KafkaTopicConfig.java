package com.grangloria.inventory.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String RETURN_COMPLETED_TOPIC = "returns.return-completed.v1";
    public static final String INVENTORY_STOCK_UPDATED_TOPIC = "inventory.stock-updated.v1";

    @Bean
    public NewTopic returnCompletedTopic() {
        return TopicBuilder.name(RETURN_COMPLETED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryStockUpdatedTopic() {
        return TopicBuilder.name(INVENTORY_STOCK_UPDATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}