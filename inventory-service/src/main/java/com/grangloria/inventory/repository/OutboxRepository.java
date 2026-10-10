package com.grangloria.inventory.repository;

import com.grangloria.inventory.entity.OutboxEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface OutboxRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    Flux<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
}