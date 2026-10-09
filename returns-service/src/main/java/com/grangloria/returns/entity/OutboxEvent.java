package com.grangloria.returns.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("outbox")
public record OutboxEvent(
        @Id Long id,
        @Column("aggregate_type") String aggregateType,
        @Column("aggregate_id") String aggregateId,
        @Column("event_type") String eventType,
        String payload,
        Boolean processed,
        @Column("created_at") LocalDateTime createdAt
) {
    public static OutboxEvent create(String aggregateType, String aggregateId, String eventType, String payload) {
        return new OutboxEvent(null, aggregateType, aggregateId, eventType, payload, false, LocalDateTime.now());
    }

    public OutboxEvent markProcessed() {
        return new OutboxEvent(this.id, this.aggregateType, this.aggregateId, this.eventType, this.payload, true, this.createdAt);
    }
}