# ADR 0013: Consolidate Carrier Mock API and Simulator into Unified Service

- **Status:** Accepted
- **Date:** 2026-10-25
- **Deciders:** James Hayes (Staff Engineer)
- **Technical Domain:** Test Infrastructure / Deployment Topology

---

## 1. Context & Problem Statement

Local integration testing, automated end-to-end verification, and staging environments require simulating third-party logistics (3PL) carrier interactions. These interactions span two distinct communication styles[cite: 11]:
1. **Synchronous HTTP REST calls:** Generating shipping labels and retrieving initial rate quotes (`CarrierController`, `CarrierService`)[cite: 11].
2. **Asynchronous event-driven updates:** Emitting simulated package transit events, barcode scans, and delivery confirmations via Kafka (`CarrierSimulatorListener`)[cite: 11].

Previously, these capabilities were divided into two standalone deployable services: `mock-external-carrier-api` (HTTP) and `mock-carrier-simulator` (Kafka listener)[cite: 11]. Operating two distinct applications incurred duplicated Spring Boot runtimes, redundant Gradle build modules, duplicate Docker container configurations, and excessive resource consumption on AWS ECS Fargate and local development machines[cite: 10, 11].

Under **Issue #45** (Stage 1: Core Infrastructure and Plumbing), we must prevent deploying redundant infrastructure while preserving both HTTP and event simulation capabilities[cite: 10].

---

## 2. Decision Drivers & Forces
- **Infrastructure Efficiency:** Eliminate duplicate ECS task definitions, load balancer target group routing rules, and local Docker Compose overhead by avoiding redundant mock container deployments[cite: 10].
- **Build & Dependency Hygiene:** Consolidate shared domain models (`LabelRequest`, `LabelResponse`) and exception handlers into a single Gradle build module and Dockerfile[cite: 11].
- **Architectural Boundary Isolation:** Maintain a strict separation between mock test infrastructure and the production `carrier-gateway`, preventing test mocks from polluting production artifacts[cite: 4, 11].
- **Developer Experience:** Provide a unified service entry point that developers can start with a single command for comprehensive local testing[cite: 11].

---

## 3. Options Considered
1. **Option 1 - Maintain Discrete Microservices (`mock-external-carrier-api` + `mock-carrier-simulator`):** Retains separate runtimes for HTTP endpoints and Kafka listeners[cite: 11].
    * **Pros:** Strict process isolation between HTTP thread pools and Kafka consumer listener containers.
    * **Cons:** Doubles ECS Fargate infrastructure costs; requires maintaining two `Dockerfile`s, two `build.gradle` configs, and duplicate logging/actuator setups; duplicates shared DTO definitions[cite: 10, 11].

2. **Option 2 - Embed Mocks Directly into `carrier-gateway` Behind a Spring Profile:** Embeds mock endpoints and simulator listeners directly inside `carrier-gateway`, activated via `--spring.profiles.active=mock-carrier`.
    * **Pros:** Zero additional container overhead.
    * **Cons:** Violates separation of concerns; leaks test logic and simulator dependencies into the production carrier gateway image; risks accidental activation of mock behavior in production environments.

3. **Option 3 - Unified `mock-carrier-service` Application:** Merges HTTP controllers, service layers, and Kafka simulator listeners into a single deployable module (`mock-carrier-service`) rooted at package `com.grangloria.carrier`[cite: 11].
    * **Pros:** Halves ECS task definitions and cloud costs; standardizes build, configuration, and containerization into a single Gradle module; co-locates carrier simulation logic in one coherent test double[cite: 10, 11].
    * **Cons:** Combines synchronous servlet/reactive threads and Kafka consumer pools into a single JVM process during integration testing.

---

## 4. Decision Outcome
**Chosen Option:** Option 3 - Unified `mock-carrier-service` Application[cite: 10, 11]

### Justification
Option 3 eliminates obsolete duplicate infrastructure without compromising production service boundaries[cite: 10, 11]. Consolidating into `mock-carrier-service` provides a single authoritative test double capable of handling synchronous REST label generation requests while listening for Kafka triggers and simulating transit scans[cite: 11]. Removing `mock-carrier-simulator` and `mock-external-carrier-api` simplifies monorepo orchestration across `settings.gradle`, Docker Compose, and AWS ECS task definitions[cite: 10, 11].

---

## 5. System Consequences & Trade-offs

### Positive
- **Reduced Cloud Footprint:** Lowers ECS Fargate provisioning from two services to one, reducing AWS compute expenditure and eliminating an redundant ALB target group[cite: 10].
- **Unified Build & Packaging:** Single `build.gradle` and multi-stage `Dockerfile` streamline CI/CD pipeline builds and local compile times[cite: 11].
- **Centralized Data Contracts:** Shared carrier payload schemas (`LabelRequest`, `LabelResponse`) are defined in one place under `com.grangloria.carrier.dto`, preventing contract drift between the mock API and simulator[cite: 11].
- **Simplified Monorepo Structure:** Eliminates redundant directories in `settings.gradle` and repository root[cite: 11].

### Negative & Mitigation
- **Drawback:** A defect or uncaught exception inside the asynchronous simulator listener could affect the shared JVM runtime hosting the mock HTTP endpoints[cite: 11].
    - **Mitigation:** Implemented `GlobalExceptionHandler` with explicit boundary validation and structured Spring Kafka error isolation so listener failures do not crash the embedded web server[cite: 11].
- **Drawback:** Memory consumption for a single container is marginally higher than each standalone microservice individually.
    - **Mitigation:** Tuned JVM memory parameters in `application.yaml` and set balanced ECS Fargate task limits (0.5 vCPU / 1024 MB)[cite: 11].

---

## 6. Observability & Verification
- **Verification:** Verified successful REST label creation via `CarrierController` and concurrent consumption of simulation events via `CarrierSimulatorListener` in unified test runs[cite: 11].
- **Metrics:** Unified Spring Boot Actuator telemetry (`/actuator/health`, `/actuator/prometheus`) exposing both HTTP request throughput and Kafka consumer lag metrics under a single port[cite: 11].
- **Logs:** Consolidated structured JSON logging via `logback-spring.xml` capturing `traceId`, `spanId`, and service tag `mock-carrier-service` for all synchronous calls and asynchronous event deliveries[cite: 11].