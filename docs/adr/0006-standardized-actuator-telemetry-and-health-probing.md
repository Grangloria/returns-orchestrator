# ADR 0006: Standardized Spring Boot Actuator Telemetry & Health Probing

* **Status:** Accepted
* **Date:** 2026-10-04
* **Deciders:** James Hayes (Staff Engineer)
* **Technical Domain:** Observability, Container Lifecycle & Operations

---

## 1. Context & Problem Statement
In a distributed event-driven ecosystem comprising 7 microservices, operating without real-time runtime diagnostics creates "black box" failure modes. Specifically:
1. Container orchestrators (Docker Compose, Kubernetes, AWS ECS) need a non-intrusive way to probe whether a service process is alive or stalled.
2. Time-series metrics engines (Prometheus) require a pull-based endpoint to collect JVM memory state, thread pool activity, R2DBC connection pool usage, and Kafka consumer lag without coupling business code to metrics logic.

---

## 2. Decision Drivers & Forces
* **Zero-Boilerplate Telemetry:** Expose deep runtime metrics across JVM, Reactor Netty, R2DBC, and Kafka without writing custom HTTP controllers.
* **Pull-Based Metrics Scraping:** Format metric data in standardized PromQL exposition format for Prometheus ingestion.
* **Container Lifecycle Parity:** Expose standardized health endpoints (`/actuator/health`) for liveness and readiness probes across local and cloud environments.

---

## 3. Options Considered
1. **Custom RestControllers for Metrics & Health:** Manually creating endpoints in every microservice. High boilerplate overhead, non-standard metric formats, and fragile maintenance.
2. **Push-Based Metrics Agent (StatsD / JMX Exporter):** Pushes metrics out over UDP/TCP. Adds network overhead and agent configuration complexity inside container images.
3. **Spring Boot Actuator + Micrometer Prometheus Registry:** Exposes `/actuator/health` and `/actuator/prometheus` automatically via runtime inspection and classpath capabilities.

---

## 4. Decision Outcome
**Chosen Option:** Option 3 — Spring Boot Actuator + Micrometer Prometheus Registry.

All microservices configure standard `management.endpoints.web.exposure.include` blocks in `application.yml`, allowing Prometheus to scrape metrics on port boundaries every 5 seconds and Docker Compose to monitor liveness.

---

## 5. System Consequences & Trade-offs

### Positive
* **Unified Observability Tagging:** `management.metrics.tags.application=${spring.application.name}` automatically namespace-stamps metrics for Grafana dashboard aggregation (`Dashboard ID: 4701`).
* **Non-Blocking Execution:** Actuator endpoints operate asynchronously on Netty reactor loops, introducing negligible CPU/memory overhead.
* **Container Health Reliability:** Enables sidecars and orchestrators to restart unhealthy containers automatically.

### Negative & Mitigation
* **Security Exposure Risk:** Exposing management endpoints publicly can leak environmental details.
    * *Mitigation:* In production, Actuator ports are bound to private internal networks (VPC) or protected behind API Gateway path-stripping rules (`/actuator/**` blocked from public ingress).