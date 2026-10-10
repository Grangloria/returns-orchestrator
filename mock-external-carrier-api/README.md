# Carrier Service

A lightweight, high-performance Spring Boot microservice acting as an **External Carrier API** and **Integration Service** between the internal Return Orchestration system and third-party logistics providers (e.g., FedEx, UPS).

It exposes clean REST APIs for shipping label generation, rate calculations, and tracking requests without coupling to internal event brokers.

---

## 🏗️ Architecture & Responsibilities

* **Anti-Corruption Layer (ACL):** Isolates internal domain models from third-party API payload changes.
* **Carrier Abstraction:** Standardizes carrier interactions so upstream services do not need to handle provider-specific request structures.
* **Pure REST Communication:** Operates entirely over HTTP/REST using Spring WebFlux, acting as a dedicated external integration service for the Return Orchestrator.

---

## 🚀 Tech Stack

* **Language:** Java 21 (or 17)
* **Framework:** Spring Boot 3.x (WebFlux / Netty)
* **HTTP Client:** Spring Reactive `WebClient`
* **Documentation:** OpenAPI 3 / Swagger UI
* **Build Tool:** Gradle

---

## 🛠️ Getting Started

### Prerequisites
* Java 21 JDK
* Gradle 8.x+

### Local Setup

1. **Run the Application**
   ```bash
   ./gradlew bootRun