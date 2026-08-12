# Architectural Specification: AES to EMCS Core Integration

## 1. Executive Summary & Business Context

### Background & Discovery Goals
Under EMCS Phase 4.1 and Union Customs Code (UCC) modernization, HMRC is replacing legacy batch-reconciled processes with a real-time event-driven interface between the **Automated Export System (AES)** and **EMCS Core**. 

### Key Technical Objectives
* **Pre-Clearance Validation Gatekeeper (`checkArc`)**: A single synchronous, blocking check executed prior to export declaration release to validate active ARC status, trader EORIs, commodity codes, and duty-suspended quantity balances.
* **Real-Time Lifecycle State Engine**: Post-clearance events (`IE829`, `IE590`, etc.) operate as an asynchronous, non-blocking stream ingested via perimeter queues to ensure customs port operations never halt.
* **Automation of Tax Guarantee Release**: Eliminates manual paper reconciliation by automatically closing 18-month excise movements and releasing financial guarantees upon confirmation of physical border exit (`IE590`).

---

## 2. Actors & System Boundaries

### System Context Actors (Machine-to-Machine Level)
* **AES (Automated Export System)**: Client system initiating synchronous validation queries (`checkArc`) and streaming lifecycle event notifications (`IE829`, `IE590`, etc.).
* **EMCS Core**: Target service provider hosting perimeter validation (RIM Service), queue buffers (RabbitMQ), state engines, and data APIs.

### Business Use Case Actors (Triggers)
* **Trader / Declarant**: Triggers export declaration submission in AES, initiating the synchronous pre-clearance validation.
* **Customs Office of Exit / Border Officer**: Confirms physical border departure of cargo, triggering the asynchronous `IE590` / `IE818` exit notification.
* **EMCS Core Engine**: Autonomous processing actor handling background state transitions, SEED registry checks, and financial guarantee releases.

---

## 3. Comprehensive Operations Matrix

| # | Operation / Message | Pattern | Trigger Event in AES | Payload Highlights | EMCS Core Action |
| :- | :--- | :--- | :--- | :--- | :--- |
| **1** | **`checkArc`** | **Synchronous** | Export declaration submission | ARC, Declarant EORI, Consignor ID, Tariff/EPC codes, Declared Quantities, Net Mass | Validates ARC status, trader permissions, and quantity balances; reserves balance temporarily. Returns `VALID`/`INVALID`. |
| **2** | **`IE829`** (Export Accepted) | **Asynchronous** | Declaration accepted and released by customs | Customs MRN, ARC, Export Release Timestamp | Binds MRN to ARC; updates movement state to *Export Released*. |
| **3** | **`IE836`** (Export Invalidated) | **Asynchronous** | Export declaration cancelled/withdrawn | Customs MRN, ARC, Invalidation Reason | Reverts movement state back to *In Transit* so goods can be re-routed. |
| **4** | **`IE839`** (Customs Rejection) | **Asynchronous** | Declaration rejected by customs checks | Customs MRN, ARC, Line-item Rejection Error Codes | Logs inspection/paperwork failures and flags movement for trader remediation. |
| **5** | **`IE590 / IE818`** (Physical Exit) | **Asynchronous** | Customs Office of Exit confirms border departure | Customs MRN, ARC, Exit Confirmation Details | Generates *Report of Export*, closes 18-month movement, and releases financial guarantee. |
| **6** | **`IE837 / IE840`** (Delay / Control) | **Asynchronous** | Border transit delay or physical inspection | Customs MRN, ARC, Discrepancy / Delay Details | Logs transit delay reasons or audit discrepancy results in movement history without closing movement. |

---

## 4. End-to-End Sequence & Workflow Diagrams

### 4.1 Synchronous Pre-Clearance Validation Flow (`checkArc`)

```text
[ Declarant / Trader ]
         │
         │ (1) Submit Export Declaration
         ▼
 ┌──────────────┐
 │     AES      │
 └──────┬───────┘
        │
        │ (2) POST /emcs/api/v1/exports/arc-verifications (Blocking Sync Call)
        ▼
 ┌─────────────────────────────────────────────────────────┐
 │                       RIM Service                       │  <-- Perimeter Guard
 │  • XSD Structural Schema Validation                      │
 │  • Version Selection (v4.0 / v4.2)                       │
 └──────┬──────────────────────────────────────────────────┘
        │
        │ (3) Direct Query (Bypasses Message Queues)
        ▼
 ┌─────────────────────────────────────────────────────────┐
 │                  System APIs / Read DB                  │
 │  • Validate ARC Active Status & Expiry                  │
 │  • Cross-check Declarant EORI & Consignor ID            │
 │  • Validate EPC / Tariff Codes                          │
 │  • Verify Quantity Balance & Execute Redis Temp Hold    │
 └──────┬──────────────────────────────────────────────────┘
        │
        │ (4) Return HTTP 200 OK (Status: VALID / INVALID)
        ▼
 ┌──────────────┐
 │     AES      │ ── (5) If VALID: Accept & Release Declaration
 └──────────────┘ ── (6) If INVALID: Reject Declaration & Alert Trader
```

### 4.2 Asynchronous Lifecycle Event Stream Flow (`IE829`, `IE590`, etc.)

```text
 [ Customs Officer / Exit Gate ]
                │
                │ (1) Confirm Departure / Event Trigger
                ▼
        ┌──────────────┐
        │     AES      │
        └──────┬───────┘
               │
               │ (2) POST Async Event Notification (e.g., IE590 Exit)
               ▼
┌───────────────────────────────────────────────┐
│                  RIM Service                  │
│ • XSD Schema Validation                       │
│ • Return Immediate HTTP 202 Accepted to AES   │  <-- AES Unblocked Immediately
└──────────────┬────────────────────────────────┘
               │
               │ (3) Route Payload
               ▼
┌───────────────────────────────────────────────┐
│     Receiver Proxy & Main Queue (RabbitMQ)     │
│ • Circuit Breaker / Traffic Shedding Protection │
│ • Retries & Dead Letter Queue (DLQ) Policies  │
└──────────────┬────────────────────────────────┘
               │
               │ (4) Asynchronous Consumption (Leg 2)
               ▼
┌───────────────────────────────────────────────┐
│               EMCS Orchestrator               │
│ • Update Movement Lifecycle State             │
│ • Generate Report of Export                   │
│ • Automatically Close 18-Month Movement       │
│ • Release Financial Guarantee                 │
└───────────────────────────────────────────────┘
```

---

## 5. Synchronous Pre-Clearance (`checkArc`) Deep Dive

### Rationale for HTTP `POST` Verb
`checkArc` is the **sole synchronous validation gatekeeper** in the integration. Although conceptually a query, it uses HTTP `POST` for three structural reasons:
1. **Complex Array Payloads**: Accepts nested arrays of commodity line items (EPCs, 8-digit CN tariff codes, quantities, net mass) that cannot fit within HTTP `GET` query strings.
2. **URL Length & Log Hygiene**: Prevents gateway URI truncation (`414 URI Too Long`) and ensures sensitive trader EORIs and commercial data are encrypted inside the body rather than logged in cleartext server URLs.
3. **State Hold Mechanics**: Enables EMCS Core to execute a short-term balance reservation in Redis to prevent "double-dipping" race conditions across concurrent declarations.

### Payload Data Contract

#### Request Payload (`POST /emcs/api/v1/exports/arc-verifications`)
```json
{
  "arc": "26GB00000000000123456",
  "customsDeclarationMRN": "26GB1234567890ABCD",
  "declarantEori": "GB123456789000",
  "consignorExciseId": "GB00001234567",
  "customsOfficeOfExport": "GB000060",
  "goodsItems": [
    {
      "itemSequenceNumber": 1,
      "commodityCode": "24041200",
      "exciseProductCode": "V100",
      "declaredQuantity": 500.00,
      "unitOfMeasure": "MLR",
      "netMassKg": 45.00
    }
  ]
}
```

#### Successful Response (`HTTP 200 OK`)
```json
{
  "arc": "26GB00000000000123456",
  "customsDeclarationMRN": "26GB1234567890ABCD",
  "overallValidationStatus": "VALID",
  "validationTimestamp": "2026-08-12T14:15:00Z",
  "itemVerificationResults": [
    {
      "itemSequenceNumber": 1,
      "status": "MATCHED",
      "declaredQuantity": 500.00,
      "remainingArcBalance": 2000.00
    }
  ],
  "validationErrors": []
}
```

---

## 6. Vaping Products Duty (VPD) XSD Schema Extensions

To support the October 1st Vaping Products Duty rollout within EMCS Core, the following updates are required within the schema libraries (`rim-service` resources / `emcs-core-schemas`):

* **Excise Product Code (EPC) Restrictions**: Add UK national vaping codes (e.g., `V100` / `V200` series) to the XSD restriction enumerations for `checkArc` and `IE815` payload models.
* **Unit of Measure Additions**: Extend allowed measurement enumerations to include liquid volume metrics (**Milliliters / `MLR`**).
* **Outbound Gateway Suppression Rule**: Header routing logic in the Receiver Proxy must detect UK Vaping EPCs originating in Great Britain (GB) and suppress downstream message dispatch to the EU CCN/Segway gateway, as vaping is a non-EU excise category.

---

## 7. Legacy Architecture vs. Modernized State Engine

| Dimension | Legacy Process (Old Way) | Modernized Architecture (New Way) |
| :--- | :--- | :--- |
| **Pre-Clearance Check** | **Format-only Regex Check**: Customs only verified string lengths in text fields. | **Real-time `checkArc`**: Cross-validates ARC state, trader EORIs, EPC codes, and quantity balances. |
| **Event Synchronization** | **Overnight Batch Files**: Systems exchanged flat files outside business hours with a 24-hour lag. | **Real-Time Asynchronous Streams**: AES sends immediate `IE829`/`IE590` event notifications over queues. |
| **Role of CDAP / CEDA** | Used as an intermediary for delayed reconciliation runs. | Dedicated **OLAP Data Lake**: Receives background event feeds via EIS for long-term audit trails, compliance analytics, and trade statistics. |
| **Discrepancy Resolution** | Discrepancies identified **days later**, requiring human audits and locking guarantees for weeks. | Invalid declarations **blocked upfront** at pre-clearance; movement closure and guarantee release automated upon exit confirmation (`IE590`). |

---

## 8. Summary of User Questions & Discussion Items

### Questions Covered & Answered in Discovery
1. **What is the RIM Service?**
   * *Answer*: Router Interface Module; the perimeter guard performing multi-version XSD schema validation and returning synchronous `400 Bad Request` errors before payloads reach queues.
2. **How are calls balanced between synchronous and asynchronous?**
   * *Answer*: Exactly **1 synchronous call** (`checkArc` for pre-clearance validation) and **5 asynchronous calls** (`IE829`, `IE836`, `IE839`, `IE590`, `IE837/IE840` for lifecycle updates).
3. **What is sent in `checkArc` and is it the sole validation check?**
   * *Answer*: `checkArc` receives the ARC, MRN, Trader EORIs, EPCs, CN Tariff Codes, and quantities in a single `POST` body. It is the sole pre-clearance gatekeeper; subsequent calls are milestone event notifications.
4. **Why is `checkArc` an HTTP `POST`?**
   * *Answer*: To accommodate nested commodity arrays, avoid URL length truncations (`414 URI Too Long`), protect sensitive trade data in server access logs, and enable Redis quantity balance reservation holds.
5. **How did this work prior to the new interface and what is CDAP's role?**
   * *Answer*: Previously handled via format-only regex checks and overnight batch files. CDAP/CEDA is an OLAP data warehouse for long-term auditing and trade statistics, not a real-time transactional broker.
6. **What actors exist in the integration?**
   * *Answer*: 2 System Context Actors (AES and EMCS Core) and 3 Business Trigger Actors (Trader at submission, Customs Officer at border exit, and EMCS Engine).
7. **How does Vaping Products Duty (VPD) affect XSD schemas?**
   * *Answer*: Requires adding new EPC codes (`V***`), units of measure (`MLR`), and EU Segway suppression rules for GB-originating vaping shipments.

### Open Architecture Discussion Items for Team Review
* Confirm TLS/mTLS mutual authentication and API Gateway token validation for `/arc-verifications`.
* Finalize Redis cache TTL duration for `checkArc` temporary quantity balance holds (e.g., 15 minutes).
* Validate Dead Letter Queue (DLQ) alert thresholds and operational replay tooling for Live Support teams.
