# ADR 0004: Restock-Gated Financial Settlement & Redis Redlock Idempotency

* **Status:** Accepted
* **Date:** 2026-10-04
* **Deciders:** James Hayes (Staff Engineer)
* **Technical Domain:** Financial Execution & Idempotency Controls

---

## 1. Context & Problem Statement
Issuing financial refunds before physical item intake creates fraud and loss exposure. Furthermore, in event-driven systems with at-least-once delivery semantics, redelivered Kafka events on `returns.inventory.restocked.v1` could cause duplicate payouts if processed multiple times by `refund-service` (:8006).

---

## 2. Decision Drivers & Forces
* **Loss Prevention:** Financial payout must strictly depend on warehouse intake completion.
* **Exact-Once Financial Processing:** Prevent double payouts under event redelivery or concurrent processing.

---

## 3. Options Considered
1. **Immediate Refund on Return Creation:** High fraud risk; violates enterprise retail accounting controls.
2. **Restock-Gated Execution with Redis Distributed Locking & R2DBC Ledger:**
    * `refund-service` strictly consumes `returns.inventory.restocked.v1` emitted by `inventory-service` (:8001).
    * Before executing credit, `refund-service` acquires a **Redis Redlock** and verifies an `X-Idempotency-Key` or event ID.
    * Payout entries are appended to an immutable R2DBC `refund_ledger` in `refund_db`.

---

## 4. Decision Outcome
**Chosen Option:** Option 2 — Restock-Gated Execution with Redis Redlock & Immutable R2DBC Ledger.

---

## 5. System Consequences & Trade-offs
* **Positive:** Guarantees financial compliance and eliminates duplicate payouts.
* **Negative:** Adds Redis as a critical runtime dependency for distributed locking.