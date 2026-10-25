# Developer Setup & Testing Guide: [Service Name]

---

## 1. Prerequisites & Tooling
* **JDK Version**: Eclipse Temurin OpenJDK 21
* **Build System**: Gradle 8.x (`./gradlew`)
* **Container Runtime**: Docker Desktop / Colima
* **CLI Tools**: `aws-cli`, `curl`, `kcat`

---

## 2. Local Environment Setup

### 1. Configure Environment Variables
```bash
cp .env.example .env
```

| Variable Name | Default Local Value | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | Local HTTP binding port |
| `SPRING_PROFILES_ACTIVE` | `local` | Activates local application configuration |
| `KAFKA_BOOTSTRAP_SERVERS`| `localhost:9092` | Broker address |
| `POSTGRES_JDBC_URL` | `jdbc:postgresql://localhost:5432/service_db` | Database connection URL |

### 2. Start Infrastructure Dependencies
```bash
# Starts service-specific Postgres, Redis, or Kafka instances
docker compose up -d
```

### 3. Run the Microservice
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## 3. Testing Workflows

### Run Unit Tests
```bash
./gradlew test
```

### Run Integration Tests (Uses Testcontainers)
```bash
./gradlew integrationTest
```

### Run Contract & Pact Verification
```bash
./gradlew pactVerify
```

---

## 4. Local Seed Data & Debugging
* **Health Check**: `curl -f http://localhost:8080/actuator/health`
* **Metrics Endpoint**: `curl http://localhost:8080/actuator/prometheus`
* **Sample Payload**:
```bash
curl -X POST http://localhost:8080/api/v1/resource \
  -H "Content-Type: application/json" \
  -d '{"trackingNumber": "TEST123456", "carrier": "UPS"}'
```