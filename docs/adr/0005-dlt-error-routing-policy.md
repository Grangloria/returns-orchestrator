# ADR 0005: Dead Letter Topic (DLT) Routing Policy for Poison Payload Isolation

* **Status:** Accepted
* **Date:** 2026-10-04
* **Deciders:** James Hayes (Staff Engineer)
* **Technical Domain:** Messaging Fault Tolerance & Error Handling

---

## 1. Context & Problem Statement
When `carrier-gateway` (:8004) or `notification-service` (:8002) encounters a malformed payload or unhandled deserialization error on `returns.label.ready.v1`, standard Kafka consumers risk entering infinite retry loops. This blocks partition offset progression and stalls downstream message processing for healthy events.

---

## 2. Decision Drivers & Forces
* **Consumer Availability:** Ensure unprocessable "poison pill" payloads do not halt entire consumer partitions.
* **Observability:** Route failed payloads to a dedicated inspection queue for operational auditing and alerting.

---

## 3. Options Considered
1. **Block & Retry Indefinitely:** Halts pipeline consumer progress across partition offsets.
2. **Log & Drop Message:** Silent failure; leads to missing customer labels and untrackable data loss.
3. **Dead Letter Topic (DLT) Redirect:** Configure Spring Kafka `ErrorHandlingDeserializer` and retry backoffs. Route permanently failed messages to `returns.label.ready.v1.DLT`.

---

## 4. Decision Outcome
**Chosen Option:** Option 3 — Dead Letter Topic (DLT) Redirect Strategy.

Unprocessable messages on `returns.label.ready.v1` will redirect to `returns.label.ready.v1.DLT` after exhausting retries, triggering an operational alert in `notification-service`.

---

## 5. System Consequences & Trade-offs
* **Positive:** Prevents pipeline stalls; provides an isolated audit topic for debugging bad payloads.
* **Negative:** Requires active monitoring and manual or automated re-drive tooling for DLT events.