# Service Level Objectives (SLO) & Reliability Contract

| Field | Value |
| :--- | :--- |
| **Target System** | Returns Orchestrator Microservices |
| **Review Cycle** | Quarterly |
| **Monitoring Tool**| Prometheus / Grafana / Alertmanager |

---

## 1. Core Service Level Objectives (SLOs)

| Service | Indicator (SLI) | Target (SLO) | Measurement Window |
| :--- | :--- | :--- | :--- |
| `carrier-gateway` | Ingestion Availability | **99.95%** success rate | 30-Day Rolling |
| `carrier-gateway` | Webhook Latency | **p99 < 150ms** | 30-Day Rolling |
| `refund-service` | Financial Processing | **100% Idempotency** (Zero double-refunds) | Continuous |
| `inventory-svc` | Kafka Consumer Lag | **Lag < 1,000 events** | 5-Minute Window |

---

## 2. Error Budget & Burn Rate Policies

* **Monthly Error Budget:** 0.05% (~21.6 minutes of downtime per month for `carrier-gateway`).
* **Burn Rate Alerts:**
    * **2% Budget Consumed in 1 Hour:** Fires `P2 - Major` Slack alert to `#logistics-oncall`.
    * **5% Budget Consumed in 6 Hours:** Fires `P1 - Critical` PagerDuty page.

## 3. Action Plan On SLO Breach
1. Freeze all non-essential feature deployments for the affected service.
2. Divert engineering resources to reliability bugs and technical debt reduction until the error budget recovers.