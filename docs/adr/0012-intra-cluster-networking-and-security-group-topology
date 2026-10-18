# ADR 0012: Intra-Cluster Networking, Task Port Parity, and Security Group Topology for ECS Microservices

- **Status:** Accepted
- **Date:** 2026-10-17
- **Deciders:** James Hayes (Staff Engineer)
- **Technical Domain:** Cloud Infrastructure / Networking / ECS Fargate

---

## 1. Context & Problem Statement

The returns orchestrator platform runs a distributed, event-driven microservices architecture on AWS ECS (Fargate) backed by AWS Cloud Map service discovery, Apache Kafka, Redis, and SQL Server.

During initial cluster deployment, tasks encountered persistent 30-second `ConnectTimeoutException` and `Connection refused` failures across multiple critical paths:
1. `returns-service` failed to establish R2DBC connection pools to SQL Server (`10.0.11.64:1433`), blocking outbox event polling.
2. `api-gateway` timed out attempting to reverse-proxy presigned S3 evidence upload requests to `returns-service:8000`.
3. `carrier-gateway` defaulted to `localhost:8003` because of mismatched environment variable injection and an incorrect port definition (`3000` instead of `8003`) on `mock-external-carrier-api`.
4. In ECS `awsvpc` network mode, tasks attached to the same security group cannot communicate internally by default unless an explicit self-referential ingress rule is present. Furthermore, declaring both inline `ingress` blocks and standalone `aws_security_group_rule` resources caused Terraform state conflicts and dropped rules on successive applies.

---

## 2. Decision Drivers & Forces

- **Zero Dropped Packets:** Guarantee deterministic TCP handshakes across all intra-cluster communication channels (database, cache, broker, and REST service discovery).
- **Environment Parity:** Mirror local development port bindings (8000–8080) directly in ECS task definitions to eliminate fallback-to-localhost bugs.
- **Maintainable Security Group Architecture:** Establish a single source of truth for firewall rules in Terraform, avoiding state thrashing between standalone resources and inline blocks.
- **Least Privilege Network Posture:** Restrict inter-service traffic strictly to the task security group (`aws_security_group.ecs_tasks.id`) rather than opening broad VPC subnet CIDR ranges.

---

## 3. Options Considered

1. **Option 1 — Open Intra-VPC CIDR Ingress (`10.0.0.0/16` on all ports):** Allows all containers within the VPC to communicate unimpeded.
   - **Pros:** Fast implementation; avoids tracking individual microservice port changes.
   - **Cons:** Violates least privilege; exposes internal databases, brokers, and admin endpoints to any compute resource provisioned in the VPC.
2. **Option 2 — Individual Granular Standalone Rules per Port:** Add separate `aws_security_group_rule` resources for every individual port (8000, 8001, 8002, 8003, 8004, 8006).
   - **Pros:** Maximum port isolation per microservice.
   - **Cons:** Rapid security group rule limit saturation; high Terraform maintenance overhead; high risk of conflicts if mixed with inline security group definitions.
3. **Option 3 — Self-Referential Ingress Topology with Dedicated Infrastructure Ports and Consolidated Microservice Application Range (8000–8080):** Consolidate all ingress rules inside `security_groups.tf` using self-referencing source security groups (`source_security_group_id = aws_security_group.ecs_tasks.id`). Allocate dedicated rules for infrastructure components (1433 for SQL Server, 6379 for Redis, 9092 for Kafka) and a consolidated range rule for application microservices (`8000–8080`), combined with aligned task definition container/host port mappings in `ecs.tf`.
   - **Pros:** Eliminates Terraform state conflicts, enforces strict security group self-isolation, supports dynamic container re-deployments without rule churn, and establishes strict parity with local development profiles.
   - **Cons:** Allows intra-cluster network visibility across the 8000–8080 range for any task assigned to the ECS task security group.

---

## 4. Decision Outcome

**Chosen Option:** Option 3 — Self-Referential Ingress Topology with Dedicated Infrastructure Ports and Consolidated Microservice Application Range (8000–8080)

### Justification
Option 3 provides clean infrastructure governance in Terraform while eliminating the recurring 30-second silent packet drops caused by missing security group rules. Defining dedicated rules for infrastructure ports (SQL Server: 1433, Redis: 6379, Kafka: 9092) and consolidating the microservice range (8000–8080) satisfies AWS security group limits, accommodates new Spring Boot downstream services without requiring infrastructure rule changes, and ensures all ingress declarations live exclusively in `security_groups.tf`.

---

## 5. System Consequences & Trade-offs

### Positive
- **Elimination of Connection Hangs:** Resolves Netty 30-second connect timeout loops across the R2DBC database layer, internal API gateway routes, and Kafka consumer group rebalances.
- **Port Mapping Determinism:** Synchronizes ECS Fargate task definitions with native Spring Boot port contracts:
  - `returns-service`: `8000`
  - `inventory-service`: `8001`
  - `notification-service`: `8002`
  - `mock-external-carrier-api`: `8003`
  - `carrier-gateway`: `8004`
  - `refund-service`: `8006`
  - `api-gateway`: `8080`
- **Terraform Consistency:** Centralizes all firewall configuration inside `security_groups.tf`, eliminating conflicts caused by mixing inline blocks and standalone `aws_security_group_rule` resources.

### Negative & Mitigation
- **Drawback:** Any container with the `returns-ecs-tasks-sg-prod` security group can technically hit any microservice port within the `8000–8080` block.
  - **Mitigation:** Services reside entirely in private VPC subnets with zero public IP assignment (`assign_public_ip = false`); boundary ingress is strictly gated by the public Application Load Balancer targeting ports `80` and `8080`.

---

## 6. Observability & Verification

- **Database Verification:** Transactional outbox records in `outbox` transition to `processed = 1`, and `return_manifests.label_url` updates from `<null>` to valid tracking URLs (`LABEL_READY`).
- **CloudWatch Logs:**
  - `/ecs/returns-service-prod`: Outbox poller logs clean execution without `DataAccessResourceFailureException`.
  - `/ecs/carrier-gateway-prod`: Reports successful Kafka record consumption from `returns.order.initiated.v1` and HTTP 200 responses from `mock-external-carrier-api:8003`.
  - `/ecs/returns-api-gateway-prod`: Evidence upload route `/api/v1/returns/evidence/presigned-url` returns HTTP 200 in `<100ms` without Netty channel aborts.