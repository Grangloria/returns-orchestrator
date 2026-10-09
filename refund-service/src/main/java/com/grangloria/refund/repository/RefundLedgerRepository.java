package com.grangloria.refund.repository;

import com.grangloria.refund.entity.RefundLedger;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface RefundLedgerRepository extends ReactiveCrudRepository<RefundLedger, Long> {
    Mono<Boolean> existsByEventId(String eventId);
}