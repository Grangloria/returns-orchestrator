# Service Design Document: [Service Name]

| Metadata | Value |
| :--- | :--- |
| **Service Tier** | Tier 1 (Critical Path) / Tier 2 / Tier 3 |
| **Bounded Context** | [e.g., Returns Fulfillment Domain] |
| **Owner Team** | [e.g., Core Platform Team] |
| **Tech Stack** | Java 21, Spring WebFlux, Reactor-Kafka, R2DBC, PostgreSQL, Redis |
| **Status** | Draft / Under Review / Approved / Production |

---

## 1. Domain Scope & Bounded Context
* **Core Responsibilities:** Detailed bullet points defining the business capability owned exclusively by this service (e.g., managing return state transitions).
* **Non-Goals & Delegations:** Explicit boundaries defining what this service does NOT handle and which upstream/downstream services own those tasks.

## 2. Service Component Architecture
High-level architectural flow showing inbound controllers, business orchestrators, data access layers, and outbound messaging.

    [ REST Gateway / Client ]
                │
                ▼ (Reactive HTTP / WebFlux)
    ┌────────────────────────────────────────────────────────┐
    │                   [ Microservice ]                     │
    │  ┌───────────────────┐      ┌───────────────────────┐  │
    │  │ Reactive Router   │ ────►│ Saga / Business Logic │  │
    │  └───────────────────┘      └───────────┬───────────┘  │
    └─────────────────────────────────────────┼──────────────┘
                                              ▼
                                  ┌───────────────────────┐
                                  │ R2DBC / Redis / Kafka │
                                  └───────────────────────┘

## 3. Data Storage & Persistence Model
* **Primary Database:** Engine type, reactive driver, and connection pool limits (e.g., PostgreSQL + R2DBC).
* **Schema & Entities:** Core domain tables, primary/foreign keys, and indices.
* **Outbox Table Schema:** Structural definition for transactional outbox tables (`id`, `aggregate_id`, `payload`, `status`, `created_at`).
* **Cache Management:** Redis key namespaces, TTL expiration strategies, and eviction policies.

## 4. Resiliency & Concurrency Controls
* **Circuit Breakers:** Resilience4j failure thresholds, ring buffer sizes, and open-state wait durations.
* **Distributed Locks:** Redis Redlock key patterns, lease durations, and retry acquisition intervals.
* **Idempotency Strategy:** Mechanism for processing requests/events exactly once using headers or event IDs paired with Redis TTL checks.
* **Reactive Backpressure:** Stream limits, buffer capacities, and overflow handling strategies (`OnBackpressureBuffer`).

## 5. API & Event Specifications
* **Inbound REST Endpoints:** HTTP path, method, headers, and reference to `docs/api/openapi/`.
* **Kafka Event Publishing:** Topic names, partition keys, and schema references.
* **Kafka Event Consumption:** Consumer groups, retry policies, and Dead Letter Topics (`.DLT`).

## 6. Observability & SRE Metrics
* **Prometheus Metrics:** Service-specific counters, gauges, and histograms (`http_requests_total`, `kafka_consumer_lag`).
* **Distributed Tracing:** OpenTelemetry context propagation across WebFlux thread boundaries and Kafka event headers.