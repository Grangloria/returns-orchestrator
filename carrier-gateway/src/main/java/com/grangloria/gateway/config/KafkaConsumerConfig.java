package com.grangloria.gateway.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    private final KafkaTopicProperties topicProperties;

    public KafkaConsumerConfig(KafkaTopicProperties topicProperties) {
        this.topicProperties = topicProperties;
    }

    @Bean
    public CommonErrorHandler commonErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(topicProperties.getCarrierLabelDlt(), record.partition())
        );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0));
    }

    @Bean
    public NewTopic carrierLabelDltTopic() {
        return TopicBuilder.name(topicProperties.getCarrierLabelDlt())
                .partitions(1)
                .replicas(1)
                .build();
    }
}