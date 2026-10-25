# Non-Functional Requirements (NFR) & Resilience Target

* **Scope**: [Global System | Service: e.g., carrier-gateway]
* **Criticality**: [Tier-1 High Availability | Tier-2 Standard]
* **Compliance Boundary**: [PCI-DSS | SOC2 | GDPR]

---

## 1. Latency & Performance SLA / SLO

| Transaction / Endpoint | Target Volume | P50 Target | P95 Target | P99 Target |
| :--- | :--- | :--- | :--- | :--- |
| `POST /api/v1/returns` | 300 RPS | < 80ms | < 250ms | < 500ms |
| `GET /api/v1/returns/{id}`| 1,200 RPS | < 20ms | < 50ms | < 120ms |
| Async Kafka Consumption | 2,500 msg/sec | < 100ms | < 500ms | < 2,000ms |

---

## 2. Resilience, Circuit Breakers & Retries (Resilience4j)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      downstreamClient:
        sliding-window-size: 50
        failure-rate-threshold: 50.0
        slow-call-rate-threshold: 75.0
        slow-call-duration-threshold: 2000ms
        wait-duration-in-open-state: 10000ms
        permitted-number-of-calls-in-half-open-state: 10
  retry:
    instances:
      downstreamClient:
        max-attempts: 3
        wait-duration: 200ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
```

---

## 3. ECS Fargate Scalability & Infrastructure Sizing

* **Base Configuration**: `0.5 vCPU / 1024 MB Memory`
* **Min Tasks**: `2` (Across distinct Availability Zones)
* **Max Tasks**: `12`
* **Target Tracking Metric 1**: CPU Utilization > 70% for 2 minutes -> Scale Out (+2)
* **Target Tracking Metric 2**: Memory Utilization > 75% -> Scale Out (+2)
* **Scale-in Cooldown**: 300 seconds

---

## 4. Disaster Recovery & Data Durability

* **Recovery Point Objective (RPO)**: `< 5 minutes` (Point-in-time Aurora backup / Multi-AZ Kafka)
* **Recovery Time Objective (RTO)**: `< 30 minutes` (Regional infrastructure redeployment via Terraform)