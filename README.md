# Returns Orchestrator: Event-Driven Reactive Ecosystem

## 📋 Overview
A high-scale, non-blocking microservice ecosystem designed to orchestrate complex product return workflows using an **Event-Driven Architecture (EDA)**. Built with **Spring WebFlux** and **Apache Kafka**, this project demonstrates a production-grade approach to distributed systems, ensuring high concurrency, resiliency, and complete asynchronous decoupling between core orchestration gates and downstream consumer workers.

---

## 🏗️ Multi-Module Architecture
This project is architected as a **Multi-Module Gradle build**, enforcing strict domain boundaries and enabling absolute code reuse through a shared contract layer:

* **`common-models`**: The "Single Source of Truth." A shared domain library containing immutable Java 21 Records and DTOs, ensuring strict schema enforcement and preventing payload drift across processing boundaries.
* **`returns-orchestrator`**: The Core Ingress Node & Event Producer. Processes active web traffic, executes schema validation, performs synchronous real-time 3PL carrier tracking requests, registers persistent audit trails to database structures, and fires transaction boundary tokens downstream over Kafka.
* **`carrier-service`**: An asynchronous logistics consumer worker. Intercepts broadcast events to handle downstream long-running tasks, such as uploading shipping manifest duplicates to permanent enterprise AWS S3 cloud archiving buckets.
* **`notification-service`**: An asynchronous communications worker. Automatically monitors topic boundaries to handle non-blocking customer transaction notifications via transactional SMTP engines (SendGrid).

---

## ⚡ The Solution: Reactive Fan-Out
Traditional imperative microservices often struggle with thread-starvation, blocked event loops, and high tail-latencies when orchestrating multiple third-party systems. This ecosystem eliminates those architectural bottlenecks through:

* **Non-Blocking Execution Loops**: Utilizing Project Reactor (`Mono`/`Flux`) and Netty to optimize thread scheduling, allowing thousands of concurrent requests to scale efficiently with minimal hardware footprint.
* **Asynchronous Fire-and-Forget (Fan-Out)**: By dropping a `ReturnInitiatedEvent` into a shared broker cluster, the core orchestrator answers the client gateway immediately. It completely separates the lifecycle of web traffic from heavy background routines like cloud uploads and email generation.

---

## 🎨 Key Architectural Design Patterns

### Self-Healing Multi-Stage Infrastructure
To bypass the limitation where Microsoft SQL Server containers do not automatically initialize a database instance on boot, this project introduces a self-healing cluster script inside Docker Compose. The primary database runs a native health check tool. A sidecar worker container (`sql-server-init`) detects when the database engine is healthy, verifies whether `returns_db` is missing, provisions the catalog space automatically, and cleanly shuts itself down to preserve system host memory.

### Distributed Tracing & Multi-Pillar Observability Matrix
Implements a unified telemetry strategy combining distributed trace propagation with centralized telemetry backends.
* Outbound tracing parameters (`x-b3-traceid`) are automatically stamped onto the binary header arrays of Kafka network packets using **Micrometer Tracing** and the **Brave/B3 Propagation Schema**.
* Consumers ingest the `ConsumerRecord` metadata envelope directly, extracting and restoring the trace identifier inside Logback's thread Mapped Diagnostic Context (MDC) scope.
* **Centralized Log Tiling:** Customized `logback-spring.xml` configurations force each independent service to simultaneously write console prints and append records to a centralized root `logs/` directory. This allows multi-service transaction chains to be parsed sequentially by tracking a single alphanumeric string across **Grafana Loki**.

### Intelligent Triage (Strategy Pattern)
Uses a specialized `TriageFactory` component to evaluate incoming item properties dynamically at runtime. It routes items to optimal sorting channels (e.g., LTL Freight handling lines versus Standard Parcel sorting lines) without polluting the core controller implementation with conditional logic blocks.

### Resilient Reactive Data Access
Employs **Spring Data R2DBC** to handle non-blocking driver calls to Microsoft SQL Server. This eliminates legacy JDBC thread-locking friction, preserving a fully reactive chain from the netty socket layer all the way down to physical disk operations.

---

## 📊 Observability & System Telemetry

This project implements an enterprise-tier telemetry suite split across **Two Key Pillars of Telemetry**: Multi-Dimensional Time-Series Metrics and Log Aggregation. This setup bridges individual application runtimes with containerized indexing nodes to provide real-time performance tracking and cross-boundary correlation inside a single workspace.

### 🔌 Local Telemetry Gateways
* **Grafana Master Control Room Workspace:** `http://localhost:3000` *(Credentials: `admin` / `admin`)*
* **Pillar 1: Metrics Data Source Engine (Prometheus):** `http://localhost:9090`
* **Pillar 2: Centralized Log Aggregation Vault (Loki):** `http://localhost:3100`

### 📈 Pillar 1: Time-Series Metrics (Prometheus)
System metrics are scraped on an active **Pull-Based Schedule**. Each running microservice utilizes Spring Boot Actuator and Micrometer to compile real-time performance statistics, exposing them on a localized HTTP endpoint:
* **Orchestrator Scraping Stream:** `http://localhost:8080/actuator/prometheus`
* **Carrier Logistics Scraping Stream:** `http://localhost:8081/actuator/prometheus`
* **Notification System Scraping Stream:** `http://localhost:8082/actuator/prometheus`

The containerized Prometheus server samples these targets every 5 seconds, archiving metrics (JVM Heap sizes, active thread pools, R2DBC connection capacity, and Garbage Collection pauses) into its database. Grafana queries this layer using **PromQL** to paint live operational graphs under **Dashboard ID: `4701`**.

### 🪵 Pillar 2: Centralized Log Aggregation (Grafana Loki + Promtail)
To resolve terminal isolation across the cluster, logs are shipped via an automated **Push-Based Log Aggregation Pipeline**:
1. **The Log Engine:** Services log transaction steps while outputting raw text to the centralized `./logs` folder at the project root using a rolling file appender strategy.
2. **The Shipping Agent (Promtail):** Runs as a container service, tailing the shared root folder and streaming incoming data lines into Loki.
3. **The Metadata Indexer (Loki):** Promtail uses a regex processing stage to parse file paths dynamically, automatic-stamping lines with clear application tags (e.g., `application="returns-orchestrator"`).

#### 🔍 Practical LogQL Filter Queries
Logs can be filtered, tracked, and isolated inside Grafana's **Explore Workspace** using **LogQL**:
* **Isolate a Specific Service Log:** `{application="returns-orchestrator"}`
* **Scan Entire Cluster for Runtime Exceptions:** `{job="app-logs"} |= "ERROR"`
* **Trace an Asynchronous Transaction Chain Across Network Barriers:** `{job="app-logs"} |= "ORD-2026-99"`

<details>
<summary>🗺️ Click to Expand: Telemetry Troubleshooting & Runbook</summary>

### Core Variable & Data Source Mapping
If dashboard panels display unlinked data errors or metric variables fail to resolve:
1. **Master Source Key:** Confirm that your Grafana workspace configuration has a data source connection variable explicitly named `DS_PROMETHEUS` mapping to your Prometheus instance.
2. **Metadata Target Validation:** Run a raw validation probe like `jvm_memory_used_bytes` directly in the Prometheus console query box (`localhost:9090`) to verify your system label variables are actively reporting.
3. **Log Stream Verification:** If Loki displays empty targets inside Grafana, run `docker logs returns-promtail` to ensure the container daemon has cleanly established file-read file paths to the mounted shared directory.

</details>

---

## 💻 Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Java 21 (Records, Patterns, Virtual Compatibility) |
| **Framework** | Spring Boot 3.2+ |
| **Messaging** | Apache Kafka (Distributed Broker / Fan-out Topology) |
| **Reactive Engine** | Project Reactor (`flatMap`, `deferContextual`, `doOnSuccess`) |
| **Persistence** | R2DBC Asynchronous Driver / Microsoft SQL Server 2022 |
| **Observability** | Micrometer Tracing / Prometheus / Grafana Loki / Promtail |
| **Build Tool** | Multi-Module Gradle Platform |
| **API Container** | Spring WebFlux / Reactor Netty Event Loop |

---

## 🛡️ Data Integrity & Ingress Guardrails
The system stops bad data from entering the reactive chain at the gateway container edge using **Jakarta Bean Validation**:

1.  **Payload Boundary Constraints**: Strict validation on incoming fields (e.g., valid email formats, matching SKU character codes, and non-empty IDs) to keep garbage collection steps lean.
2.  **Contract Enforcement**: Because the schema definitions live natively inside the `common-models` JAR library, it is compile-time impossible for a producer to send a payload that a downstream consumer cannot parse.

---

## 🚀 Getting Started

### Prerequisites
* Java 21 SDK (Temurin / Oracle)
* Docker Desktop & Docker Compose Engine

### Running the Ecosystem
1. **Launch Self-Healing Cluster**:
   ```bash
   docker compose up -d