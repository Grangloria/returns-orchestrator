package com.grangloria.refund.listener;

import com.grangloria.refund.entity.RefundLedger;
import com.grangloria.refund.repository.RefundLedgerRepository;
import com.returns.common.event.InventoryRestockedEvent;
import com.returns.common.event.RefundCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RefundLedgerRepository refundLedgerRepository;
    private final RedissonReactiveClient redissonReactiveClient;

    @Value("${kafka.topics.refund-completed:returns.refund.completed.v1}")
    private String refundCompletedTopic;

    @KafkaListener(
            topics = "${kafka.topics.inventory-restocked:returns.inventory.restocked.v1}",
            groupId = "${spring.kafka.consumer.group-id:refund-service-group}",
            properties = {"spring.json.value.default.type=com.returns.common.event.InventoryRestockedEvent"}
    )
    public void processRefund(InventoryRestockedEvent event) {
        if (event == null) {
            log.warn("[REFUND-SERVICE] Received null or un-parsable InventoryRestockedEvent envelope.");
            return;
        }

        String lockKey = "lock:refund:" + event.orderId();
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);
        String eventId = (event.returnId() != null ? event.returnId() : event.orderId()) + "-RESTOCKED";

        Mono.from(lock.tryLock(5, 10, TimeUnit.SECONDS))
                .flatMap(acquired -> {
                    if (!acquired) {
                        log.warn("[REDLOCK] Could not acquire lock [{}] for Order ID: [{}]. Processing skipped or concurrently locked.",
                                lockKey, event.orderId());
                        return Mono.empty();
                    }

                    log.info("[REDLOCK] Acquired lock [{}] for Order ID: [{}]", lockKey, event.orderId());

                    return refundLedgerRepository.existsByEventId(eventId)
                            .flatMap(exists -> {
                                if (exists) {
                                    log.info("[IDEMPOTENCY] Event [{}] already recorded in refund_ledger. Skipping duplicate payout for Order ID: [{}]",
                                            eventId, event.orderId());
                                    return Mono.empty();
                                }

                                log.info("[REFUND-SERVICE] Consumed InventoryRestockedEvent for Return ID: [{}], Order ID: [{}]. Initiating payout of [{}]...",
                                        event.returnId(), event.orderId(), event.refundAmount());

                                String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                                BigDecimal amount = !event.refundAmount().equals(BigDecimal.ZERO) ? event.refundAmount() : BigDecimal.ZERO;

                                RefundLedger ledgerEntry = RefundLedger.create(
                                        eventId,
                                        event.orderId(),
                                        amount,
                                        "COMPLETED"
                                );

                                return refundLedgerRepository.save(ledgerEntry)
                                        .doOnSuccess(savedLedger -> {
                                            RefundCompletedEvent refundEvent = new RefundCompletedEvent(
                                                    event.returnId(),
                                                    event.orderId(),
                                                    event.customerEmail(),
                                                    amount,
                                                    transactionId,
                                                    Instant.now()
                                            );

                                            kafkaTemplate.send(refundCompletedTopic, event.orderId(), refundEvent);

                                            log.info("[REFUND-SERVICE] Payout recorded in ledger & event emitted for Order ID: [{}]. Transaction ID: [{}]",
                                                    event.orderId(), transactionId);
                                        });
                            })
                            .doFinally(signal -> Mono.from(lock.unlock())
                                    .doOnSuccess(v -> log.info("[REDLOCK] Released lock [{}] for Order ID: [{}]", lockKey, event.orderId()))
                                    .doOnError(ex -> log.error("[REDLOCK-ERROR] Failed to release lock [{}] for Order ID: [{}]: {}", lockKey, event.orderId(), ex.getMessage()))
                                    .subscribe());
                })
                .subscribe();
    }
}