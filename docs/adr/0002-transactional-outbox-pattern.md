# ADR 0002: Transactional Outbox Pattern for Non-Blocking Event Emission

* **Status:** Accepted
* **Date:** 2026-10-04
* **Deciders:** James Hayes (Staff Engineer)
* **Technical Domain:** Ingress Persistence & Event Reliability

---

## 1. Context & Problem Statement
In `returns-service` (:8000), accepting a return requires writing the return state to Azure SQL Edge (`returns_db`) and publishing a `returns.order.initiated.v1` event to Kafka. Executing a direct `kafkaProducer.send()` inside the HTTP request loop creates a dual-write vulnerability: if the database transaction commits but the Kafka broker is temporarily unreachable, the system enters an inconsistent state where downstream services never process the return.

---

## 2. Decision Drivers & Forces
* **Zero Event Loss:** Guarantee at-least-once event publication to Kafka even during broker downtime.
* **Latency Isolation:** HTTP API responses (`POST /api/v1/returns`) must not fail or block due to transient Kafka network delays.

---

## 3. Options Considered
1. **Direct Kafka Send inside Controller/Service:** Simple, but risks ghost database records on Kafka transport failures.
2. **Two-Phase Commit (2PC / XA):** Guarantees distributed transaction consistency, but introduces heavy blocking locks and severe latency.
3. **Transactional Outbox Pattern:** Writes return data and an outbox event payload to `returns_db` in a single atomic R2DBC transaction. A background poller reads and streams outbox records to Kafka.

---

## 4. Decision Outcome
**Chosen Option:** Option 3 — Transactional Outbox Pattern.

We will write `returns` state and `outbox` records atomically using R2DBC `@Transactional` reactive boundaries, paired with a scheduled outbox publisher streaming to `returns.order.initiated.v1`.

---

## 5. System Consequences & Trade-offs
* **Positive:** Eliminates dual-write data loss; client HTTP responses remain fast regardless of Kafka availability.
* **Negative:** Introduces eventual consistency (polling lag ~500ms) and potential duplicate event delivery during worker restarts, requiring downstream idempotency.