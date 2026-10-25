# Observability & Alert Catalog: [Service Name]

* **Service**: [e.g., carrier-gateway]
* **Primary Dashboard**: [Grafana / Datadog Link]
* **Logging Index**: `application-logs-returns-*`

---

## 1. Alert Rules & Thresholds

| Alert Name | Severity | Condition / PromQL Query | Window | Action |
| :--- | :--- | :--- | :--- | :--- |
| `High5xxRate` | P1 - Critical | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) > 0.05` | 3m | PagerDuty On-Call |
| `KafkaConsumerLag` | P2 - Major | `sum(kafka_consumergroup_lag{topic=~"returns.*"}) > 5000` | 10m | Slack `#alerts-returns` |
| `CircuitBreakerOpen` | P2 - Major | `resilience4j_circuitbreaker_state{state="open"} == 1` | 2m | Slack `#alerts-returns` |

---

## 2. Standard Structured Log Queries

### Find Internal Errors and Exceptions (CloudWatch / LogQL)
```sql
fields @timestamp, @message, traceId, exception
| filter @message like /ERROR/ or ispresent(exception)
| sort @timestamp desc
| limit 100
```

### Trace a Single Request Lifecycle Across Microservices
```sql
fields @timestamp, serviceName, @message
| filter traceId = "c8f921ab9d40a28f"
| sort @timestamp asc
```