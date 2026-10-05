# ADR [NUMBER]: [SHORT TITLE OF DECISION]

* **Status:** [Proposed | Accepted | Rejected | Superseded by ADR-XXXX]
* **Date:** YYYY-MM-DD
* **Deciders:** [Name/Role, e.g., James Hayes (Staff Engineer)]
* **Technical Domain:** [e.g., Ingress / Persistence / Messaging / Financial Execution]

---

## 1. Context & Problem Statement
[Describe the technical context and the specific operational challenge or risk being addressed. Explain what happens if this decision is not addressed.]

*Example:* The `returns-service` needs to save state to the database and emit an event to Kafka. Executing these as two uncoordinated calls creates a dual-write vulnerability where database commits succeed but event publishing fails.

---

## 2. Decision Drivers & Forces
* **Reliability:** Must guarantee zero message loss across network or database partitions.
* **Performance:** Must maintain non-blocking execution using Java 21, Spring WebFlux, and R2DBC.
* **Maintainability:** Avoid complex Two-Phase Commit (2PC) or heavyweight distributed locking where simple patterns suffice.

---

## 3. Options Considered
1. **[Option 1 - Status Quo / Naive Approach]:** [Brief description + pros/cons]
2. **[Option 2 - Alternative Architecture]:** [Brief description + pros/cons]
3. **[Option 3 - Selected Approach]:** [Brief description + pros/cons]

---

## 4. Decision Outcome
**Chosen Option:** [Option 3 - e.g., Transactional Outbox Pattern with R2DBC Poller]

### Justification
[Explain why this option best addresses the decision drivers. Highlight key technical trade-offs.]

---

## 5. System Consequences & Trade-offs

### Positive
* [Key benefit 1, e.g., Guarantees at-least-once event delivery to Kafka]
* [Key benefit 2, e.g., Decouples HTTP ingress response latency from Kafka broker availability]

### Negative & Mitigation
* **Drawback:** Introduces eventual consistency with minor polling lag.
    * *Mitigation:* Configured outbox poller interval to 500ms to keep end-to-end latency below 1 second.
* **Drawback:** Risk of duplicate event delivery on poller restarts.
    * *Mitigation:* Downstream consumers (`carrier-gateway`, `refund-service`) enforce idempotency checks.

---

## 6. Observability & Verification
* **Metrics:** Tracked via `outbox_unprocessed_records_total` in Prometheus.
* **Logs:** MDC trace correlation via `traceId` and `spanId` logged to Loki.