# C4 Architecture Model: Returns Orchestrator Platform

---

## Level 1: System Context Diagram
High-level perspective detailing human actors and external integrations.

[ Customer / Web Client ] ────► (Submits Return) ────► [ Returns Orchestrator Platform ]
[ Warehouse Inspector ]   ────► (Submits Grade)  ────► [ Returns Orchestrator Platform ]
│
├─► [ External Payment Gateway ]
└─► [ Carrier Logistics API ]

---

## Level 2: Container Diagram
Details executable applications, microservices, databases, and message brokers.

[ API Gateway ] ──────► (HTTP / OAuth2) ──────► [ Return Orchestrator Service ]
│
├─► [ PostgreSQL (R2DBC Outbox) ]
├─► [ Redis (Redlock / Cache) ]
└─► [ Kafka Event Broker ]
│
├─► [ Inventory Service ]
└─► [ Payment Service ]

---

## Level 3: Component Diagram (`return-orchestrator-service`)
Internal modular organization within the single microservice boundary.

┌────────────────────────────────────────────────────────────────────────┐
│ return-orchestrator-service                                           │
│                                                                        │
│  ┌───────────────────────┐             ┌────────────────────────────┐  │
│  │ WebFlux Routers &     │ ──────────► │ Saga Orchestrator Engine   │  │
│  │ Endpoint Controllers  │             │ (State Machine Logic)      │  │
│  └───────────────────────┘             └─────────────┬──────────────┘  │
│                                                      │                 │
│                                                      ▼                 │
│  ┌───────────────────────┐             ┌────────────────────────────┐  │
│  │ Outbox Sweeper        │ ◄────────── │ R2DBC Reactive Repository  │  │
│  │ (Scheduled Publisher) │             │ (ACID Transaction Layer)   │  │
│  └───────────┬───────────┘             └────────────────────────────┘  │
└──────────────┼─────────────────────────────────────────────────────────┘
▼
[ Kafka Event Broker ]