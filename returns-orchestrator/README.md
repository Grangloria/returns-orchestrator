# Return Orchestrator Service (`return-orchestrator`)

The **Return Orchestrator Service** manages customer return lifecycles. It processes return requests, evaluates eligibility, coordinates label generation via the Carrier Service, and emits domain lifecycle events.

---

## Technical Stack & Configuration

* **Port:** `8080`
* **Tech Stack:** Java 21, Spring Boot 3.x, Spring WebFlux, Spring Kafka, Micrometer Tracing
* **Service Type:** REST API Gateway & Saga Orchestrator

### `application.yml`
```yaml
server:
  port: 8080

spring:
  application:
    name: return-orchestrator
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

carrier:
  service:
    base-url: ${CARRIER_SERVICE_URL:http://localhost:8020}

logging:
  level:
    root: INFO
    com.grangloria.returns: DEBUG
```

# Strategy Factory
This package contains the logic for **Strategy Resolution**.

### Responsibilities:
- **TriageFactory / StrategyResolver:** Acts as the single point of entry for selecting the correct processing path.
- **Selection Logic:** Evaluates incoming `ReturnRequest` metadata (e.g., weight, dimensions, SKU type) to determine which implementation from the `impl` package should be used.

### Why this exists:
It keeps the `orchestration` layer clean. The Orchestrator simply asks the Factory for "the right tool for this job" without needing to know the specific business rules behind the choice.

# Strategy Engine
Implements the **Strategy Design Pattern** to determine triage logic.

### Responsibilities:
- Deciding the processing path (LTL vs. Parcel) based on product metadata and weight.
- `impl/`: Contains specific implementations like `LtlStrategy` and `ParcelStrategy`.

# Strategy Implementations
This package contains the concrete logic for the various return pathways. Each class here implements the `TriageStrategy` interface.

### The "How it Works"
The system uses the **Strategy Design Pattern**. At runtime, the `ReturnOrchestrator` looks at the metadata of a return (weight, dimensions, location) and picks the correct implementation from this folder.

### Current Implementations:
- **ParcelStrategy:** - Handles standard consumer returns (shoes, electronics, etc.).
    - Optimized for carriers like FedEx, UPS, or USPS.
    - Logic focuses on printable labels and drop-off locations.

- **LtlStrategy (Less-Than-Truckload):** - Handles heavy, bulky, or palletized items (furniture, appliances).
    - Logic focuses on scheduling freight pick-ups and specialized bill-of-lading (BOL) generation.

### Adding New Strategies:
To add a new processing path (e.g., `InternationalStrategy` or `HazardousMaterialStrategy`):
1. Create a new class in this directory.
2. Implement `TriageStrategy`.
3. Annotate with `@Component("SpecificName")`.

# Repository Layer (Persistence)
This package handles the communication with **SQL Server**.

### Tech Stack:
- **Spring Data R2DBC:** Reactive, non-blocking database access.
- **ManifestRepository:** CRUD operations for persisting return manifests and tracking states.

# Orchestration Layer
The "Traffic Controller" of the system.

### Responsibilities:
- Coordinating the flow between the **Strategy** engine, **External Clients**, and the **Repository**.
- Managing reactive streams (`Flux` and `Mono`) to ensure non-blocking execution across the entire lifecycle of a return.