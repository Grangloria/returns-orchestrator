# Runbook Directory & On-Call Operating Index

| Metadata | Details |
| :--- | :--- |
| **System** | Returns Orchestrator Platform |
| **Primary On-Call** | `#logistics-oncall` (PagerDuty: `returns-orchestrator-tier1`) |
| **Escalation SLA** | P1: < 15 mins \| P2: < 30 mins \| P3: < 4 hours |
| **Grafana Dashboard** | `https://grafana.internal/d/returns-orchestrator-overview` |
| **Loki Log Query** | `{app="returns-orchestrator"}` |

---

## Escalation Matrix

* **Severity 1 (P1 - Critical):** Core ingestion blocked, data loss risk, financial refund double-processing. Immediate PagerDuty page.
* **Severity 2 (P2 - Major):** High consumer lag (>10k messages), elevated p99 latency (>200ms), single node failure in cluster.
* **Severity 3 (P3 - Minor):** Non-blocking retry spikes, minor DLQ accumulation (<100 messages), non-critical telemetry drops.

---

## Active Operational Runbooks

| Alert / Issue | Severity | Target Component | Runbook Link |
| :--- | :--- | :--- | :--- |
| **High Kafka Consumer Lag** | `P2 - Major` | `carrier-gateway`, `inventory-svc` | [`KAFKA-CONSUMER-LAG-AND-DLQ.md`](./KAFKA-CONSUMER-LAG-AND-DLQ.md) |
| **Redis Redlock Lock Exhaustion** | `P1 - Critical` | `refund-authorization-svc` | [`REDIS-LOCK-EXHAUSTION.md`](./REDIS-LOCK-EXHAUSTION.md) |
| **R2DBC Pool Exhaustion** | `P2 - Major` | Reactive PostgreSQL / SQL Server | [`R2DBC-POOL-STARVATION.md`](./R2DBC-POOL-STARVATION.md) |
| **Transactional Outbox Stuck** | `P1 - Critical` | `outbox-publisher-service` | [`TRANSACTIONAL-OUTBOX-STUCK.md`](./TRANSACTIONAL-OUTBOX-STUCK.md) |
| **Carrier API Circuit Opened** | `P2 - Major` | Resilience4j WebFlux Gateway | [`CIRCUIT-BREAKER-OPEN.md`](./CIRCUIT-BREAKER-OPEN.md) |

---

## Quick Diagnostic Commands

```bash
# 1. Health check all 7 reactive microservices
curl -s http://localhost:8080/actuator/health | jq .

# 2. Check total Kafka consumer lag across all active consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups

# 3. Check Redis memory and lock count
redis-cli -h localhost -p 6379 INFO memory
```