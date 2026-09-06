package com.grangloria.notification.messaging.consumer;

import com.grangloria.returns.event.ReturnInitiatedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    private final Tracer tracer;

    @KafkaListener(topics = "return-events.v1", groupId = "notification-group")
    public void handleNotification(ConsumerRecord<String, ReturnInitiatedEvent> record) {
        String incomingTraceId = null;
        Header traceHeader = record.headers().lastHeader("x-b3-traceid");
        if (traceHeader != null) {
            incomingTraceId = new String(traceHeader.value(), StandardCharsets.UTF_8);
        }

        ReturnInitiatedEvent event = record.value();
        if (incomingTraceId == null && event != null) {
            incomingTraceId = event.traceId();
        }

        Span nextSpan = (incomingTraceId != null) ? tracer.nextSpan().start() : tracer.nextSpan().name("fallback-span").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(nextSpan)) {
            if (event == null) {
                log.warn("[NOTIFICATION-SERVICE] Received an empty or un-parsable Kafka payload envelope.");
                return;
            }

            log.info("[NOTIFICATION-SERVICE] Asynchronous Kafka message picked up successfully.");
            log.info("[NOTIFICATION-SERVICE] Processing communication routing rules for Customer: [{}]", event.customerEmail());
            log.info("[NOTIFICATION-SERVICE] MOCK outbound SMTP trigger status -> Notification transactional email sent via SendGrid for Order: [{}]",
                    event.orderId());

        } catch (Exception e) {
            log.error("[NOTIFICATION-SERVICE-ERROR] Exception thrown during email dispatch routine: {}", e.getMessage(), e);
        } finally {
            nextSpan.end();
        }
    }
}