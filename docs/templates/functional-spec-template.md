# Functional Specification: [Feature / Epic Name]

* **Feature Code**: `FEAT-[Number]`
* **Business Owner**: [Product Manager]
* **Technical Lead**: [Engineering Lead]
* **Target Milestone**: [Sprint / Quarter]

---

## 1. Context & Business Value
[Describe the business requirement, user friction being solved, and value delivered to the enterprise or customer.]

---

## 2. Actors & User Journeys
* **Primary Actor**: [e.g., Authenticated Retail Customer]
* **Secondary Actor**: [e.g., Warehouse Intake Staff]

### Primary User Flow (Happy Path)
1. Customer initiates return via portal selecting item and reason code.
2. System validates item return window (< 30 days).
3. Carrier label is generated and displayed for download.
4. Customer drops package with carrier; tracking updates automatically.

### Alternative / Exception Flows
* **Flow A (Window Expired)**: If purchase date > 30 days, disable selection with clear customer explanation.
* **Flow B (Hazardous Material)**: If item is classified as non-returnable HazMat, trigger instant refund without requiring physical return.

---

## 3. Acceptance Criteria (Gherkin Format)
```gherkin
Scenario: Customer successfully initiates an eligible return
  Given an order in "DELIVERED" status within the 30-day window
  When the customer requests a return for 1 eligible line item
  Then the system transitions the return state to "LABEL_GENERATED"
  And emits a "ReturnInitiatedEvent" to the event bus
  And returns a printable prepaid shipping label URL
```