# ADR 0003: Carrier Integration Boundary & Async Transit Simulation

* **Status:** Accepted
* **Date:** 2026-10-04
* **Deciders:** James Hayes (Staff Engineer)
* **Technical Domain:** Logistics Tendering & Integration Resilience

---

## 1. Context & Problem Statement
Integrating directly with third-party logistics carriers (FedEx, UPS, ABF Freight) exposes internal microservices to third-party API instability, rate limiting, and network latency. Furthermore, testing real-world multi-day physical package transit locally requires an asynchronous simulation mechanism.

---

## 2. Decision Drivers & Forces
* **Domain Decoupling:** Keep internal return saga logic separate from external carrier API contracts.
* **Fault Tolerance:** Wrap third-party HTTP calls in circuit breakers to isolate downstream outages.
* **Realistic Testing Parity:** Simulate temporal package transit delays without blocking execution threads.

---

## 3. Options Considered
1. **Direct Third-Party HTTP Calls from Core Orchestrator:** Violates domain boundaries and exposes `returns-service` to external API failures.
2. **Dedicated Carrier Gateway + Mock API + Temporal Simulator:**
    * `carrier-gateway` (:8004): Internal logistics orchestrator wrapping WebClient calls with Resilience4j circuit breakers.
    * `carrier-service` (:8003): Mock REST API simulating carrier bill-of-lading (BOL) generation and network faults.
    * `mock-carrier-simulator` (:8005): Asynchronous transit worker adding a 5-second delay before emitting `returns.package.received.v1`.

---

## 4. Decision Outcome
**Chosen Option:** Option 2 — Decoupled Carrier Architecture (`carrier-gateway` + `carrier-service` + `mock-carrier-simulator`).

---

## 5. System Consequences & Trade-offs
* **Positive:** Isolates external API instability; enables full local simulation of physical transit lag and circuit breaker fallbacks.
* **Negative:** Increases container service footprint across local Docker Compose setups.