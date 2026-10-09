package com.grangloria.returns.repository;

import com.grangloria.returns.entity.OutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OutboxRepository extends ReactiveCrudRepository<OutboxEvent, Long> {
    Flux<OutboxEvent> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}