# Documentation Directory Taxonomy Guide

This guide defines the purpose and expected contents for every directory across the global repository docs and individual microservice modules[cite: 3, 7].

---

## 1. Global Documentation (`/docs/`)

Global documentation captures cross-cutting decisions, overall system design, operational playbooks, and platform-wide templates[cite: 7].

### `docs/adr/`
* **Purpose**: System-wide Architecture Decision Records (ADRs)[cite: 7].
* **What belongs here**: Foundational architectural decisions that impact multiple services or cross bounded contexts (e.g., messaging stack, transactional outbox pattern, global persistence models)[cite: 7].

### `docs/api/`
* **Purpose**: Centralized interface contracts, schemas, and API governance[cite: 7].
* **Subdirectories**:
    * `asyncapi/`: System-wide event schemas and AsyncAPI specifications for Kafka topics[cite: 7].
    * `openapi/`: RESTful API specifications, OpenAPI schemas, and shared HTTP contracts[cite: 7].

### `docs/architecture/`
* **Purpose**: High-level platform architecture models, governance docs, and system topography[cite: 7].
* **Subdirectories**:
    * `c4-models/`: System context, container, and component diagrams showing macro-level architecture[cite: 7].
    * `event-storming/`: Domain maps, event-storming artifacts, and bounded-context definitions[cite: 7].
    * `sequence-diagrams/`: Cross-service message flows and end-to-end transaction sequences[cite: 7].

### `docs/development/`
* **Purpose**: Platform-wide engineering standards and contributor guides[cite: 7].
* **What belongs here**: Monorepo conventions, Git branching models, commit message rules, PR checklists, and developer onboarding instructions[cite: 7].

### `docs/functional/`
* **Purpose**: Overarching functional specifications and business domain requirements[cite: 7].
* **Subdirectories**:
    * `epics/`: High-level business initiatives and multi-service functional requirements[cite: 7].
    * `use-cases/`: Multi-actor interaction flows and functional requirements[cite: 7].

### `docs/non-functional/`
* **Purpose**: Cross-cutting system quality attributes (NFRs) and governance rules[cite: 7].
* **What belongs here**: Latency and throughput targets, PII masking rules, security audits, compliance baselines, and disaster recovery (RTO/RPO) objectives[cite: 7].

### `docs/operations/`
* **Purpose**: Production support procedures, operational guidelines, and telemetry definitions[cite: 7].
* **What belongs here**: Severity protocols, links to shared dashboards, and deployment/release strategies[cite: 7].

### `docs/rfcs/`
* **Purpose**: Requests for Comments[cite: 7].
* **What belongs here**: Formal technical proposals, system design revisions, and major refactoring proposals submitted for peer review before implementation[cite: 7].

### `docs/runbooks/`
* **Purpose**: Operational remediation procedures[cite: 7].
* **What belongs here**: Step-by-step resolution guides for alerts, incident playbooks, dead-letter queue (DLQ) re-drive procedures, and maintenance instructions[cite: 7].

### `docs/services/`
* **Purpose**: Monorepo service inventory and cross-service relationship maps[cite: 7].
* **What belongs here**: Service catalog registries, metadata manifests, and dependency matrices mapping which services interact with each other[cite: 7].

### `docs/templates/`
* **Purpose**: Master authoring templates[cite: 7].
* **What belongs here**: Reusable Markdown and YAML templates for creating new ADRs, runbooks, C4 models, system designs, interface specs, and service catalog entries[cite: 7].

---

## 2. Microservice Documentation (`<service>/docs/`)

Each microservice maintains an isolated `docs/` directory at its root alongside its source code and build files, capturing decisions and operational details restricted to that specific module[cite: 3, 4].

### `<service>/docs/adrs/`
* **Purpose**: Local Architecture Decision Records[cite: 3].
* **What belongs here**: Implementation choices strictly isolated to this microservice (e.g., choice of local client libraries, templating engines, or internal cache providers)[cite: 3].

### `<service>/docs/interfaces/`
* **Purpose**: Service boundaries, contract specifications, and interface definitions[cite: 3].
* **Subdirectories**:
    * `sync-apis/`: OpenAPI/Swagger specifications for endpoints exposed or consumed synchronously by this service[cite: 3].
    * `async-events/`: AsyncAPI contracts and message schemas for Kafka topics published or subscribed to by this service[cite: 3].
    * `postman-collections/`: Exported request collections and environment variables for local testing of this service[cite: 3].

### `<service>/docs/functional/`
* **Purpose**: Service-specific business rules and domain logic[cite: 3].
* **What belongs here**: Bounded-context domain rules, local state machine definitions, input validation rules, and error-handling policies[cite: 3].

### `<service>/docs/non-functional/`
* **Purpose**: Service constraints, protection limits, and performance goals[cite: 3].
* **What belongs here**: Resilience settings (circuit breakers, retry thresholds, rate limits) and infrastructure resource targets (CPU/memory scaling thresholds for ECS Fargate)[cite: 3].

### `<service>/docs/operations/`
* **Purpose**: Service-level SRE, alerting, and observability[cite: 3].
* **Subdirectories**:
    * `runbooks/`: Step-by-step troubleshooting and remediation procedures for specific alerts originating from this service[cite: 3].
* **What belongs here**: Definitions for local CloudWatch/Datadog alerts, logging query syntax, and links to service-specific dashboards[cite: 3].

### `<service>/docs/development/`
* **Purpose**: Local developer experience and testing workflows[cite: 3].
* **What belongs here**: Local Docker Compose instructions, required environment variables, local database/Redis schemas, seed data, and testing guides (unit, integration with Testcontainers, and contract tests)[cite: 3].