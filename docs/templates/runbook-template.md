# Service Operations Runbook: [Service Name]

| Operational Metadata | Link / Command / Identifier |
| :--- | :--- |
| **Escalation Policy** | `PD-PLATFORM-TIER1` |
| **Grafana Dashboard** | `https://grafana.internal/d/return-orchestrator` |
| **Logs Query** | `{app="return-orchestrator-service"}` |
| **K8s Namespace** | `returns-prod` |

---

## 1. System Health Diagnostics
* **Liveness Probe:** `GET /actuator/health/liveness`
* **Readiness Probe:** `GET /actuator/health/readiness`
* **Consumer Lag Query:** `sum(kafka_consumergroup_lag{topic="returns.raw-ingestion.v1"}) by (consumergroup)`
* **Circuit Breaker Status Query:** `resilience4j_circuitbreaker_state`

---

## 2. Operational Incident Remediation

### Scenario A: High Kafka Consumer Lag
1. Verify if consumer pods are in crash loops or restarting:
   ```bash
   kubectl get pods -n returns-prod -l app=inventory-service
   ```
2. Check application logs for downstream connection timeouts or database pool exhaustion:
   ```bash
   kubectl logs -n returns-prod -l app=inventory-service --tail=200
   ```
3. Scale consumer deployment up to match max topic partition count:
   ```bash
   kubectl scale deployment inventory-service -n returns-prod --replicas=10
   ```

### Scenario B: Dead Letter Topic (DLT) Backlog
1. Query Loki logs to identify exception types attached to dead-lettered message headers.
2. Resolve underlying infrastructure blockages (e.g., database network partition or locked table).
3. Trigger automated re-drive job to move messages from DLT back to primary processing queue:
   ```bash
   kubectl create job --from=cronjob/dlq-redrive-job manual-redrive-001 -n returns-prod
   ```

### Scenario C: Redis Redlock Contention / Stale Locks
1. Check Redis CPU and memory saturation metrics on ElastiCache dashboard.
2. Manually clear orphan lock keys if a node pod crashed mid-flight before releasing its lock:
   ```bash
   redis-cli -h redis.prod.internal -p 6379 DEL "lock:sku:SKU-8831-XL"
   ```