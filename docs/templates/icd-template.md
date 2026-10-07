# Interface Control Document (ICD)

| Specification Metadata | Detail |
| :--- | :--- |
| **Interface ID** | `ICD-RO-INV-001` |
| **Source System** | `return-orchestrator-service` |
| **Target System** | `inventory-service` |
| **Communication Pattern**| Asynchronous Event-Driven |

---

## 1. Boundary & Purpose
Defines the binding operational contract for notifying inventory systems upon item inspection during a return workflow.

## 2. Transport & Delivery Guarantees
* **Protocol:** Apache Kafka
* **Topic Name:** `returns.item-inspected.v1`
* **Partitioning Key:** `sku_id` (Ensures strict message ordering per item type)
* **Delivery Semantics:** At-least-once delivery. Target consumers must implement idempotency checks.
* **Target Throughput:** Sustained 500 TPS / Peak 2,500 TPS.

## 3. Payload Contract
```json
{
  "headers": {
    "eventId": "c9bf9e57-1685-4c89-bafb-ff5af830be8a",
    "correlationId": "trace-abc-123",
    "timestamp": "2026-10-07T16:00:00Z"
  },
  "payload": {
    "returnId": "RET-99281",
    "skuId": "SKU-8831-XL",
    "warehouseId": "WH-EAST-01",
    "condition": "RESTOCK",
    "quantity": 1
  }
}
```

## 4. Backpressure & SLA Requirements
* **Maximum Message Size:** 64 KB
* **Target Processing SLA:** p99 processing time <= 100ms
* **Consumer Scaling Threshold:** Automatically scale consumer pods if consumer lag exceeds 5,000 unread events.

## 5. Failure Recovery Policy
* **Retry Strategy:** 3 retries with exponential backoff (1s, 2s, 4s).
* **Dead Letter Destination:** Unprocessable events are routed to `returns.item-inspected.DLT` with error headers attached.