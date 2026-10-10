# 8. Polyglot Persistence — Utilizing Amazon DynamoDB for Inventory Inspection Audit Logs

* **Status:** Accepted
* **Date:** 2026-10-09
* **Deciders:** James Hayes
* **Technical Area:** `inventory-service` / Storage Strategy

---

## Context and Problem Statement

During return intake at warehouse fulfillment centers, incoming items undergo physical quality checks before restock or disposal. The inspection data collected during this process varies significantly based on product categories (e.g., electronics require power checks and cosmetic ratings; apparel requires stain checks and tag presence).

Initially, all `inventory-service` data resided in relational SQL Server storage (`inventory_db`). Storing unstructured inspection checklists and bin location telemetry in a relational database created schema rigidity, requiring frequent DDL schema migrations or sparse `JSON`/EAV columns. Furthermore, audit telemetry is append-heavy and rapidly growing, creating database bloat and index maintenance overhead on transactional SQL Server tables.

We need a dedicated, schema-agnostic storage solution for return inspection audit records that offloads high-throughput writes from SQL Server while supporting dynamic checklist structures.

---

## Decision Drivers

* **Schema Heterogeneity:** Inspection checklists vary by product SKU and category; schemas must evolve without requiring SQL migration scripts or ALTER TABLE locks.
* **Write-Heavy Ingestion:** High-velocity, append-only event stream during peak warehouse processing windows.
* **Relational Offloading:** Isolate immutable audit trails from core ACID stock updates in `inventory_db` to protect relational query performance.
* **Cloud & Local Parity:** Seamless integration with AWS native infrastructure and Docker Compose developer environments (`dynamodb-local`).

---

## Considered Options

1. **Amazon DynamoDB (Selected)**
2. **PostgreSQL / SQL Server JSON Columns (`JSONB` / `NVARCHAR(MAX)`)**
3. **MongoDB**

---

## Decision Outcome

**Chosen Option:** **Amazon DynamoDB** (via AWS SDK v2 Enhanced Client).

`inventory-service` adopts a polyglot persistence pattern:
* **SQL Server (`inventory_db`):** Maintains current relational stock levels, bin references, and relational ledger states.
* **DynamoDB (`inventory_inspection_audit`):** Stores append-only inspection logs, inspector metadata, and dynamic key-value checklist maps.

### Key Design Parameters

* **Table Name:** `inventory_inspection_audit`
* **Partition Key (`HASH`):** `returnId` (String)
* **Sort Key (`RANGE`):** `itemSku` (String)
* **Attributes:** `binLocation`, `inspectorId`, `overallCondition`, `checklist` (`Map<String, String>`), `inspectedAt`
* **Local Development Environment:** `amazon/dynamodb-local:latest` running on port 8000 paired with `dynamodb-admin` on port 8001.

---

## Pros and Cons of Options

### Option 1: Amazon DynamoDB

* **Pros:**
    * **Dynamic Attribute Support:** Supports arbitrary key-value pairs (`Map<String, String>`) without predefined schema bounds.
    * **Consistent Operational Latency:** Predictable single-digit millisecond read/write performance at any write volume.
    * **Zero Schema Maintenance:** Eliminates Flyway migration overhead for audit log structure changes.
    * **Serverless Scalability:** Native auto-scaling in AWS environments with pay-per-request capacity.
* **Cons:**
    * Requires explicit type mappings in AWS SDK Enhanced Client (e.g., avoiding raw generic `Object` maps).
    * Ad-hoc analytical querying requires Global Secondary Indexes (GSIs) or exporting to AWS Athena/S3.

### Option 2: SQL Server JSON Columns

* **Pros:** Single database technology stack; transactions span stock updates and audit records natively.
* **Cons:** Write lock contention on relational indexes; increased database storage footprint and snapshot costs over time.

### Option 3: MongoDB

* **Pros:** Strong document indexing and aggregation pipeline.
* **Cons:** Introduces additional cluster management overhead in AWS (DocumentDB or self-hosted) compared to serverless DynamoDB.

---

## System Impact & Consequences

* **Data Isolation:** Audit records are strictly decoupled from relational inventory state, preventing audit log growth from degrading stock availability lookups.
* **Development Workflow:** Developers run `dynamodb-local` via Docker Compose and inspect local audit payloads using `dynamodb-admin` or AWS CLI.
* **Eventual Consistency:** `inventory-service` updates relational stock balances and writes DynamoDB audit logs within the event processing flow, enforcing domain-level eventual consistency.