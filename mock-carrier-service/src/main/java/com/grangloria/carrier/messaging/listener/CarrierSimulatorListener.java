package com.grangloria.carrier.messaging.listener;

import com.grangloria.carrier.config.CarrierProperties;
import com.returns.common.event.PackageReceivedEvent;
import com.returns.common.event.ReturnLabelReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CarrierSimulatorListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String packageReceivedTopic;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final CarrierProperties carrierProperties;

    public CarrierSimulatorListener(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.package-received}") String packageReceivedTopic,
            CarrierProperties carrierProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.packageReceivedTopic = packageReceivedTopic;
        this.carrierProperties = carrierProperties;
    }

    @KafkaListener(
            topics = "${kafka.topics.return-label-ready}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleLabelReady(ReturnLabelReadyEvent event) {
        // Fall back to orderId if returnId was not populated in the event
        String effectiveReturnId = (event.returnId() != null && !event.returnId().isBlank())
                ? event.returnId()
                : "RET-" + (event.orderId() != null ? event.orderId() : "UNKNOWN");

        long transitDelaySeconds = carrierProperties.simulator().transitDelaySeconds();

        System.out.printf("[%s] Label generated for Return ID: %s. Simulating %d-second transit delay...%n",
                carrierProperties.name(), effectiveReturnId, transitDelaySeconds);

        scheduler.schedule(() -> {
            PackageReceivedEvent receivedEvent = new PackageReceivedEvent(
                    effectiveReturnId,
                    event.orderId(),
                    event.sku(),
                    event.quantity(),
                    Instant.now()
            );

            kafkaTemplate.send(packageReceivedTopic, effectiveReturnId, receivedEvent);
            System.out.printf("[%s] Package delivered to warehouse dock for Return ID: %s. Emitted 'package-received'%n",
                    carrierProperties.name(), effectiveReturnId);
        }, transitDelaySeconds, TimeUnit.SECONDS);
    }
}