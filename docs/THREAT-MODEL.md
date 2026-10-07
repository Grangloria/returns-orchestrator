# System Security & STRIDE Threat Model

| Field | Value |
| :--- | :--- |
| **System** | Returns Orchestrator Core Platform |
| **Assessment Date**| 2026-10-06 |
| **Standard** | STRIDE Framework / OWASP Top 10 |

---

## 1. Trust Boundaries & Sensitive Data
* **Trust Boundary 1:** External Freight Carrier Webhooks ──> API Gateway (Public Internet to Internal Mesh).
* **Trust Boundary 2:** Service-to-Service gRPC/HTTP communication inside Kubernetes cluster.
* **Sensitive Assets:** Carrier API Keys, Customer Order Details, Refund Transaction Tokens.

---

## 2. STRIDE Threat Assessment & Mitigations

| Threat Category | Threat Description | Mitigation Strategy |
| :--- | :--- | :--- |
| **Spoofing** | Unauthorized actor forging carrier status updates. | HMAC SHA-256 signature verification on inbound webhooks + OAuth2 JWT validation. |
| **Tampering** | In-transit alteration of refund authorization payloads. | Mandatory TLS 1.3 encryption across all internal microservice calls. |
| **Repudiation** | Carrier claims return tracking update was never received. | Immutable audit log in PostgreSQL + Kafka append-only log retained for 90 days. |
| **Information Disclosure** | Leakage of API secrets or database credentials. | HashiCorp Vault integration for dynamic credential leasing and secret injection. |
| **Denial of Service** | Webhook flooding crashing ingestion services. | Resilience4j rate-limiting + WebFlux reactive non-blocking backpressure handles traffic spikes. |
| **Elevation of Privilege**| Compromised service accessing financial refund APIs. | Role-Based Access Control (RBAC) enforcing least-privilege mTLS communication. |