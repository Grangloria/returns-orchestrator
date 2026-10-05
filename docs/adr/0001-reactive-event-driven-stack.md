# ADR 0001: Reactive Non-Blocking Stack Selection (Java 21, WebFlux, R2DBC, Reactor-Kafka)

* **Status:** Accepted
* **Date:** 2026-10-04
* **Deciders:** James Hayes (Staff Engineer)
* **Technical Domain:** Core Ingress & Execution Runtime

---

## 1. Context & Problem Statement
High-concurrency reverse logistics systems must handle heavy peak traffic (e.g., post-holiday return spikes) involving multi-step asynchronous processing: HTTP ingress, database persistence, carrier label generation, and financial credit settlement. Traditional thread-per-request models (Spring MVC + JDBC) risk thread starvation, high memory overhead, and connection pool exhaustion when waiting on slow I/O or downstream services.

---

## 2. Decision Drivers & Forces
* **Throughput & Efficiency:** Maximize request handling per CPU core without thread-blocking stalls.
* **Non-Blocking End-to-End:** Ensure the reactive chain is unbroken from HTTP Netty sockets down to database disk operations and Kafka network sockets.
* **Modern Platform Alignment:** Utilize modern Java language features (Java 21 Records, Pattern Matching).

---

## 3. Options Considered
1. **Spring MVC + JDBC + Blocking Kafka Producer:** Traditional stack; easy to write, but susceptible to thread starvation under high I/O concurrency.
2. **Spring WebFlux + R2DBC (Azure SQL Edge) + Reactor-Kafka:** Fully non-blocking reactive stack operating on Project Reactor event loops (`Mono`/`Flux`).

---

## 4. Decision Outcome
**Chosen Option:** Option 2 — Reactive End-to-End Stack (Java 21, Spring WebFlux, R2DBC, Reactor-Kafka).

---

## 5. System Consequences & Trade-offs
* **Positive:** Drastically reduces thread memory overhead; achieves low tail-latencies under high event concurrency.
* **Negative:** Requires strict reactive programming discipline; blocking JDBC calls or blocking library functions must be completely avoided within `Mono`/`Flux` chains.