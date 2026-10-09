package com.grangloria.returns.messaging.publisher;

import com.grangloria.returns.repository.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@EnableScheduling
public class OutboxEventPublisher
{

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String returnInitiatedTopic;

    public OutboxEventPublisher(
            OutboxRepository outboxRepository,
            @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            @Value("${kafka.topics.return-initiated:returns.order.initiated.v1}") String returnInitiatedTopic
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.returnInitiatedTopic = returnInitiatedTopic;
    }

    @Scheduled(fixedDelay = 500)
    public void processOutboxEvents() {
        outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc()
                .concatMap(outbox -> {
                    log.info("Publishing outbox event [{}] of type [{}] to topic [{}] for aggregate [{}]",
                            outbox.id(), outbox.eventType(), returnInitiatedTopic, outbox.aggregateId());

                    ProducerRecord<String, String> record = new ProducerRecord<>(
                            returnInitiatedTopic,
                            outbox.aggregateId(),
                            outbox.payload()
                    );

                    record.headers().add(new RecordHeader("__TypeId__",
                            "com.returns.common.event.ReturnInitiatedEvent".getBytes(StandardCharsets.UTF_8)));

                    return Mono.fromFuture(kafkaTemplate.send(record))
                            .then(outboxRepository.save(outbox.markProcessed()))
                            .doOnError(ex -> log.error("Failed to process outbox id {}: {}", outbox.id(), ex.getMessage()));
                })
                .subscribe();
    }
}