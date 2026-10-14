# Transporter Lane Optimizer

> **Enterprise-grade Transporter Assignment Optimization Engine** for Trade Lanes  
> Built with **Java 21**, **Spring Boot 3.4.x**, **Spring Data JPA**, **Flyway**, and **PostgreSQL**.

---

## Table of Contents
1. [Overview & Core Objectives](#overview--core-objectives)
2. [Mathematical Formulation & Objective Decision](#mathematical-formulation--objective-decision)
3. [Verified Reference Calculations](#verified-reference-calculations)
4. [Algorithm & Architecture Design](#algorithm--architecture-design)
5. [API Specification](#api-specification)
6. [Quick Start & Running Locally](#quick-start--running-locally)
7. [Testing & Verification](#testing--verification)
8. [Reproducing Sample Test Cases via cURL](#reproducing-sample-test-cases-via-curl)
9. [Deployment (Docker & Cloud)](#deployment-docker--cloud)

---

## 1. Overview & Core Objectives

In logistics procurement, platforms evaluate transporter quotes across multiple trade lanes to identify the most cost-effective and operationally resilient allocations.

This service implements a deterministic optimization engine meeting three core requirements:
1. **Full Lane Coverage**: Every requested trade lane is assigned to exactly one transporter with a valid quote.
2. **Maximize Transporter Usage**: Distinct transporters actually assigned lanes is maximized up to a user-specified limit $K$.
3. **Minimize Total Cost**: Among allocations achieving maximal transporter usage, the allocation minimizing the sum of quotes is selected.

---

## 2. Mathematical Formulation & Objective Decision

The assignment requirements define two primary optimization goals: minimizing total cost and utilizing the maximum number of transporters up to $K$.

### Interpretation Decision: Interpretation B (Lexicographic)
We adopt **Interpretation B**:
$$\max \left( |\text{Distinct Transporters Actually Assigned}| \right) \quad \text{subject to } \le K$$
$$\text{followed by } \min \sum_{l \in \text{Lanes}} \text{Cost}(l, \text{Transporter}(l))$$

- **Constraint**: Each selected transporter in `selectedTransporters` **must receive at least one assigned trade lane**. No unassigned transporters are ever included.
- **Ceiling / Pigeonhole Invariant**: If there are $L$ lanes, at most $L$ distinct transporters can ever be utilized. Thus, maximum possible usage is $\min(K, N, L)$.
- **Deterministic Tie-Breaking**: When multiple distinct allocations produce identical maximal usage and identical minimum cost, ties are resolved deterministically by lexicographically smallest transporter ID.

---

## 3. Verified Reference Calculations

All calculations have been independently verified through exhaustive enumeration and exact arithmetic.

### Test Case 1 (5 Lanes, 7 Transporters)
*Data extracted from `transporter_assignment_data.csv` (Lanes 1 to 5)*:

| Lane | T1 | T2 | T3 | T4 | T5 | T6 | T7 |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Lane 1** | 20,835 | 48,844 | 39,020 | 14,400 | **11,601** | 35,095 | 26,070 |
| **Lane 2** | **10,512** | 31,326 | 20,648 | 44,514 | 19,760 | 12,494 | 41,098 |
| **Lane 3** | 22,105 | 18,640 | 31,438 | **14,316** | 40,870 | 17,808 | 20,932 |
| **Lane 4** | 42,481 | 45,828 | 36,447 | **10,678** | 20,635 | 36,210 | 16,897 |
| **Lane 5** | 19,862 | 18,297 | 12,789 | **13,032** | 26,421 | 39,444 | 27,938 |

- **$K = 3$**:
  - Distinct usage: **3** transporters (`{1, 4, 5}`).
  - Assignments: Lane 1 → T5 (11,601), Lane 2 → T1 (10,512), Lane 3 → T4 (14,316), Lane 4 → T4 (10,678), Lane 5 → T4 (13,032).
  - Total Cost: $11,601 + 10,512 + 14,316 + 10,678 + 13,032 =$ **60,139** *(PDF claimed 75,000, which contradicted its own quote table and was mathematically suboptimal).*
- **$K = 5$ (Interpretation B divergence)**:
  - Distinct usage: **5** transporters (`{1, 3, 4, 5, 6}`).
  - Assignments: Lane 1 → T5 (11,601), Lane 2 → T1 (10,512), Lane 3 → T6 (17,808), Lane 4 → T4 (10,678), Lane 5 → T3 (12,789).
  - Total Cost: $11,601 + 10,512 + 17,808 + 10,678 + 12,789 =$ **63,388**.
- **$K = 7$**:
  - 5 trade lanes can allocate at most 5 distinct transporters (pigeonhole principle). Maximum usage remains 5, cost = **63,388**.

### Test Case 2 (5 Lanes, 4 Transporters with Sparse Quotes)
- Transporter X (1): Lane 1 = 1,000, Lane 2 = 1,500
- Transporter Y (2): Lane 3 = 2,000, Lane 4 = 1,800
- Transporter Z (3): Lane 5 = 1,200
- Transporter W (4): Lane 1 = 1,100, Lane 4 = 1,750

- **$K = 1, 2$**: **Proven Infeasible (HTTP 422)**. Lanes 2, 3, and 5 are single-quoted by X, Y, and Z respectively, requiring at least 3 distinct transporters.
- **$K = 3$**: Mandatory subset `{X, Y, Z}`. Total Cost = $1,000 + 1,500 + 2,000 + 1,800 + 1,200 =$ **7,500**.
- **$K = 4$**: All 4 utilized (`{X, Y, Z, W}`). Lane 4 reallocated from Y to W ($1,750$ vs $1,800$). Total Cost = **7,450**.

---

## 4. Algorithm & Architecture Design

### Package Structure
```
com.freightfox.laneoptimizer
├── domain/          ← Immutable Records & Sealed Outcome Interfaces (Pure Java)
├── optimizer/       ← Pure Java Optimizer (Zero Spring imports; 100% unit testable)
│   ├── QuoteMatrix.java
│   ├── AssignmentStrategy.java
│   ├── ExhaustiveStrategy.java
│   ├── LocalSearchStrategy.java
│   ├── ResultAssembler.java
│   └── LaneOptimizer.java (Facade)
├── entity/          ← JPA Entities (LaneEntity, TransporterEntity, LaneQuoteEntity)
├── repository/      ← Spring Data JPA Repositories
├── dto/             ← Request/Response Records with Jakarta Bean Validation
├── exception/       ← Domain Exceptions (InvalidInputException, etc.)
├── service/         ← InputService (Transactional Replace) & AssignmentService
├── web/             ← TransporterController, GlobalExceptionHandler, HomeController
└── config/          ← AppConfig, DatabaseUrlEnvironmentPostProcessor
```

### Solvers & Pruning Mechanics
1. **`ExhaustiveStrategy`**:
   - Enumerates combinations of size $u = \min(K, N, L)$ down to 1.
   - For each candidate subset, explores backtracking allocations where **all** transporters in the subset receive $\ge 1$ lane.
   - **Prune 1 (Running Cost Bound)**: Aborts branch if `runningCost >= bestCost`.
   - **Prune 2 (Pigeonhole Invariant)**: Aborts if remaining unassigned lanes $<$ unused transporters in subset.
   - **Prune 3 (Lower Bound Pruning)**: Sums minimum possible remaining lane costs; aborts if `runningCost + lowerBound >= bestCost`.
   - **Global Node Budget**: Hard cap of `10,000,000` nodes across the entire invocation prevents hangs.
2. **`LocalSearchStrategy`**:
   - Greedy seed with mandatory transporter detection.
   - Usage expansion invariant: accepts reallocation to new transporter if and only if donor retains $\ge 1$ other lane (guarantees usage strictly increases by +1).
   - Cost optimization swaps.
3. **`LaneOptimizer` Facade Policy**:
   - Pre-check: orphan lanes $\to$ **HTTP 422**.
   - Subsets $\le 50,000 \to$ executes `ExhaustiveStrategy`.
   - If complete $\to$ returns optimal result (`optimal: true`). If proven impossible $\to$ **HTTP 422**.
   - If budget hit $\to$ falls back to `LocalSearchStrategy`.
   - If both fail to determine feasibility $\to$ **HTTP 503** (`SolverBudgetExceededException`). **Never falsely returns 422.**

---

## 5. API Specification

Interactive Swagger UI documentation is available at:
```
http://localhost:8080/swagger-ui.html
```
*(Visiting `/` automatically redirects to `/swagger-ui.html`)*

### Endpoints
| Method | Path | Description | Success Code |
|---|---|---|:---:|
| `POST` | `/api/v1/transporters/input` | Uploads trade lanes and transporter quotes (atomically replaces active dataset) | `200 OK` |
| `POST` | `/api/v1/transporters/assignment` | Computes optimal assignment subject to `maxTransporters` | `200 OK` |

### Error Response Schema
```json
{
  "status": "error",
  "message": "Validation failed for request body",
  "details": ["lanes list cannot be empty"]
}
```

---

## 6. Quick Start & Running Locally

### Prerequisites
- **JDK 21** or later
- **Maven 3.9+**
- **Docker** & **Docker Compose** (optional for containerized setup)

### Running with Local Maven
```bash
cd freightfox-lane-optimizer
mvn clean spring-boot:run
```
*(By default, uses PostgreSQL if configured, or in-memory H2 if testing).*

### Running with Docker Compose
```bash
docker-compose up --build
```
This boots:
1. `freightfox-postgres` on port `5432`
2. `freightfox-lane-optimizer` on port `8080` (with Flyway database migrations automatically applied)

---

## 7. Testing & Verification

Run the complete test suite:
```bash
mvn clean test
```

### Verified Test Results
- **49 unit and integration tests** passing:
  - `ExhaustiveStrategyTest` (14 tests): Verifies TC1 K=3 (60,139), TC1 K=5 (63,388), TC2 K=3 (7,500), TC2 K=4 (7,450), TC2 K=2 (Infeasible), lexicographic priority 1 over priority 2, single transporter multiple lanes, multiple transporters single lane, non-contiguous IDs, large 64-bit quotes, deterministic tie-breaking, and budget exhaustion.
  - `LocalSearchStrategyTest` (4 tests): Validates expansion invariant, coverage preservation, greedy failure handling, and sparse quoter seeds.
  - `LaneOptimizerTest` (7 tests): Validates combination threshold, overflow protection capping at Long.MAX_VALUE, orphan lane detection, budget fallback, and 503 error handling.
  - `DatabaseUrlEnvironmentPostProcessorTest` (2 tests): Validates automatic parsing and conversion of cloud PostgreSQL URIs (e.g., Render/Railway) into JDBC format.
  - `QuoteMatrixTest` (2 tests): Validates matrix creation, sentinels, and orphan lane identification.
  - `ResultAssemblerTest` (2 tests): Validates assignment extraction and illegal state assertions.
  - `InputServiceTest` (5 tests): Validates transactional replacement, duplicate lanes, duplicate transporters, duplicate quotes, and unknown lane references.
  - `TransporterControllerIT` (11 tests): End-to-end integration tests verifying HTTP 200, 400 (negative quotes, bad JSON, missing fields, duplicate IDs, unknown lane quote), 404, and 422 status codes against H2 in PostgreSQL mode.

---

## 8. Reproducing Sample Test Cases via cURL

### Test Case 1 ($K = 3 \implies 60,139$)

```bash
# 1. Ingest Test Case 1 data
curl -X POST http://localhost:8080/api/v1/transporters/input \
  -H "Content-Type: application/json" \
  -d @samples/test-case-1-input.json

# 2. Compute Assignment with maxTransporters = 3
curl -X POST http://localhost:8080/api/v1/transporters/assignment \
  -H "Content-Type: application/json" \
  -d '{"maxTransporters": 3}'
```
**Expected Response:**
```json
{
  "status": "success",
  "totalCost": 60139,
  "optimal": true,
  "assignments": [
    { "laneId": 1, "transporterId": 5, "cost": 11601 },
    { "laneId": 2, "transporterId": 1, "cost": 10512 },
    { "laneId": 3, "transporterId": 4, "cost": 14316 },
    { "laneId": 4, "transporterId": 4, "cost": 10678 },
    { "laneId": 5, "transporterId": 4, "cost": 13032 }
  ],
  "selectedTransporters": [1, 4, 5]
}
```

---

### Test Case 2 ($K = 3 \implies 7,500$, $K = 4 \implies 7,450$, $K = 2 \implies 422$)

```bash
# 1. Ingest Test Case 2 data
curl -X POST http://localhost:8080/api/v1/transporters/input \
  -H "Content-Type: application/json" \
  -d @samples/test-case-2-input.json

# 2. Compute Assignment with K = 3
curl -X POST http://localhost:8080/api/v1/transporters/assignment \
  -H "Content-Type: application/json" \
  -d '{"maxTransporters": 3}'
# Output totalCost: 7500, selectedTransporters: [1, 2, 3]

# 3. Compute Assignment with K = 4
curl -X POST http://localhost:8080/api/v1/transporters/assignment \
  -H "Content-Type: application/json" \
  -d '{"maxTransporters": 4}'
# Output totalCost: 7450, selectedTransporters: [1, 2, 3, 4]

# 4. Infeasible Assignment with K = 2
curl -X POST http://localhost:8080/api/v1/transporters/assignment \
  -H "Content-Type: application/json" \
  -d '{"maxTransporters": 2}'
# Output HTTP 422 Unprocessable Entity
```

---

## 9. Deployment (Docker & Cloud)

### Build Docker Image
```bash
docker build -t freightfox-lane-optimizer:latest .
```

### Run Container with External PostgreSQL
```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<dbname> \
  -e SPRING_DATASOURCE_USERNAME=<user> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  freightfox-lane-optimizer:latest
```

The repository includes `render.yaml` for 1-click infrastructure deployment on Render with managed PostgreSQL.
