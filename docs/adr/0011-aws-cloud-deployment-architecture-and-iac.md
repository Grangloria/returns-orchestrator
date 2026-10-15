# ADR 0011: AWS Cloud Deployment Architecture and Infrastructure as Code (IaC)

- **Status:** Accepted
- **Date:** 2026-10-15
- **Deciders:** James Hayes (Staff Engineer)
- **Technical Domain:** Cloud Infrastructure / Cloud Deployment & CI/CD

---

## 1. Context & Problem Statement

As part of Phase 6 (AWS Cloud Deployment & CI/CD Pipeline), the Returns Orchestrator platform requires a production-grade AWS cloud deployment topology in `us-east-2`. The system encompasses customer-facing web tiers (`customer-portal`), an API routing layer (`api-gateway`), core business microservices (`returns-service`), transactional relational persistence (SQL Server 2022 Express for `return_manifests` and `outbox`), immutable event stores (DynamoDB), and document storage (S3).

Previously, components relied on local execution and disparate manual provisioning commands. This lacked environment parity, introduced security vulnerabilities through static networking, lacked dynamic intra-VPC DNS discovery, and prevented automated CI/CD pipeline deployments. A standardized cloud infrastructure architecture must be codified via Terraform to establish automated, reproducible, and secure cloud environments.

---

## 2. Decision Drivers & Forces
- **Automated Cloud Delivery:** Codify all AWS infrastructure components using Terraform to integrate seamlessly with automated GitHub Actions CI/CD workflows (`.github/workflows/deploy.yml`).
- **Zero-Trust Network Isolation:** Strictly confine backend microservices, SQL Server persistence, and service-to-service traffic to private subnets across multiple Availability Zones, exposing only the Application Load Balancer (ALB) to public subnets.
- **Private Inter-Service Communication:** Provide dynamic, zero-maintenance service discovery inside the VPC without deploying expensive internal load balancers or managing hardcoded private IPs.
- **Automated Container Lifecycle & Persistence:** Ensure containerized workloads (specifically SQL Server) bootstrap required database schemas (`returns_db`, `return_manifests`, `outbox`) idempotently upon task spin-up.

---

## 3. Options Considered
1. **Option 1 - EC2-Hosted Docker Compose with Manual CLI Networking:** Deploy applications onto provisioned EC2 virtual machines using Docker Compose and elastic network interfaces. **Pros:** Low initial configuration complexity. **Cons:** Requires host-level OS patching, manual scaling, lacks dynamic DNS failover, and fails modern cloud infrastructure compliance.
2. **Option 2 - ECS EC2 Launch Type with Internal Application Load Balancers:** Deploy containerized services on ECS clusters managed with EC2 worker nodes, placing internal ALBs in front of every internal microservice. **Pros:** Standardized HTTP health checking across all endpoints. **Cons:** Significant recurring cost for idle internal ALBs, inability to route TDS (TCP 1433) through ALBs without Network Load Balancers (NLBs), and lingering EC2 capacity management overhead.
3. **Option 3 - Serverless AWS ECS Fargate, Public ALB, AWS Cloud Map Private DNS, and Terraform IaC:** Provision a multi-tier AWS VPC (public and private subnets), an Application Load Balancer for perimeter traffic, an ECS Fargate cluster for serverless compute, AWS Cloud Map (Route 53 Private DNS) for private service discovery, and DynamoDB/S3 storage via a unified Terraform codebase. **Pros:** Zero compute host maintenance, strict network isolation in private subnets, dynamic `A`-record discovery via Cloud Map, native integration with ECR and GitHub Actions CI/CD, and fully declarative infrastructure. **Cons:** Tasks in `awsvpc` network mode require fine-grained security group rules and entry-point dependency coordination.

---

## 4. Decision Outcome
**Chosen Option:** Option 3 - Serverless AWS ECS Fargate, Public ALB, AWS Cloud Map Private DNS, and Terraform IaC

### Justification
Option 3 delivers an enterprise-grade cloud deployment architecture that satisfies all Phase 6 deliverables. AWS ECS Fargate abstracts container serverless host management, and the multi-AZ private subnet topology ensures backend microservices and databases are unreachable from the public internet. Utilizing AWS Cloud Map (`returns-orchestrator-cluster-prod.local`) allows `returns-service` and `sqlserver` to register their task ENI IPs directly into Route 53 private `A` records upon boot, enabling reliable internal communication (`sqlserver.returns-orchestrator-cluster-prod.local:1433`) without additional load balancing infrastructure.

---

## 5. System Consequences & Trade-offs

### Positive
- **Declarative Infrastructure as Code (IaC):** The complete cloud footprint—VPC, subnets, route tables, security groups, ALB, ECR, ECS Fargate services, IAM roles, CloudWatch logs, and Cloud Map namespaces—is codified in `infra/terraform/`.
- **Perimeter Security:** Ingress from the public internet terminates at the Application Load Balancer; ECS tasks run in private subnets with strict egress-only internet access through NAT Gateways.
- **Dynamic Service Discovery:** Cloud Map registers and deregistrations task ENIs automatically upon task lifecycle events, providing immediate DNS failover without manual record maintenance.
- **Bootstrap Idempotency:** The SQL Server Fargate task automatically executes initialization routines to verify and create the `returns_db` database, `return_manifests` table, and `outbox` table and indexes before accepting application queries.

### Negative & Mitigation
- **Drawback:** ECS Fargate `awsvpc` network mode does not support `containerPort` or `containerName` parameters when registering with Route 53 `A`-record discovery services.
    - **Mitigation:** Omitted port-mapping parameters from `service_registries` definitions in Terraform, relying on AWS ECS to publish task IP addresses directly to Cloud Map.
- **Drawback:** Relational database execution on ECS Fargate lacks managed RDS automated multi-region replication.
    - **Mitigation:** Acceptable for current project scope; persistence is decoupled across relational outbox state in SQL Server and immutable event streams stored in DynamoDB.

---

## 6. Observability & Verification
- **Deployment Verification:** Confirmed full infrastructure convergence via `terraform apply` with zero configuration drift (`0 destroyed`).
- **Telemetry & Monitoring:** Configured centralized Amazon CloudWatch log groups with 7-day retention (`/ecs/returns-service-prod`, `/ecs/returns-sqlserver-prod`, `/ecs/returns-customer-portal-prod`, `/ecs/returns-api-gateway-prod`).
- **Performance & Insights:** Enabled ECS Container Insights across the cluster (`containerInsights = "enabled"`) to capture CPU, memory, and network throughput metrics across tasks.
- **Service Mesh Validation:** Verified Route 53 private instance resolution using the AWS CLI (`aws servicediscovery discover-instances --namespace-name returns-orchestrator-cluster-prod.local --service-name returns-service`).