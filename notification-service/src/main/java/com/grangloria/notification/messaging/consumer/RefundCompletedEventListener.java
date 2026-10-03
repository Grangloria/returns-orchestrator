package com.grangloria.notification.messaging.consumer;

import com.returns.common.event.RefundCompletedEvent;
import com.grangloria.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundCompletedEventListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.refund-completed:returns.refund.completed.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-group}",
            properties = {"spring.json.value.default.type=com.returns.common.event.RefundCompletedEvent"}
    )
    public void handleRefundCompleted(RefundCompletedEvent event) {
        if (event == null) {
            log.warn("[NOTIFICATION-SERVICE] Received null or un-parsable RefundCompletedEvent envelope.");
            return;
        }

        log.info("[NOTIFICATION-SERVICE] RefundCompletedEvent received for Return ID: [{}]", event.returnId());
        notificationService.sendRefundCompletedEmail(event);
    }
}