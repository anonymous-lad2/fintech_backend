# Finance Dashboard API

A production-grade REST API for a multi-role financial records management system,
built with **Spring Boot 4.0**, **PostgreSQL**, **JWT authentication**, and **Docker**.

---

## Table of Contents

1. [Tech Stack & Rationale](#tech-stack--rationale)
2. [Architecture Overview](#architecture-overview)
3. [Project Structure](#project-structure)
4. [Role & Permission Matrix](#role--permission-matrix)
5. [Quick Start (Docker)](#quick-start-docker)
6. [Quick Start (Local)](#quick-start-local)
7. [API Reference](#api-reference)
8. [Design Decisions & Tradeoffs](#design-decisions--tradeoffs)
9. [Database Schema](#database-schema)
10. [Security Model](#security-model)
11. [Testing](#testing)
12. [Environment Variables](#environment-variables)
13. [Assumptions Made](#assumptions-made)
14. [Production Checklist](#production-checklist)

---

## Tech Stack & Rationale

| Layer | Choice | Why this over alternatives |
|---|---|---|
| Framework | Spring Boot 4.0 (Spring Framework 7) | Battle-tested enterprise framework; first-class JPA, Security, Validation. Spring MVC chosen over WebFlux — dashboard data is DB-bound, not I/O-bound, so reactive overhead isn't justified. |
| Language | Java 21 | LTS release; records, sealed classes, virtual threads available. |
| Database | PostgreSQL 16 | ACID transactions critical for financial data. `NUMERIC(19,4)` exact decimals. Superior window functions for analytics vs MySQL. |
| Schema migrations | Flyway | Version-controlled, auditable, rollback-safe schema changes. Never `ddl-auto: create` in production. |
| Authentication | JWT (JJWT 0.12) | Stateless — no session store required, horizontally scalable. HS256 signing for single-service deployment. |
| Password hashing | BCrypt (strength 12) | Adaptive hashing — inherently slow to prevent brute-force. Industry standard. |
| DTO mapping | MapStruct | Compile-time code generation — no runtime reflection, IDE-navigable, type-safe. |
| API docs | SpringDoc OpenAPI 3 | Auto-generates Swagger UI from annotations — always in sync with code. |
| Rate limiting | Bucket4j (token-bucket) | Smooths bursts, no thundering herd at window reset. In-memory for single node. |
| Containerisation | Docker multi-stage build | Build stage discarded — runtime image is JRE-only (~90MB vs ~350MB). |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP Request                             │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Spring Security Filter Chain                   │
│  ┌─────────────────────┐  ┌────────────────────────────────┐   │
│  │ JwtAuthFilter       │  │ RateLimiterService             │   │
│  │ (extracts JWT,      │  │ (per-IP token bucket,          │   │
│  │  sets SecurityCtx)  │  │  login: 10/min, api: 200/min)  │   │
│  └─────────────────────┘  └────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Controllers                              │
│   AuthController  UserController  RecordController  Dashboard  │
│          @PreAuthorize role checks on each method               │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Service Layer                             │
│   AuthService  UserService  FinancialRecordService  Dashboard  │
│          @Transactional — business logic, no HTTP concerns      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Repository Layer                            │
│   UserRepository  FinancialRecordRepository                    │
│   Spring Data JPA + JPA Specifications for dynamic filtering    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  PostgreSQL 16 (via HikariCP)                   │
│          Flyway-managed schema — V1, V2, V3 migrations          │
└─────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

- **Controller** — HTTP in/out only. Parses requests, calls services, returns responses. Zero business logic.
- **Service** — All business rules, access validation, transaction boundaries.
- **Repository** — Data access only. JPQL queries and Specifications live here.
- **Entity** — JPA-mapped domain objects. No business logic.
- **DTO** — Request/response contracts. Decoupled from entities — changes to DB schema don't break API contracts.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/fintech/
│   │   ├── FintechDashboardApplication.java   # Entry point
│   │   ├── config/
│   │   │   ├── JwtProperties.java             # @ConfigurationProperties
│   │   │   ├── OpenApiConfig.java             # Swagger setup
│   │   │   ├── RateLimiterService.java        # Bucket4j per-IP limits
│   │   │   └── SecurityConfig.java            # Filter chain, RBAC
│   │   ├── controller/
│   │   │   ├── AuthController.java            # POST /auth/login
│   │   │   ├── DashboardController.java       # GET /dashboard/summary, /trends
│   │   │   ├── FinancialRecordController.java # CRUD /records
│   │   │   └── UserController.java            # CRUD /users
│   │   ├── dto/
│   │   │   ├── request/                       # Validated inbound payloads
│   │   │   └── response/                      # Outbound API shapes
│   │   ├── entity/
│   │   │   ├── FinancialRecord.java
│   │   │   ├── RecordType.java                # INCOME | EXPENSE
│   │   │   ├── Role.java                      # VIEWER | ANALYST | ADMIN
│   │   │   └── User.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   │   │   ├── ResourceAlreadyExistsException.java
│   │   │   └── ResourceNotFoundException.java
│   │   ├── repository/
│   │   │   ├── FinancialRecordRepository.java # JPQL aggregations
│   │   │   ├── FinancialRecordSpecification.java # Dynamic filtering
│   │   │   └── UserRepository.java
│   │   ├── security/
│   │   │   ├── AppUserDetailsService.java
│   │   │   ├── JwtAuthenticationFilter.java   # OncePerRequestFilter
│   │   │   └── JwtTokenProvider.java          # Token gen/validation
│   │   ├── service/
│   │   │   ├── AuthService.java               # Interface
│   │   │   ├── DashboardService.java
│   │   │   ├── FinancialRecordService.java
│   │   │   ├── UserService.java
│   │   │   └── impl/                          # Implementations
│   │   └── util/
│   │       ├── RecordMapper.java              # MapStruct
│   │       └── UserMapper.java
│   └── resources/
│       ├── application.yml                    # Main config
│       ├── application-test.yml               # Test profile (H2)
│       └── db/migration/
│           ├── V1__create_users_table.sql
│           ├── V2__create_financial_records_table.sql
│           └── V3__seed_admin_user.sql
├── test/
│   └── java/com/finance/dashboard/
│       ├── controller/
│       │   └── AuthControllerIntegrationTest.java
│       └── service/
│           ├── DashboardServiceTest.java
│           ├── FinancialRecordServiceTest.java
│           └── UserServiceTest.java
├── Dockerfile                                 # Multi-stage build
├── docker-compose.yml                         # postgres + api
├── .dockerignore
└── pom.xml
```

---

## Role & Permission Matrix

| Endpoint | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| `POST /auth/login` | ✅ | ✅ | ✅ |
| `GET /dashboard/summary` | ✅ | ✅ | ✅ |
| `GET /dashboard/trends` | ❌ | ✅ | ✅ |
| `GET /records` (list + filter) | ✅ | ✅ | ✅ |
| `GET /records/{id}` | ✅ | ✅ | ✅ |
| `POST /records` | ❌ | ❌ | ✅ |
| `PATCH /records/{id}` | ❌ | ❌ | ✅ |
| `DELETE /records/{id}` | ❌ | ❌ | ✅ |
| `GET /users` | ❌ | ❌ | ✅ |
| `POST /users` | ❌ | ❌ | ✅ |
| `PATCH /users/{id}` | ❌ | ❌ | ✅ |
| `DELETE /users/{id}` | ❌ | ❌ | ✅ |

---

## Quick Start (Docker)

**Prerequisites:** Docker Desktop installed and running.

```bash
# 1. Clone the repository
git clone https://github.com/your-org/finance-dashboard.git
cd finance-dashboard

# 2. Start the full stack (PostgreSQL + API)
docker compose up --build

# 3. Verify API is running
curl http://localhost:8080/actuator/health

# 4. Open Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

The database schema is auto-applied by Flyway on first startup.
Three seed users are created (all use password: `Admin@1234`):

| Email | Role |
|---|---|
| admin@finance-dashboard.com | ADMIN |
| analyst@finance-dashboard.com | ANALYST |
| viewer@finance-dashboard.com | VIEWER |

> ⚠️ These credentials are for development only. Change immediately in production.

---

## Quick Start (Local)

**Prerequisites:** Java 21, Maven 3.9+, PostgreSQL 16 running locally.

```bash
# 1. Create database
psql -U postgres -c "CREATE DATABASE finance_db;"

# 2. Set environment variables (or update application.yml)
export DB_USER=postgres
export DB_PASS=postgres
export JWT_SECRET=local-dev-secret-key-minimum-32-characters

# 3. Run the application
./mvnw spring-boot:run

# 4. Swagger UI
open http://localhost:8080/swagger-ui/index.html
```

---

## API Reference

### Authentication

#### POST `/api/v1/auth/login`
Authenticate and receive a JWT Bearer token.

**Rate limit:** 10 requests/minute per IP.

**Request:**
```json
{
  "email": "admin@finance-dashboard.com",
  "password": "Admin@1234"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "email": "admin@finance-dashboard.com",
  "role": "ADMIN"
}
```

Use the token in subsequent requests:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

### Financial Records

#### GET `/api/v1/records`
List records with optional filtering and pagination.

**Roles:** VIEWER, ANALYST, ADMIN

**Query parameters:**

| Parameter | Type | Description |
|---|---|---|
| `type` | `INCOME` \| `EXPENSE` | Filter by record type |
| `category` | string | Partial match on category name |
| `dateFrom` | `YYYY-MM-DD` | Start of date range |
| `dateTo` | `YYYY-MM-DD` | End of date range |
| `page` | integer | Page number (0-indexed, default 0) |
| `size` | integer | Page size (default 20) |
| `sort` | string | Sort field and direction, e.g. `transactionDate,desc` |

**Example:**
```
GET /api/v1/records?type=EXPENSE&category=office&dateFrom=2025-01-01&dateTo=2025-03-31&sort=amount,desc
```

**Response (200):**
```json
{
  "content": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "amount": 2500.0000,
      "type": "EXPENSE",
      "category": "Office Supplies",
      "transactionDate": "2025-02-15",
      "notes": "Q1 stationery order",
      "createdBy": "1fa85f64-5717-4562-b3fc-2c963f66afa6",
      "createdAt": "2025-02-15T10:30:00Z",
      "updatedAt": "2025-02-15T10:30:00Z"
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

#### POST `/api/v1/records`
Create a financial record.

**Roles:** ADMIN only

**Request:**
```json
{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "transactionDate": "2025-03-01",
  "notes": "March salary"
}
```

**Response (201):** Created record object.

#### PATCH `/api/v1/records/{id}`
Partially update a record. Only provided fields are changed.

**Roles:** ADMIN only

**Request (only update amount):**
```json
{
  "amount": 5500.00
}
```

#### DELETE `/api/v1/records/{id}`
Soft-delete a record (sets `deleted=true`, excluded from future queries).

**Roles:** ADMIN only — **Response:** 204 No Content

---

### Dashboard

#### GET `/api/v1/dashboard/summary`
Returns aggregated KPIs for the dashboard.

**Roles:** VIEWER, ANALYST, ADMIN

**Response (200):**
```json
{
  "totalIncome": 45000.0000,
  "totalExpenses": 28500.0000,
  "netBalance": 16500.0000,
  "incomeByCategory": {
    "Salary": 36000.0000,
    "Freelance": 9000.0000
  },
  "expenseByCategory": {
    "Rent": 12000.0000,
    "Office Supplies": 3500.0000,
    "Utilities": 2000.0000
  },
  "recentActivity": [ ... ]
}
```

#### GET `/api/v1/dashboard/trends?from=2025-01-01&to=2025-12-31`
Monthly income vs expense breakdown.

**Roles:** ANALYST, ADMIN

**Response (200):**
```json
[
  {
    "year": 2025,
    "month": 1,
    "totalIncome": 5000.0000,
    "totalExpenses": 3200.0000,
    "netBalance": 1800.0000
  },
  {
    "year": 2025,
    "month": 2,
    "totalIncome": 4800.0000,
    "totalExpenses": 2900.0000,
    "netBalance": 1900.0000
  }
]
```

---

### Users (Admin only)

#### POST `/api/v1/users`
```json
{
  "name": "Jane Smith",
  "email": "jane@company.com",
  "password": "Secure@123",
  "role": "ANALYST"
}
```

#### GET `/api/v1/users?role=VIEWER&page=0&size=10`

#### PATCH `/api/v1/users/{id}`
```json
{ "role": "ADMIN", "active": true }
```

#### DELETE `/api/v1/users/{id}` — 204, soft-deactivates the user

---

### Error Responses

All errors follow a consistent envelope:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/records",
  "timestamp": "2025-03-01T12:00:00Z",
  "validationErrors": {
    "amount": "must be greater than 0",
    "category": "must not be blank"
  }
}
```

| Status | Condition |
|---|---|
| 400 | Validation failure, malformed request |
| 401 | Missing or invalid JWT token |
| 403 | Valid token but insufficient role |
| 404 | Resource not found or soft-deleted |
| 409 | Duplicate resource (e.g. email already exists) |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error |

---

## Design Decisions & Tradeoffs

### 1. JWT over Session-based Auth
**Chosen:** JWT stored client-side.
**Tradeoff:** Tokens cannot be revoked mid-life. Mitigated by short expiry (1 hour) and a blacklist extension point (Redis JTI store). For a dashboard with low security sensitivity, this is acceptable.

### 2. Soft-Delete over Hard-Delete
**Chosen:** `deleted` boolean flag on records and `active` on users.
**Why:** Financial records must survive their creators. Audit compliance. Accidental deletion recovery.
**Tradeoff:** Queries must always include `WHERE deleted = false`. Mitigated by partial indexes on `deleted = false` in PostgreSQL.

### 3. PATCH Semantics (Partial Updates)
**Chosen:** Only non-null fields in the request body are applied.
**Why:** Callers only need to send what they want to change. Full PUT would require sending the entire object, risking accidental field clearing.

### 4. JPA Specification for Filtering
**Chosen:** `JpaSpecificationExecutor` + `Specification` factory class.
**Why:** 4 optional filter parameters would require 2⁴ = 16 derived method combinations. Specification composes predicates at runtime.
**Tradeoff:** Slightly more verbose than Spring Data query methods. Worth it for maintainability.

### 5. BigDecimal for Money
**Chosen:** `NUMERIC(19, 4)` in DB, `BigDecimal` in Java.
**Why:** `double` and `float` use IEEE 754 binary floating-point: `0.1 + 0.2 = 0.30000000000000004`. This is unacceptable for financial calculations.

### 6. Role stored in JWT
**Chosen:** Role embedded in the JWT claims (`"role": "ADMIN"`).
**Why:** Avoids a DB lookup on every request. Simpler and faster.
**Tradeoff:** Role changes don't take effect until the current token expires. Acceptable for a 1-hour token window. For immediate enforcement, use a shorter expiry or revocation list.

### 7. Interface + Impl Service Pattern
**Chosen:** `UserService` interface + `UserServiceImpl` implementation.
**Why:** Enables easy mocking in unit tests. Allows swapping implementations (e.g., adding caching layer) without changing controllers. Spring's `@Transactional` AOP proxy requires an interface or subclass proxy.

---

## Database Schema

### `users`
```sql
id            UUID         PK
name          VARCHAR(100) NOT NULL
email         VARCHAR(150) UNIQUE NOT NULL
password_hash VARCHAR(255) NOT NULL          -- BCrypt hash
role          VARCHAR(20)  NOT NULL          -- VIEWER | ANALYST | ADMIN
active        BOOLEAN      DEFAULT true      -- Soft-delete flag
created_at    TIMESTAMPTZ
updated_at    TIMESTAMPTZ
```

### `financial_records`
```sql
id               UUID           PK
amount           NUMERIC(19,4)  NOT NULL CHECK (amount > 0)
type             VARCHAR(10)    NOT NULL     -- INCOME | EXPENSE
category         VARCHAR(100)   NOT NULL
transaction_date DATE           NOT NULL     -- No timezone, calendar date
notes            VARCHAR(500)
deleted          BOOLEAN        DEFAULT false -- Soft-delete
created_by       UUID           NOT NULL     -- Soft-ref to users(id)
created_at       TIMESTAMPTZ
updated_at       TIMESTAMPTZ
```

### Indexes
```sql
-- Users
idx_users_email_active   ON users(email)         WHERE active = TRUE
idx_users_role           ON users(role)

-- Financial Records (all partial — only active records)
idx_records_type_active      ON financial_records(type)             WHERE deleted = FALSE
idx_records_category_active  ON financial_records(category, type)   WHERE deleted = FALSE
idx_records_date_active      ON financial_records(transaction_date)  WHERE deleted = FALSE
idx_records_date_type        ON financial_records(transaction_date, type) WHERE deleted = FALSE
```

---

## Security Model

### Authentication Flow
```
1. Client → POST /auth/login {email, password}
2. AuthController → RateLimiterService.resolveLoginBucket(ip).tryConsume(1)
3. AuthenticationManager → AppUserDetailsService.loadUserByUsername(email)
4. BCryptPasswordEncoder.matches(rawPassword, hash)
5. JwtTokenProvider.generateToken(email, role)
6. Response: { accessToken, tokenType: "Bearer", expiresIn: 3600 }
```

### Request Authorization Flow
```
1. JwtAuthenticationFilter extracts "Authorization: Bearer <token>"
2. JwtTokenProvider.isValid(token) — checks signature + expiry
3. Extracts email + role from claims — NO database call
4. Sets SecurityContextHolder with UsernamePasswordAuthenticationToken
5. Spring Security evaluates @PreAuthorize("hasRole('ADMIN')") on the method
6. Access granted or 403 thrown
```

### Password Policy (enforced at creation)
- Minimum 8 characters
- Must contain: uppercase, lowercase, digit, special character (`@$!%*?&`)
- Stored as BCrypt hash (strength 12, ~250ms per check)

---

## Testing

### Run all tests
```bash
./mvnw test
```

### Test strategy

| Test type | Location | What it tests |
|---|---|---|
| Unit | `service/FinancialRecordServiceTest` | Business logic, PATCH semantics, soft-delete |
| Unit | `service/UserServiceTest` | Password hashing, duplicate email detection |
| Unit | `service/DashboardServiceTest` | Net balance calculation, negative balance |
| Integration | `controller/AuthControllerIntegrationTest` | Full login flow with H2, validation errors |

**Unit tests** use Mockito only — no Spring context, millisecond execution.

**Integration tests** use `@SpringBootTest` + H2 in-memory database + `@Transactional` rollback — no external dependencies needed, each test isolated.

---

## Environment Variables

| Variable | Default (dev only) | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL hostname |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `finance_db` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASS` | `postgres` | Database password |
| `JWT_SECRET` | `dev-only-secret...` | HS256 signing key (min 32 chars) — **must override** |
| `JWT_EXPIRY_SECONDS` | `3600` | Token lifetime in seconds |
| `SPRING_PROFILES_ACTIVE` | `default` | Active Spring profile |

---

## Assumptions Made

1. **Single-tenant** — All users share the same financial records dataset. A multi-tenant design would add a `tenantId` to all entities and filter every query by it.

2. **No refresh tokens** — Token expiry requires re-login. Acceptable for a 1-hour window. Refresh tokens would add complexity (rotation, revocation storage) not required by the spec.

3. **Admin creates records** — Only ADMIN role can create/modify financial records. The `createdBy` field captures which admin created a given record for audit purposes.

4. **Soft-delete is permanent** — There is no "restore" endpoint. An admin can re-activate users via PATCH. Deleted records cannot currently be un-deleted (a restore endpoint is trivial to add).

5. **Categories are free-text** — No `categories` table. Normalising categories would allow renaming, but adds complexity. The current approach is simpler and sufficient for the dashboard use case.

6. **No pagination on dashboard** — `/dashboard/summary` returns all-time totals. For very large datasets, a date-range parameter and caching layer would be the next step.

7. **In-memory rate limiter** — Bucket4j uses a `ConcurrentHashMap`. For multi-instance deployments behind a load balancer, replace with Bucket4j + Redis so limits are shared across nodes.

---

## Production Checklist

- [ ] Rotate `JWT_SECRET` to a cryptographically random 256-bit value
- [ ] Change all seed user passwords (or disable V3 migration entirely)
- [ ] Set `logging.level.com.finance.dashboard=INFO` (not DEBUG)
- [ ] Configure HTTPS (TLS termination at load balancer or Nginx)
- [ ] Replace in-memory rate limiter with Redis-backed Bucket4j
- [ ] Add `@Cacheable` on `DashboardService.getSummary()` with Redis
- [ ] Set up database connection via a secrets manager (AWS Secrets Manager, Vault)
- [ ] Configure HikariCP pool size based on `max_connections` in PostgreSQL
- [ ] Enable Spring Boot Actuator metrics → Prometheus → Grafana
- [ ] Set resource limits in docker-compose / Kubernetes deployment
- [ ] Add request logging (MDC with correlation IDs for distributed tracing)
