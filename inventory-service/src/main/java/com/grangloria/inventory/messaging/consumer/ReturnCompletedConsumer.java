package com.grangloria.inventory.messaging.consumer;

import com.grangloria.inventory.config.KafkaTopicConfig;
import com.grangloria.inventory.dto.ReturnCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReturnCompletedConsumer
{

    private static final Logger log = LoggerFactory.getLogger(ReturnCompletedConsumer.class);

    @KafkaListener(
            topics = KafkaTopicConfig.RETURN_COMPLETED_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleReturnCompleted(ReturnCompletedEvent event) {
        log.info("Received ReturnCompletedEvent for Return ID: {} | SKU: {} | Quantity: {}",
                event.returnId(), event.sku(), event.quantity());

        // TODO: Call Reactive InventoryService to increment sellable stock in R2DBC
    }
}