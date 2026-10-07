# Event Schema & Data Contract Governance

| Field | Value |
| :--- | :--- |
| **Serialization** | JSON Schema / Avro |
| **Broker Target** | Apache Kafka |
| **Version Strategy**| Semantic Versioning (MAJOR.MINOR.PATCH) |

---

## 1. Schema Compatibility Rules
* **BACKWARD Compatibility Required:** Consumers running version `N` must be able to process messages produced by version `N+1`.
* **Prohibited Changes Without Major Version Bump:**
    1. Renaming or deleting existing fields.
    2. Changing field data types (e.g., `string` to `integer`).
    3. Adding required fields without default values.

---

## 2. Event Specification Example: `ReturnRequestedEvent`

* **Kafka Topic:** `returns.raw-ingestion.v1`
* **Partition Key:** `return_id` (Ensures strict message ordering per return request)

```json
{
  "$schema": "[http://json-schema.org/draft-07/schema#](http://json-schema.org/draft-07/schema#)",
  "title": "ReturnRequestedEvent",
  "type": "object",
  "properties": {
    "eventId": { "type": "string", "format": "uuid" },
    "returnId": { "type": "string" },
    "trackingNumber": { "type": "string" },
    "carrierCode": { "type": "string", "enum": ["FEDEX", "UPS", "ABF"] },
    "timestamp": { "type": "string", "format": "date-time" }
  },
  "required": ["eventId", "returnId", "trackingNumber", "carrierCode", "timestamp"]
}