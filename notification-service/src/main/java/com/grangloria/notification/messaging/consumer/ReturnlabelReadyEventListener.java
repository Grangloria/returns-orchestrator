package com.grangloria.notification.messaging.consumer;

import com.grangloria.notification.event.ReturnLabelReadyEvent;
import com.grangloria.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnlabelReadyEventListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "#{@kafkaTopicProperties.getReturnLabelReady()}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}"
    )
    public void handleReturnLabelReady(ConsumerRecord<String, ReturnLabelReadyEvent> record) {
        ReturnLabelReadyEvent event = record.value();

        if (event == null) {
            log.warn("[NOTIFICATION-SERVICE] Received an empty or un-parsable Kafka payload envelope.");
            return;
        }

        log.info("[NOTIFICATION-SERVICE] Kafka event received for Order ID: [{}]", event.orderId());

        try {
            notificationService.sendReturnReadyLabelEmail(event);
        } catch (Exception e) {
            log.error("[NOTIFICATION-SERVICE-ERROR] Exception thrown during email dispatch routine for Order ID: [{}]: {}",
                    event.orderId(), e.getMessage(), e);
            throw e;
        }
    }
}