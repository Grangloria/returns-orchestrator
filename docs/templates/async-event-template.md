# Async Event Specification: [Topic Name / Event Name]

* **Topic**: `returns.<environment>.<domain>.<event-name>.v1`
* **Schema Format**: CloudEvents Spec v1.0.2 / JSON Schema
* **Producing Service**: [e.g., returns-service]
* **Consuming Services**: [e.g., refund-service, notification-service]

---

## 1. Kafka Topic Topology & Keying Strategy

* **Partition Count**: 6 (Development) / 24 (Production)
* **Replication Factor**: 3 (Production)
* **Retention Policy**: 7 days (or Compaction if Key-value state store)
* **Partition Key**: `returnId` (Ensures strict chronological ordering per return lifecycle)

---

## 2. CloudEvent Envelope & Payload Schema

```json
{
  "$schema": "[https://json-schema.org/draft/2020-12/schema](https://json-schema.org/draft/2020-12/schema)",
  "type": "object",
  "properties": {
    "specversion": { "type": "string", "enum": ["1.0"] },
    "id": { "type": "string", "format": "uuid" },
    "source": { "type": "string", "enum": ["/services/returns-service"] },
    "type": { "type": "string", "enum": ["com.returns.item.received.v1"] },
    "time": { "type": "string", "format": "date-time" },
    "datacontenttype": { "type": "string", "enum": ["application/json"] },
    "data": {
      "type": "object",
      "required": ["returnId", "sku", "receivedAt", "condition"],
      "properties": {
        "returnId": { "type": "string", "format": "uuid" },
        "sku": { "type": "string" },
        "quantity": { "type": "integer", "minimum": 1 },
        "condition": { "type": "string", "enum": ["RESTOCKABLE", "DAMAGED", "LOST"] },
        "warehouseId": { "type": "string" },
        "receivedAt": { "type": "string", "format": "date-time" }
      }
    }
  },
  "required": ["specversion", "id", "source", "type", "time", "data"]
}
```

---

## 3. Failure Routing & Dead Letter Queue (DLQ)

* **Retry Strategy**: Fixed delay of 1000ms, max 3 attempts.
* **Dead Letter Topic**: `returns.<environment>.<domain>.<event-name>.dlq`
* **DLQ Alert Threshold**: > 5 messages in 5 minutes triggers Sev-2 alert.