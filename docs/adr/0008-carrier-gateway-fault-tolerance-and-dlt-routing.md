# 0008. Carrier Gateway Fault Tolerance and DLT Error Propagation

## Status
Accepted

## Context
The `carrier-gateway` microservice consumes `ReturnInitiatedEvent` messages from Kafka (`returns.order.initiated.v1`) and interacts with external third-party logistics (3PL) APIs via `WebClient` to generate return shipping labels.

External 3PL integrations are inherently subject to network latency, service outages, and rate limits. Previously, carrier failures risked being "swallowed" via local try-catch blocks or unhandled reactive `.subscribe()` callbacks. Returning dummy status payloads (e.g., `"PENDING_GENERATION"`) or suppressing errors prevented Spring Kafka from detecting processing failures, causing:
1. False success states in the overall return saga execution.
2. Accidental publication of invalid `ReturnLabelReadyEvent` messages to downstream services.
3. Inability to capture and route unprocessable event payloads to a Dead Letter Topic (DLT) for automated saga compensation.

We required a fail-fast fault tolerance and error propagation architecture that allows Resilience4j to manage transient network retries while guaranteeing unhandled failures propagate directly to Spring Kafka for DLT routing.

## Decision
We decided to implement a multi-tiered fault tolerance, reactive error propagation, and Dead Letter Topic architecture within `carrier-gateway`:

1. **Application-Level Resilience (Resilience4j + WebClient):**
    - Guard external carrier calls in `CarrierClient` using Resilience4j `@Retry` and `@CircuitBreaker` annotations.
    - Configure `@Retry` to wrap `@CircuitBreaker` so client retries run prior to circuit state transitions.
    - On retry exhaustion or open circuit states, the fallback method (`requestLabelFallback`) returns `Mono.error()` containing the root cause exception instead of returning a fallback string or dummy DTO.

2. **Reactive Error Propagation & Downstream Guardrails:**
    - In `CarrierGatewayService`, chain operations using `.flatMap()` rather than `.doOnSuccess()`. If `CarrierClient` returns `Mono.error()`, the reactive pipeline halts immediately, bypassing publication of `ReturnLabelReadyEvent`.

3. **Synchronous Thread Unblocking at Listener Boundary:**
    - In `ReturnInitiatedConsumer`, use `.block()` on the reactive stream instead of asynchronous, fire-and-forget `.subscribe()`. This forces Reactor exceptions onto the Kafka listener thread, allowing Spring Kafka's container exception handler to intercept failure signals.

4. **Dedicated DLT Routing & Property Binding:**
    - Configure Spring Kafka's `CommonErrorHandler` using `DeadLetterPublishingRecoverer` in `KafkaConsumerConfig`.
    - Explicitly route failed messages to a domain-descriptive DLT topic: `returns.carrier-label.failed.v1.DLT`.
    - Bind topic names dynamically and type-safely via `KafkaTopicProperties` (`@ConfigurationProperties(prefix = "kafka.topics")`).
    - Set Kafka container retries to 0 (`FixedBackOff(0L, 0)`) to delegate application retries entirely to Resilience4j before initiating DLT recovery.

5. **Telemetry & Observability:**
    - Implement `Resilience4jLoggingConfig` as an `onRetry` event consumer subscribed to `RetryRegistry` to output structured logs containing attempt numbers, backoff durations, and root-cause exceptions.

## Consequences

### Positive
* **Zero Silent Failures:** Eliminates swallowed exceptions and prevents corrupt/incomplete events from entering the system.
* **Separation of Retry Concerns:** Prevents retry amplification by cleanly separating application-level HTTP retries (Resilience4j, 3 attempts) from Kafka consumer container retries (0 attempts).
* **Automated Saga Compensation:** Routing failed records to `returns.carrier-label.failed.v1.DLT` provides a reliable trigger for `returns-service` to transition return states to `FAILED_CARRIER_UNAVAILABLE`.
* **Auditability & Traceability:** Original event payloads and exception stack traces (`kafka_dlt-exception-message`) are preserved in Kafka message headers.

### Negative / Trade-offs
* **Listener Thread Blocking:** Calling `.block()` inside `@KafkaListener` holds the Kafka consumer thread until the reactive HTTP pipeline completes, slightly reducing peak throughput in exchange for strict transactional error guarantees.