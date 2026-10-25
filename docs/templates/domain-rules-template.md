# Domain Rules & Business Invariants: [Service Name]

* **Service**: [e.g., refund-service]
* **Bounded Context**: [e.g., Financial Settlement]
* **Last Updated**: [YYYY-MM-DD]
* **Owner**: [Team or Tech Lead]

---

## 1. Core Invariants (Non-Negotiable Business Rules)
*Rules that must never be violated under any transaction or state transition.*

* **INV-01**: [e.g., A refund can only be initiated after items reach RECEIVED or RESTOCKED state.]
* **INV-02**: [e.g., Total refund amount cannot exceed the original authorized settlement amount.]
* **INV-03**: [e.g., Return window is strictly bounded to 30 days from customer delivery timestamp.]

---

## 2. Validation Constraints & Business Logic

### Field Validations
| Field Name | Type / Format | Validation Rule | Error on Failure |
| :--- | :--- | :--- | :--- |
| `returnId` | UUIDv4 | Must exist in `returns-service` | `ERR_RETURN_NOT_FOUND` (404) |
| `refundAmount` | Decimal (2 dp) | Greater than 0.00; <= line item sum | `ERR_INVALID_AMOUNT` (422) |
| `currency` | ISO-4217 | Must match original order currency | `ERR_CURRENCY_MISMATCH` (400) |

### Edge Case & Calculation Policies
* **Tax Proration**: [Define how sales tax is recalculated on partial returns.]
* **Shipping Fee Deductions**: [Define conditions under which return shipping fees are deducted.]
* **Promotion / Coupon Clawback**: [Define how volume discounts are forfeited or adjusted.]

---

## 3. Error Handling & Gateway Mapping

| Business Error Code | HTTP Status Code | Kafka DLQ Action | Client Retryable? |
| :--- | :--- | :--- | :--- |
| `ERR_PRECONDITION_FAILED` | `412 Precondition Failed` | Discard (Log Sev-3) | No |
| `ERR_IDEMPOTENCY_CONFLICT`| `409 Conflict` | Commit Offset | No (Return cached state) |
| `ERR_CARRIER_UNAVAILABLE` | `503 Service Unavailable`| Retry with backoff | Yes |