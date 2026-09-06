package com.grangloria.returns.messaging.publisher;

import com.grangloria.returns.event.ReturnInitiatedEvent;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class ReturnEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Tracer tracer; // Automatically provided by Spring Boot's tracing bridge
    private static final String TOPIC = "return-events.v1"; // Kafka topic for return events

    // Constructor injection includes both the template and the tracer
    public ReturnEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, Tracer tracer) {
        this.kafkaTemplate = kafkaTemplate;
        this.tracer = tracer;
    }

    public void publishReturnEvent(ReturnInitiatedEvent event) {
        // Convert to a ProducerRecord so we can attach custom network metadata/headers
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(TOPIC, event.returnId(), event);

        // Intercept the active trace ID from the running reactive thread context
        var currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            String traceId = currentSpan.context().traceId();

            // Inject the trace ID straight into the outbound Kafka message headers
            producerRecord.headers().add(new RecordHeader("x-b3-traceid", traceId.getBytes(StandardCharsets.UTF_8)));
            log.info("Successfully stamped outbound trace context header: {}", traceId);
        } else {
            log.warn("No active tracing context found. Kafka event will be emitted un-monitored.");
        }

        // Asynchronously fire the event to the Kafka broker
        kafkaTemplate.send(producerRecord)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Asynchronous Event successfully delivered to Kafka topic [{}] for Return ID: {}",
                                TOPIC, event.returnId());
                    } else {
                        log.error("Failed to deliver Kafka event for Return ID: {}. Error: {}",
                                event.returnId(), ex.getMessage());
                    }
                });
    }
}