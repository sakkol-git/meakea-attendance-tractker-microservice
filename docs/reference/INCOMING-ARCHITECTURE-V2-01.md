# Approved architecture detail 01

> **Decision:** This is part of the latest user-approved final system design. It is authoritative design intent, not evidence of implemented behavior. Read the [canonical Architecture index](../Architecture.md) for supersession, security limits and deployment constraints. Cloud chapters remain the approved future topology; the latest user decision defers their execution beyond this twelve-week local finish line.

> **Note:** Event-driven, multi-tenant HR platform: attendance, leave, requests, approvals, notifications and reporting.
> **Note:** Status: **design document**. Nothing here has been load-tested; items marked **[UNVERIFIED]** depend on library versions or cloud behaviour that must be confirmed during the build.
> **Note:** Team: 2 people (backend + web + infra / mobile). Timeline: 12 weeks.

## Table of contents

1. Purpose, scope and quality goals
1. Architectural principles
1. System context
1. Container view
1. Technology stack
1. Repository and module structure
1. Multi-tenancy
1. Security architecture
1. Service catalogue
1. Event-driven backbone
1. Attendance domain (deep dive)
1. Leave domain
1. Request workflow domain
1. Notification pipeline
1. Reporting and read models
1. ML confidence service
1. Data architecture
1. API architecture
1. Web client (Nuxt)
1. Mobile client (Flutter)
1. Infrastructure and deployment
1. CI/CD
1. Observability
1. Resilience and failure handling
1. Performance and capacity
1. Testing strategy
1. Privacy and compliance
1. Architecture decision records
1. Risks and open questions
1. Delivery plan (12 weeks)
1. Glossary



## 1. Purpose, scope and quality goals

### 1.1 Purpose

HRFlow lets a company record who worked when, who is away, and what staff have asked for, with as little effort as possible from employees. The distinctive idea is **"tap first, infer if forgotten"**: the app reminds the employee to check in with one tap; if they still forget, passive signals plus a small ML model infer the attendance and ask for next-day confirmation.

### 1.2 In scope

| Area                      | Capability                                                                                                                                                 |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Identity and organisation | Tenants, users, roles, employees, departments, positions, work sites, shifts, holidays                                                                     |
| Attendance                | One-tap check-in/out, geofence and Wi-Fi nudges, evidence log, inferred sessions, corrections, daily status (present, late, absent, leave early, on leave) |
| Leave                     | Leave types, balances, requests, approvals, calendar, special leave                                                                                        |
| Requests                  | Material and general requests with a configurable approval chain                                                                                           |
| Notifications             | Push, email, in-app inbox, per-user preferences                                                                                                            |
| Reporting                 | Dashboards, summaries, async exports                                                                                                                       |
| ML                        | Presence-confidence scoring with explainable reasons                                                                                                       |

### 1.3 Out of scope (first release)

- Payroll calculation and payslips.
- Face recognition or any biometric identification of employees by a camera.
- Multi-region active-active deployment.
- Native tablet kiosk app.
- Third-party HRIS integrations.

### 1.4 Quality goals (ordered by priority)

| #   | Quality attribute                 | Target                                                                 | How the architecture serves it                                                                |
| --- | --------------------------------- | ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| 1   | Correctness of attendance records | No lost or duplicated check-ins; every automatic decision explainable  | Append-only evidence, outbox, idempotent consumers, reasons stored with each inferred session |
| 2   | Loose coupling                    | Any service can be deployed, restarted or fail without breaking others | Events only, no service-to-service HTTP, local read models                                    |
| 3   | Responsiveness                    | Check-in API p95 < 300 ms; read models updated < 1 s after an event    | Command path touches one service and one database; async fan-out                              |
| 4   | Tenant isolation                  | One company never sees another's data                                  | `tenant_id` on every row, gateway-enforced headers, tenant in event key                       |
| 5   | Low cost                          | Runs on a student budget                                               | Single Postgres instance with schema per service, single-broker Kafka for dev, small ML model |
| 6   | Operability by two people         | Setup reproducible with one command                                    | Docker Compose locally, Terraform for cloud, uniform service template                         |



## 2. Architectural principles

1. **Events are facts.** Past-tense, immutable, versioned (`AttendanceCheckedIn`, never `CheckInRequested`).
1. **Each service owns its data.** No shared tables, no cross-schema joins, no cross-service foreign keys.
1. **No service-to-service HTTP.** Services learn about each other only through Kafka events and keep a local read model of what they need.
1. **A consumer failure never fails a user request.** Writes commit locally first; downstream work is asynchronous.
1. **Atomic publish via the outbox.** The business row and the event row commit in one transaction; a relay publishes later.
1. **Idempotent consumers.** An inbox table keyed by `(consumer, eventId)` makes redelivery harmless.
1. **Never delete attendance history.** Corrections are appended; the current state is derived by precedence rules.
1. **Manual always wins.** A human decision overrides any inference.
1. **Contracts first.** Avro schemas in `contracts/events` are the source of truth; code is generated from them.
1. **Explainable automation.** Every automatic decision stores the evidence and reasons that produced it.
1. **Least privilege.** The gateway and every Java service hold only JWT verification material and independently validate the bearer token; the signing key remains only with the issuer. ADR 010 supersedes the earlier gateway-injected identity-header choice.
1. **Boring technology where possible.** One database engine, one broker, one language per tier.



## 3. System context

```text
                         ┌──────────────────────┐
   Employee ───────────► │                      │ ───► Push provider (FCM/APNs)
   (Flutter app)         │                      │
                         │        HRFlow        │ ───► Email provider (SES/SMTP)
   Manager / HR / Owner ►│                      │
   (Nuxt web app)        │                      │ ───► Object storage (S3)
                         └──────────┬───────────┘
                                    │
   Office signals ──────────────────┘
   (Wi-Fi BSSID, BLE beacons, GPS geofence, phone sensors)
```


| Actor                | Goal                                                                   | Main interfaces   |
| -------------------- | ---------------------------------------------------------------------- | ----------------- |
| Employee             | Check in/out, request leave, request materials, see status             | Flutter app       |
| Manager              | Approve leave and requests, see team attendance                        | Nuxt web, Flutter |
| HR admin             | Configure org, shifts, holidays, policies; fix attendance; run reports | Nuxt web          |
| Owner                | Company settings, audit log                                            | Nuxt web          |
| Platform admin       | Create tenants                                                         | Internal endpoint |
| Push/email providers | Deliver notifications                                                  | Outbound only     |

External systems are deliberately few: HRFlow is the system of record.



## 4. Container view

```text
┌───────────────┐      ┌────────────────┐
│ Flutter app   │      │ Nuxt web app   │
└──────┬────────┘      └───────┬────────┘
       │  HTTPS + JWT          │
       └───────────┬───────────┘
                   ▼
        ┌─────────────────────┐
        │ api-gateway :8080   │  validates JWT (public key), strips and re-injects
        │ (Spring Cloud GW)   │  X-Tenant-Id / X-Employee-Id / X-Roles, rate-limits
        └──┬───┬───┬───┬───┬──┘
           │   │   │   │   │            (HTTP, client commands and queries only)
   ┌───────┘   │   │   │   └────────────────────┐
   ▼           ▼   ▼   ▼                        ▼
┌────────┐ ┌────────┐ ┌───────┐ ┌─────────┐ ┌────────────┐ ┌───────────┐
│identity│ │attend. │ │leave  │ │request  │ │notification│ │reporting  │
│-org    │ │ :8102  │ │ :8103 │ │ :8104   │ │ :8105      │ │ :8106     │
│ :8101  │ │        │ │       │ │         │ │            │ │           │
└───┬────┘ └───┬────┘ └──┬────┘ └────┬────┘ └─────┬──────┘ └─────┬─────┘
    │          │         │           │            │              │
    └──────────┴─────────┴───────────┴────────────┴──────────────┘
                         │  publish / consume (Avro)
                         ▼
                ┌────────────────────┐        ┌──────────────────┐
                │ Kafka (KRaft) 9092 │◄──────►│ Schema Registry  │
                └─────────▲──────────┘        │      8081        │
                          │                   └──────────────────┘
                ┌─────────┴──────────┐
                │ confidence-service │  scores evidence, publishes confidence
                │ (Python, :8107)    │
                └────────────────────┘

        ┌───────────────────────────────────────────────┐
        │ PostgreSQL 5432: one database, schema/service │
        │ identity | attendance | leave | request |     │
        │ notification | reporting                      │
        └───────────────────────────────────────────────┘
        S3: attachments and exports        Kafka UI :8090 (dev only)
```


### 4.1 Container responsibilities

| Container          | Responsibility                                                                | Talks to                               |
| ------------------ | ----------------------------------------------------------------------------- | -------------------------------------- |
| api-gateway        | Single entry point, authentication, routing, rate limiting, CORS, request IDs | Clients, all services (HTTP)           |
| identity-org       | Login, JWT signing, tenants, employees, org structure, shifts, holidays       | Postgres, Kafka                        |
| attendance         | Check-ins, evidence, inference orchestration, daily status                    | Postgres, Kafka                        |
| leave              | Leave types, balances, requests                                               | Postgres, Kafka, S3                    |
| request            | Material and general requests, approval chain                                 | Postgres, Kafka, S3                    |
| notification       | Turn events into messages and deliver them                                    | Postgres, Kafka, FCM/SES               |
| reporting          | Read-optimised projections and exports                                        | Postgres, Kafka, S3                    |
| confidence-service | ML scoring of presence                                                        | Kafka (worker), HTTP (demo/debug only) |
| Kafka              | Durable event log                                                             | all services                           |
| Schema Registry    | Avro schema storage and compatibility checks                                  | Kafka clients                          |

### 4.2 The only synchronous paths

| Path                                 | Why synchronous                                     |
| ------------------------------------ | --------------------------------------------------- |
| Client → gateway → service           | A person is waiting for a response                  |
| Service → Schema Registry (cached)   | Schema lookup at serialise/deserialise time         |
| Service → S3 (presigned URLs)        | The client uploads directly; the service only signs |
| Gateway → identity-org JWKS (cached) | Fetch the public key at startup and on rotation     |



## 5. Technology stack

| Layer              | Choice                                               | Reason                                     | Note                                                                                                 |
| ------------------ | ---------------------------------------------------- | ------------------------------------------ | ---------------------------------------------------------------------------------------------------- |
| Language / runtime | Java 21                                              | Virtual threads, long-term support         | — |
| Backend framework  | Spring Boot 4.1.x                                    | Matches the Initializr default             | Jackson 3 packages (`tools.jackson.*`); some starters renamed **[UNVERIFIED against every library]** |
| Gateway            | Spring Cloud Gateway (reactive)                      | Native JWT and filter support              | Config prefix `spring.cloud.gateway.server.webflux.routes`                                           |
| Messaging          | Apache Kafka, KRaft mode                             | No ZooKeeper; simpler to run               | Single broker in dev                                                                                 |
| Serialisation      | Avro + Confluent Schema Registry                     | Enforced contracts, compact payloads       | Confluent serializer 8.1.0 chosen, compatibility with Boot 4 **[UNVERIFIED]**                        |
| Database           | PostgreSQL                                           | Mature, JSONB, partitioning, `SKIP LOCKED` | One instance, schema per service                                                                     |
| Migrations         | Flyway                                               | Versioned SQL per service                  | `flyway-database-postgresql` module needed                                                           |
| Data access        | Spring JDBC (`JdbcClient`/`JdbcTemplate`)            | Explicit SQL, outbox control               | JPA avoided on purpose                                                                               |
| Web                | Nuxt (Vue 3, TypeScript), npm                        | Chosen over React                          | — |
| Mobile             | Flutter                                              | Owned by the second team member            | — |
| ML                 | Python, scikit-learn, FastAPI                        | Small, explainable model                   | Logistic regression vs gradient boosting                                                             |
| Containers         | Docker, Kubernetes                                   | Required by the brief                      | — |
| Cloud              | AWS, Terraform                                       | Required by the brief                      | — |
| CI/CD              | GitHub Actions                                       | Free for students, integrates with GitHub  | — |
| Observability      | Micrometer, Prometheus, Grafana, OpenTelemetry, Loki | Open standards                             | — |



## 6. Repository and module structure

```text
hrflow/
├── pom.xml                         # hrflow-parent: versions, repositories, module list
├── contracts/events/*.avsc         # Avro schemas (source of truth)
├── libs/
│   ├── hrflow-contracts/           # generates Java classes from the .avsc files
│   └── hrflow-event-starter/       # outbox, inbox, relay, Avro Kafka auto-configuration
├── services/
│   ├── api-gateway/  identity-org/  attendance/  leave/
│   └── request/  notification/  reporting/
├── ml/confidence-service/          # Python: training + scoring
├── web/                            # Nuxt (npm)
├── mobile/                         # Flutter (or a separate repository)
├── infra/
│   ├── docker/                     # docker-compose.yaml, postgres-init.sql
│   ├── scripts/                    # topic creation, schema registration
│   ├── k8s/                        # manifests / kustomize overlays
│   └── terraform/                  # AWS modules
└── docs/adr/                       # architecture decision records
```


### 6.1 Dependency direction

```text
services ──► hrflow-event-starter ──► hrflow-contracts ──► avro
```


Rules enforced in review:

- A service never depends on another service.
- `hrflow-contracts` has no Spring dependency.
- The gateway depends on neither library (it has no database or Kafka usage).
- Versions live only in the root pom (`dependencyManagement`); modules declare no versions.

### 6.2 Internal layout of every service

```text
com.hrflow.<service>
├── api/            # REST controllers, request/response DTOs, validation
├── application/    # use cases (command handlers, query handlers), transaction boundaries
├── domain/         # entities, value objects, state machines, domain rules (no Spring)
├── infrastructure/
│   ├── persistence/   # JDBC repositories
│   ├── messaging/     # event consumers, event mappers
│   └── config/
└── Application.java
```


| Layer            | May depend on                      | Must not depend on        |
| ---------------- | ---------------------------------- | ------------------------- |
| `api`            | `application`                      | `infrastructure` directly |
| `application`    | `domain`, repository interfaces    | HTTP types                |
| `domain`         | nothing framework-specific         | Spring, JDBC, Kafka       |
| `infrastructure` | `domain`, `application` interfaces | `api`                     |

Keeping `domain` framework-free means the attendance precedence rules and leave balance maths can be unit-tested in milliseconds.



## 7. Multi-tenancy

### 7.1 Model

One **shared database, shared schema per service, `tenant_id` column on every table** (except the `tenant` table). This is the cheapest model that still isolates data logically; a schema-per-tenant or database-per-tenant model would multiply migrations and connections for a two-person team.

### 7.2 Enforcement points

| Point        | Mechanism                                                                                                                                   |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| Gateway      | Strips any client-sent `X-Tenant-Id`, `X-Employee-Id`, `X-Roles`; sets them from validated JWT claims                                       |
| Controller   | Reads tenant from the header into a request-scoped `TenantContext`; never from the body                                                     |
| Repository   | Every query includes `tenant_id = :tenantId`; a base class makes it impossible to forget                                                    |
| Database     | Optional second line of defence: PostgreSQL row-level security policy using `set_config('app.tenant_id', …)` per transaction **[OPTIONAL]** |
| Kafka        | `tenantId` is part of the message key and of the event envelope; consumers verify it before applying                                        |
| Logs/metrics | `tenantId` tag on every log line and a bounded-cardinality metric label                                                                     |

### 7.3 Tenant-aware uniqueness

Natural keys are unique **per tenant**: `unique (tenant_id, employee_no)`, `unique (tenant_id, date)` for holidays, `unique (tenant_id, email)` for users.

### 7.4 Noisy neighbour control

- Gateway rate limit per tenant and per user.
- Kafka partition key `tenantId:employeeId` spreads one tenant across partitions while keeping one employee ordered.
- Heavy exports run as async jobs with a per-tenant concurrency limit.



## 8. Security architecture

### 8.1 Authentication flow

```text
Client ──POST /auth/login──► gateway ──► identity-org
                                          │ verify password (argon2id/bcrypt)
                                          │ issue access JWT (RS256, 15 min)
                                          │ issue refresh token (random, hashed in DB, 14 days)
Client ◄───────── tokens ─────────────────┘

Client ──GET /attendance/today  Authorization: Bearer <JWT>──► gateway
          gateway: verify signature with PUBLIC key, check exp/iss/aud
          gateway: remove client X-* headers, add X-Tenant-Id / X-Employee-Id / X-Roles
          ──► attendance (trusts the headers because only the gateway can reach it)
```


### 8.2 Key management

| Item           | Rule                                                                                                     |
| -------------- | -------------------------------------------------------------------------------------------------------- |
| Signing        | RS256. Private key only in identity-org (secret store); public key published at `/.well-known/jwks.json` |
| Rotation       | Keys carry a `kid`; JWKS lists current and previous key during an overlap window                         |
| Gateway        | Caches JWKS, refreshes on unknown `kid` with a cooldown                                                  |
| Refresh tokens | Stored as hashes, rotated on every use, revoked on logout and on reuse detection                         |

### 8.3 JWT claims

| Claim                                    | Meaning                                           |
| ---------------------------------------- | ------------------------------------------------- |
| `sub`                                    | user id                                           |
| `tid`                                    | tenant id                                         |
| `eid`                                    | employee id                                       |
| `roles`                                  | array: `OWNER`, `HR_ADMIN`, `MANAGER`, `EMPLOYEE` |
| `iss`, `aud`, `exp`, `iat`, `jti`, `kid` | standard                                          |

### 8.4 Authorisation model

Role-based, with three data-scope rules applied in the service layer:

| Scope  | Rule                                                                             |
| ------ | -------------------------------------------------------------------------------- |
| Self   | An employee reads and changes only their own records                             |
| Team   | A manager reads records of employees whose `manager_id` equals their employee id |
| Tenant | HR admin and owner act across the tenant                                         |

Roles are checked at the gateway for coarse routes (for example `/reports/**` requires HR_ADMIN or MANAGER) and again in the service for scope, so a bypassed gateway does not expose data.

### 8.5 Network trust boundary

- Services listen only on the cluster network; the ingress exposes only the gateway.
- Kubernetes `NetworkPolicy`: services accept ingress only from the gateway namespace and from the monitoring namespace.
- Because services trust headers, **network isolation is a security control, not an optimisation**.
- Service-to-Kafka and service-to-Postgres use credentials from the secret store, one DB role per service limited to its schema.

### 8.6 Other controls

| Threat                 | Control                                                                                           |
| ---------------------- | ------------------------------------------------------------------------------------------------- |
| Credential stuffing    | Rate limit on `/auth/login`, lockout with backoff, uniform error message                          |
| Spoofed GPS            | `mock_location_flag` forces confidence 0; device binding; evidence kept for review                |
| Phone left at office   | Movement and interaction features lower the ML score; a tap is never replaced silently            |
| Replay of a check-in   | `Idempotency-Key` header; unique constraint on `(tenant_id, employee_id, work_date, check_in_at)` |
| Injection              | Parameterised SQL only; bean validation on all DTOs                                               |
| Mass assignment        | Dedicated request DTOs; no entity binding                                                         |
| Sensitive data at rest | Postgres and S3 encryption enabled in AWS; no secrets in git                                      |
| Audit                  | Append-only `audit_log` for admin changes; attendance corrections keep who and why                |
| Dependency risk        | Dependabot and a CI dependency scan                                                               |
