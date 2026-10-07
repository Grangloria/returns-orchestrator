# RFC-001: Returns Orchestrator Platform Architecture

| Field | Value |
| :--- | :--- |
| **Status** | Approved |
| **Author** | Principal Architect |
| **Created** | 2026-10-06 |
| **Domain** | Supply Chain / Carrier Integration / Logistics |

---

## 1. Context & Business Problem
E-commerce return processing requires real-time coordination across third-party freight carriers, warehouse inventory systems, and payment gateways. Legacy synchronous systems suffer from carrier API latency and double-refund risks during retry storms.

## 2. High-Level Architecture
The **Returns Orchestrator** is an event-driven, reactive microservices platform built on Spring WebFlux, R2DBC, Apache Kafka, and Redis (Redlock).

```text
[ Client / Webhook ] ──> [ API Gateway ] ──> [ Carrier Gateway ]
                                                    │
                                           (Kafka Event Bus)
                                                    │
                      ┌─────────────────────────────┼─────────────────────────────┐
                      ▼                             ▼                             ▼
              [ Returns Service ]          [ Refund Service ]           [ Inventory Service ]
                      │                             │                             │
               (R2DBC Postgres)             (Redis Redlock)               (R2DBC Postgres)
```

## 3. Technology Stack & Trade-Offs

| Component | Choice | Decision Rationale |
| :--- | :--- | :--- |
| **Reactive Core** | Spring WebFlux + Netty | Non-blocking I/O maximizing hardware concurrency under high carrier webhook volume. |
| **Persistence** | PostgreSQL + R2DBC | Non-blocking reactive database access preventing thread pool exhaustion. |
| **Event Streaming** | Reactor-Kafka | High-throughput, asynchronous event streaming with reactive backpressure support. |
| **Distributed Locking**| Redis (Redlock) | Enforces idempotency during concurrent financial refund processing. |

## 4. Key Design Decisions
* **Outbox Pattern:** Guarantees atomic database writes and Kafka event publishing without distributed 2PC transactions.
* **Reactive Backpressure:** Uses Reactor operators (`onBackpressureBuffer`) to prevent memory overflow during carrier traffic spikes.