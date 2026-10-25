# Architecture Sequence Flow: [Flow Name]

* **Initiator**: [e.g., Customer Portal / Carrier Webhook]
* **Flow ID**: `SEQ-[Domain]-[Number]` (e.g., `SEQ-RET-001`)
* **Primary Path**: [Happy Path / Compensation Saga]

---

## 1. Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant API as api-gateway
    participant Ret as returns-service
    participant Kafka as Kafka Broker
    participant Inv as inventory-service
    participant Ref as refund-service

    Customer->>API: POST /returns (Submit return request)
    API->>Ret: Proxy request (with JWT context)
    Ret->>Ret: Validate order eligibility & items
    Ret->>Ret: Persist Return Entity (INITIATED)
    Ret-->>API: 201 Created (returnId, labelUrl)
    API-->>Customer: Return created confirmation

    Note over Ret,Kafka: Asynchronous Downstream Processing
    Ret->>Kafka: Publish `ReturnInitiatedEvent` (Key: returnId)
    Kafka->>Inv: Consume event
    Inv->>Inv: Reserve incoming restock slot
    Kafka->>Ref: Consume event
    Ref->>Ref: Initialize pending settlement ledger
```

---

## 2. Transaction Checkpoints & Outbox Events
* **Database State Checkpoint**: `returns` table updated to `INITIATED` inside atomic transaction with `outbox_events` table.
* **Failure Boundary**: If Kafka publishing fails, the transactional outbox polling worker retries every 500ms until successful acknowledgment.