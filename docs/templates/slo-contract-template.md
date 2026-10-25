# Service Level Objective (SLO) Contract: [System / Service Name]

* **Service Classification**: [Tier-1 Core Dependency]
* **Effective Date**: [YYYY-MM-DD]
* **Review Cycle**: Quarterly

---

## 1. Service Level Indicators (SLIs)

* **Availability SLI**: The ratio of successful (non-5xx) HTTP requests to total HTTP requests measured at the API Gateway over a rolling 30-day window.
* **Latency SLI**: The proportion of valid HTTP requests served faster than 250ms (P95) measured at the service edge over a rolling 30-day window.
* **Event Processing SLI**: The percentage of Kafka events consumed and acknowledged within 1,000ms of publish timestamp.

---

## 2. SLO Targets & Error Budgets

| Metric | Target | Monthly Allowed Error Budget |
| :--- | :--- | :--- |
| **Availability** | `99.95%` | `21.6 minutes of downtime / month` |
| **P95 Latency** | `95.0% < 250ms` | `5% of valid requests may exceed 250ms` |
| **Event Processing**| `99.9% < 1000ms` | `0.1% delayed messages` |

---

## 3. Error Budget Policy & Escalation

* **Budget Consumption > 50%**: Alert engineering lead; prioritize reliability bugs in upcoming sprint.
* **Budget Consumption > 85%**: Code freeze on non-critical feature deployments; dedicated sprint on reliability.
* **Budget Exhausted (100%)**: Immediate Sev-2 incident response; halt all feature releases to production until error budget recovers.