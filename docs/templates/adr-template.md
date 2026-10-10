# ADR 0008: Carrier Gateway Fault Tolerance and DLT Routing

- **Status:** Accepted
- **Date:** 2026-10-10
- **Deciders:** James Hayes (Staff Engineer)
- **Technical Domain:** Messaging / Integration Fault Tolerance

---

1. ## Context & Problem Statement

The `carrier-gateway` microservice consumes `ReturnInitiatedEvent` messages from Kafka (`returns.order.initiated.v1`) and interacts with external third-party logistics (3PL) carrier APIs via Spring WebFlux `WebClient` to generate return shipping labels.

External 3PL carrier APIs are inherently prone to network timeouts, service degradation, and rate limiting. Previously, handling HTTP failures inside non-blocking `.subscribe()` callbacks swallowed reactive exceptions (`Mono.error()`). This caused Spring Kafka to treat failed executions as successfully processed, commit partition offsets, omit dead-letter routing, and risk publishing false downstream events (`ReturnLabelReadyEvent`).

---

## 2. Decision Drivers & Forces
- **Reliability:** Must guarantee zero swallowed error signals and route unrecoverable carrier integration failures directly to a dedicated Dead Letter Topic (DLT) with full event context.
- **Isolation of Concerns:** Must cleanly separate application-level HTTP retries (Resilience4j) from Kafka consumer container retries to prevent container-level retry amplification and partition blocking.
- **Data Integrity:** Must strictly prevent downstream event emission (`ReturnLabelReadyEvent`) whenever external carrier integration fails.

---

## 3. Options Considered
1. **Option 1 - Asynchronous Fire-and-Forget (`.subscribe()` with local logging):** Executes WebClient calls asynchronously on Reactor threads. **Pros:** Non-blocking listener thread execution. **Cons:** Swallows errors; Spring Kafka auto-commits offsets; failed messages never route to a DLT.
2. **Option 2 - Container-Level Consumer Retries (`DefaultErrorHandler` backoff):** Delegates failure retries to Spring Kafka listener retries. **Pros:** Standard Spring Kafka retry pattern. **Cons:** Re-queues Kafka records and blocks partition consumption; creates retry amplification when combined with HTTP client retries.
3. **Option 3 - Resilience4j Client Retries + Synchronous `.block()` Unblocking + Spring Kafka `DeadLetterPublishingRecoverer`:** Combines Resilience4j `@Retry` and `@CircuitBreaker` on `CarrierClient` with `.block()` inside `@KafkaListener`, and delegates failure routing to Spring Kafka's `CommonErrorHandler` mapped to `returns.carrier-label.failed.v1.DLT` via `KafkaTopicProperties`. **Pros:** Eliminates false success events, isolates HTTP retries, and guarantees automatic DLT routing for saga compensation. **Cons:** Holds the listener thread during HTTP execution.

---

## 4. Decision Outcome
**Chosen Option:** Option 3 - Resilience4j Client Retries + Synchronous `.block()` Unblocking + Spring Kafka `DeadLetterPublishingRecoverer`

### Justification
Option 3 isolates HTTP retries to Resilience4j (3 attempts) while setting container retries to 0 (`FixedBackOff(0L, 0)`), eliminating retry amplification. Calling `.block()` inside `@KafkaListener` surfaces reactive errors (`Mono.error()`) directly to Spring Kafka's `CommonErrorHandler`, allowing `DeadLetterPublishingRecoverer` to publish unrecoverable failures to `returns.carrier-label.failed.v1.DLT` with original headers and exception traces intact.

---

## 5. System Consequences & Trade-offs

### Positive
- **Zero Swallowed Exceptions:** Halts downstream reactive pipelines on failure, preventing invalid `ReturnLabelReadyEvent` messages from entering the cluster.
- **Automated Saga Compensation Trigger:** Unprocessable events in `returns.carrier-label.failed.v1.DLT` provide a deterministic payload for `returns-service` to transition return states to `FAILED_CARRIER_UNAVAILABLE`.
- **Type-Safe DLT Property Binding:** Binds DLT topic definitions dynamically using `@ConfigurationProperties` (`KafkaTopicProperties`).

### Negative & Mitigation
- **Drawback:** Calling `.block()` inside `@KafkaListener` holds the Kafka consumer thread until HTTP retries exhaust.
  - **Mitigation:** Resilience4j `CircuitBreaker` transitions to `OPEN` after consecutive failures, short-circuiting calls in ~50ms to preserve listener thread availability.
- **Drawback:** Resilience4j does not log individual retry attempt numbers by default.
  - **Mitigation:** Implemented `Resilience4jLoggingConfig` as an `onRetry` event consumer (`EventConsumer<RetryOnRetryEvent>`) attached to `RetryRegistry` for structured attempt logging.

---

## 6. Observability & Verification
- **Metrics:** Tracked via Resilience4j Actuator metrics (`resilience4j.retry.calls`, `resilience4j.circuitbreaker.state`).
- **Logs:** MDC trace correlation via `traceId` and `spanId`; structured retry attempt logs via `Resilience4jLoggingConfig` (`[RESILIENCE4J-RETRY] Attempt #X failed...`); error headers (`kafka_dlt-exception-message`, `kafka_dlt-original-topic`) preserved in DLT message metadata.