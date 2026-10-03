package com.grangloria.returns.messaging.consumer;

import com.returns.common.event.PackageReceivedEvent;
import com.grangloria.returns.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PackageReceivedConsumer {

    private final ReturnService returnService;

    @KafkaListener(
            topics = "${kafka.topics.package-received:returns.package.received.v1}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePackageReceivedEvent(PackageReceivedEvent event) {
        log.info("[RETURN-SERVICE-CONSUMER] Processing PackageReceivedEvent for Order: [{}]", event.orderId());

        returnService.handlePackageReceived(event)
                .subscribe(
                        null,
                        error -> log.error("[RETURN-SERVICE-ERROR] Failed updating state to RECEIVED for Order [{}]: {}",
                                event.orderId(), error.getMessage())
                );
    }
}