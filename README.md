# Returns Orchestrator: Event-Driven Reactive Ecosystem

## 📋 Overview
An enterprise-grade, non-blocking 7-microservice architecture designed to orchestrate complex reverse logistics and return workflows using an **Event-Driven Architecture (EDA)**. Built with **Java 21**, **Spring WebFlux**, **R2DBC**, and **Reactor-Kafka**, this system executes end-to-end reactive processing from HTTP ingress down to non-blocking database persistence in **Azure SQL Edge**.

The ecosystem features a restock-gated financial settlement pipeline, asynchronous third-party carrier integration, dead-letter error isolation, and temporal shipment simulation—ensuring ultra-low latency, high event concurrency, and zero thread starvation under heavy load.

---

## 🗺️ System Governance & Architecture Documentation

Macro system design, event schemas, and trade-off records are maintained directly in the source repository:

* 📖 **[System Architecture & Flow Diagram](docs/architecture/ARCHITECTURE.md)**: Visual Mermaid pipeline, physical container port bindings, database schemas, and Kafka event contracts.
* 📜 **[Architecture Decision Records (ADRs)](docs/adr/)**: Documented design trade-offs (Transactional Outbox, R2DBC, Resilience4j, Redis Redlock, and DLT routing).

---

## 🏗️ Multi-Module Ecosystem Topology

This project is structured as a **Multi-Module Gradle Platform** enforcing domain isolation and shared contract enforcement across services:

| Subproject / Module | Port | Persistence / Storage | Primary Role & Lifecycle Description |
| :--- | :--- | :--- | :--- |
| **`common-events`** | N/A | Shared Library | Single source of truth containing immutable Java 21 Records, DTOs, and Kafka topic constants. |
| **`returns-service`** | `:8000` | Azure SQL Edge (`returns_db`) | Ingress entry point & Saga Orchestrator. Executes R2DBC SQL inserts and Transactional Outbox publishing. |
| **`inventory-service`** | `:8001` | Azure SQL Edge (`inventory_db`) | Warehouse intake worker that validates returned items, updates bin stock, and triggers the financial restock gate. |
| **`notification-service`** | `:8002` | Redis (Deduplication) | Asynchronous communications worker dispatching customer receipts and routing system DLT alerts. |
| **`carrier-service`** | `:8003` | Mock In-Memory | Mock 3rd-party logistics REST API simulating rate limits, carrier bill-of-lading (BOL), and network latency. |
| **`carrier-gateway`** | `:8004` | Azure SQL Edge | Logistics domain proxy wrapping external calls with Resilience4j circuit breakers and DLT fault routing. |
| **`mock-carrier-simulator`** | `:8005` | Stateful Memory | Temporal transit worker simulating a 5-second asynchronous package scan/drop-off delay before emitting scan events. |
| **`refund-service`** | `:8006` | Azure SQL Edge (`refund_db`) + Redis | Gated financial settlement engine. Enforces idempotency via Redis Redlock and appends R2DBC ledger credits. |

---

## ⚡ System Event Choreography

```text
[Postman / Client] ──(1. POST /api/v1/returns)──► [returns-service :8000] ──► (R2DBC SQL Edge)
                                                         │
                                               (2. Outbox Event)
                                                         │
                                                         ▼
                                            [returns.order.initiated.v1]
                                                         │
                                               (3. Consume Event)
                                                         │
                                                         ▼
                                             [carrier-gateway :8004] ──(HTTP WebClient)──► [carrier-service :8003]
                                                         │
                                               (4. Produce Event)
                                                         │
                                                         ▼
                                              [returns.label.ready.v1] ───(Deserialization Failure)──► [returns.label.ready.v1.DLT]
                                                         │
                                     ┌───────────────────┴───────────────────┐
                       (5a. Consume) │                                       │ (5b. Consume)
                                     ▼                                       ▼
                         [notification-service :8002]            [mock-carrier-simulator :8005]
                                                                             │
                                                                 (6. 5s Transit Delay)
                                                                             │
                                                                             ▼
                                                              [returns.package.received.v1]
                                                                             │
                                                                   (7. Consume & Restock)
                                                                             │
                                                                             ▼
                                                                  [inventory-service :8001]
                                                                             │
                                                                    (8. Restock Gate)
                                                                             │
                                                                             ▼
                                                              [returns.inventory.restocked.v1]
                                                                             │
                                                                   (9. Consume & Credit)
                                                                             │
                                                                             ▼
                                                                   [refund-service :8006] ──► (R2DBC Ledger Insert)
                                                                             │
                                                                   (10. Produce Event)
                                                                             │
                                                                             ▼
                                                             [returns.refund.completed.v1]