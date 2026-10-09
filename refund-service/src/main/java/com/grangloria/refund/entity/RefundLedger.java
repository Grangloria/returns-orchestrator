package com.grangloria.refund.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("refund_ledger")
public record RefundLedger(
        @Id Long id,
        @Column("event_id") String eventId,
        @Column("order_id") String orderId,
        BigDecimal amount,
        String status,
        @Column("created_at") LocalDateTime createdAt
) {
    public static RefundLedger create(String eventId, String orderId, BigDecimal amount, String status) {
        return new RefundLedger(null, eventId, orderId, amount, status, LocalDateTime.now());
    }
}