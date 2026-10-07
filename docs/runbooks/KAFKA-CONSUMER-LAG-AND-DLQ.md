# Operational Runbook: Kafka Consumer Lag & DLQ Remediation

| Field | Detail |
| :--- | :--- |
| **Service Target** | `returns-orchestrator-carrier-gateway`, `returns-orchestrator-inventory` |
| **Alert Trigger** | `KafkaConsumerLagHigh` (Lag > 10,000 messages for > 5 minutes) |
| **Severity Level** | **P2 - Major** (Potential event delay; eventual consistency lagging) |
| **On-Call Escalation** | Logistics Integration / Messaging Platform Team |

---

## 1. Triage & Diagnosis (First 5 Minutes)

### Step 1.1: Verify Alert & Identify Stuck Consumer Group
Run the Kafka consumer group CLI tool or check the Grafana dashboard to identify which partition or consumer instance is stalling.

```bash
# Check lag across all active consumer groups in the cluster
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group returns-processing-group
```

*Look at the `LAG` and `CURRENT-OFFSET` columns. If lag is steadily increasing while `CURRENT-OFFSET` is stationary, the consumer thread is stuck or throwing unhandled reactive stream exceptions.*

### Step 1.2: Inspect Loki Logs for Backpressure or Thread Starvation
Execute the following LogQL query in Grafana Loki to search for non-blocking stream failures or database connection pool exhaustion:

```logql
{app="returns-orchestrator"} |= "Reactor-Kafka" |= "ERROR"
```

Look for signatures such as:
* `java.lang.OutOfMemoryError: Direct buffer memory` (Netty buffer leak)
* `r2dbc.spi.R2dbcTimeoutException` (SQL Server R2DBC connection pool lockup)
* `org.springframework.dao.TransientDataAccessException`

---

## 2. Immediate Mitigation ("Stop the Bleeding")

### Option A: Scale Out Consumer Pods (If CPU / Throughput Bound)
If consumer CPU utilization is at > 85% and partition count permits, scale up the reactive service deployment to rebalance partitions across more workers.

```bash
# Scale the container instances in Docker Compose / K8s
docker compose up -d --scale carrier-gateway-service=4
```

### Option B: Throttle Reactive Ingestion Backpressure
If database write performance (R2DBC) is the bottleneck, temporarily lower the reactive prefetch limit via Spring Environment property overrides to prevent thread starvation.

```bash
# Update reactive batch prefetch dynamically via Spring Boot Actuator
curl -X POST http://localhost:8081/actuator/env \
  -H "Content-Type: application/json" \
  -d '{"name":"spring.kafka.consumer.properties.max.poll.records", "value":"100"}'

curl -X POST http://localhost:8081/actuator/restart
```

---

## 3. Dead-Letter Queue (DLQ) Triage & Event Replay

When events fail deserialization or exceed retry limits (3 retries with exponential backoff), Reactor-Kafka routes them to `returns.carrier-gateway.dlq`.

### Step 3.1: Inspect Poison-Pill Payloads
Dump the last 5 messages from the Dead-Letter Queue to inspect headers and failure stack traces:

```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic returns.carrier-gateway.dlq \
  --max-messages 5 \
  --property print.headers=true \
  --from-beginning
```

Common causes:
* **Schema Mismatch:** Carrier sent an unexpected null field in `carrier_tracking_num`.
* **Lock Timeout:** Redis Redlock lease expired before financial refund authorization completed.

### Step 3.2: Replay Validated DLQ Messages
Once the root cause is resolved or upstream data is corrected, run the administrative replay utility to re-inject messages from the DLQ back into the primary processing topic (`returns.raw-ingestion`).

```bash
# Execute the internal CLI replay job
docker exec -it returns-orchestrator-app \
  java -jar app.jar --job=ReplayDLQ \
  --source-topic=returns.carrier-gateway.dlq \
  --target-topic=returns.raw-ingestion \
  --batch-size=500
```

---

## 4. Verification & Recovery

1. **Verify Lag Resolution:** Confirm `kafka-consumer-groups.sh` shows `LAG` returning to < 500 records.
2. **Check System Health:** Ensure HTTP `/actuator/health` returns `UP` for all 7 microservice instances.
3. **Log Resolution:** Post an incident summary update in the emergency slack channel `#incident-log` detailing:
   * Total messages affected / routed to DLQ.
   * Root cause (e.g., third-party API timeout, database index fragmentation).
   * Duration of error budget impact.