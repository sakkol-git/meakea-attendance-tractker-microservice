# Architecture reference 01

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## 1. Document control and reading guide

- Revision: 2026-10-23.
- Status: design baseline for implementation; not a description of a production system.
- Audience: backend, web, mobile, operations, QA, security reviewers, and maintainers.
- Product: HRFlow is a placeholder name for a multi-tenant HR platform.
- Delivery constraint: twelve weeks, contributor A on backend/web/infrastructure and B on Flutter.
- Authoritative contracts: [API, persistence, and events](../REST-API-DATABASE-DESIGN.md).
- Authoritative delivery sequence: [execution plan](../PLAN.md).
- Historical reconnaissance: [project analysis](../PROJECT-ANALYSIS.md), a dated snapshot rather than the current contract.

### 1.1 Status vocabulary

| Label | Meaning |
|---|---|
| Implemented | Verified source or configuration exists; runtime readiness is stated separately |
| Partial | Some code/configuration exists but the capability is incomplete |
| Planned | Existing project documents establish intended behavior; implementation is absent |
| Proposed | Recommendation introduced by this specification; not an approved product decision |
| Unknown | Evidence or an external decision is missing |

Sections describing runtime behavior below are **planned design**, unless explicitly labeled implemented. Examples, limits, schemas, and new endpoint extensions are **proposed contracts** until reviewed and implemented. An ADR marked Proposed records a recommendation, not approval.

### 1.2 Evidence and conflict resolution

Current behavior is established by source/configuration, then migrations/tests. Intended behavior comes from requirements and the pre-existing plan/API draft, then recorded ADRs. Accidental scaffold behavior does not silently replace a business requirement. This document records discrepancies and a disposition rather than claiming they are fixed. No application dependencies, infrastructure definitions, or business code were changed for this specification.

### 1.3 Repository baseline

| Component | Evidence | Status |
|---|---|---|
| Seven Java services | `services/*/src/main/java` contains application entry points only | Partial |
| Root reactor | `pom.xml`, seven service modules, two libraries | Implemented configuration |
| Event library | `libs/hrflow-event-starter` dependencies and Kafka YAML, no Java classes | Partial |
| Event contracts | One Avro check-in record in `libs/hrflow-contracts/events` | Partial |
| Attendance persistence | Datasource/JPA/Flyway YAML, no entities or migrations | Partial |
| Business REST APIs | No controllers/DTOs/security policies found | Planned |
| Nuxt web | `web/app/app.vue` renders Nuxt welcome | Partial |
| Flutter | No source found in repository | Planned; external repository unknown |
| Confidence scoring | FastAPI, rules, training, model artifact, five tests | Implemented standalone prototype |
| Local dependencies | PostgreSQL/Kafka/UI/Registry Compose | Partial; configuration defects remain |
| Deployment/CI | No Terraform, Kubernetes, Helm, or workflow files | Planned |
| API/design contracts | Markdown specification in this suite | Planned; not generated implementation |

### 1.4 Verified discrepancies and disposition

| ID | Finding | Disposition; implementation task |
|---|---|---|
| D01 | Root Boot parent 4.1.1; services independently use 4.0.8 | Align parent inheritance/version in week 1; no upgrade prescribed |
| D02 | Plan previously said Boot 3 | Source establishes current Boot 4 configuration; remove stale intended-version claim |
| D03 | React plan; Nuxt 4.6.1/Vue source | Preserve scaffold; propose Nuxt for MVP, decision O01 |
| D04 | Attendance port 8082; API draft 8102 | 8101–8106 remain target service ports; config correction pending |
| D05 | API draft schema `identity`; init creates `identity_org` | Canonical target is `identity_org`, consistent with bootstrap |
| D06 | Six YAML resources contain `spring.application.name=...` | Convert to YAML mappings before service startup |
| D07 | PostgreSQL SQL mount repeats `infra/docker` relative path | Correct mount relative to Compose file before fresh initialization |
| D08 | Registry points to `kafka:19092`; listener is 29092 | Use declared internal listener; verify readiness |
| D09 | Avro plugin points at absent root `contracts/events` | Correct to library's `events` directory before generation |
| D10 | No service imports shared libraries | Wire starter only after its auto-configuration and tests exist |
| D11 | Original “all writes return 202” rule | Synchronous durable writes use 200/201; exports/year-end jobs use 202 |
| D12 | Universal employee Kafka key | Use aggregate keys for shifts, holidays, requests; see section 13 |
| D13 | “Never delete” versus privacy retention | No normal business deletion of evidence; controlled retention erasure remains required |
| D14 | Manual always wins versus holiday/leave precedence | Separate evidence from classification; unresolved policy O02 blocks automatic conflict resolution |
| D15 | Reporting balance endpoint lacks source event | Propose `leave.balance.v1`; no cross-schema query fallback |
| D16 | Notification called command-free but has preferences/device/read commands | It owns notification-specific commands, not HR workflow decisions |

The original empty `Achitecture.MD` was corrected to `Architecture.md` to match the requested canonical deliverable. The API/database draft already had meaningful coverage at this revision; it was preserved and normalized.

## 2. System overview and domain

### 2.1 Goals and non-goals

Employees use mobile self-service to record attendance and submit requests. Managers review direct-report attendance and approve assigned decisions. HR configures organization data, policies, corrections, and exports. The design favors a durable check-in even when notifications are unavailable. Auditability, tenant separation, and recoverable asynchronous work are core requirements. Payroll, recruitment, continuous GPS tracking, authoritative warehouse inventory, and facial identification are outside MVP scope. Camera evidence remains an optional future proposal; its presence in an old enum does not establish permission to collect it.

```text
flowchart LR
  E[Employee] --> M[Flutter mobile - planned]
  H[HR and manager] --> W[Web - Nuxt scaffold]
  M --> G[API gateway - partial]
  W --> G
  G --> S[Domain services - scaffolds]
  S --> P[(PostgreSQL service schemas - planned tables)]
  S --> K[Kafka - local configuration]
  K --> R[Notification and reporting - planned consumers]
  R --> X[FCM and email - planned]
```


### 2.2 Actors and authorization scope

| Actor | Responsibilities | Resource scope |
|---|---|---|
| EMPLOYEE | Attendance, own leave/requests/preferences | Own linked employee only |
| MANAGER | Team views and assigned approvals | Current direct reports; no implicit tenant-wide access |
| HR_ADMIN | Tenant organization, policies, authorized corrections/reports | Own tenant |
| OWNER | Tenant settings and audit review | Own tenant; HR capabilities require explicit HR grant |
| Platform operator | Provision/suspend tenants, key/deployment operations | Separate control-plane credential; not an OWNER role |
| Background worker | Cutoff, relay, export, provider delivery | Explicit tenant and job scope |

A role is necessary but insufficient: every object operation also checks ownership, current manager relationship, assigned step, and tenant. No approver may approve their own request; escalation goes to a different HR administrator. The platform provisioning API is not available to ordinary tenant access tokens.

### 2.3 Domain vocabulary and lifecycle

A **user** is a login identity; an **employee** is an employment record, optionally linked to one user in the tenant. A **work site** defines approved nudge/evidence context, not proof of human presence. A **shift** is a local-time schedule; an **assignment** applies it over effective dates. A **work date** is the local date on which the assigned shift starts. **Evidence** records an observation with provenance; it is not an attendance conclusion. A **session** records an interpreted working interval and source. A **correction** requests or records a new interpretation without deleting prior evidence. A **daily status** is a recalculable projection of calendar, leave, and effective sessions. A **revision** is a monotonic aggregate version, not a wall-clock timestamp.

Employee lifecycle: provision → assign schedule → activate → update/transfer → terminate. Termination revokes refresh sessions, prevents new self-service commands, and publishes a full employee snapshot. Downstream historical records remain; future cutoff jobs exclude terminated employees from their effective termination date. Tenant lifecycle is ACTIVE → SUSPENDED → controlled closure; suspension blocks normal login and mutations. Deletion is an administrative retention workflow, not a cascade that silently removes HR history.

Attendance lifecycle: evidence → open session → checkout or missing-checkout review → effective revision → daily recomputation. Inference adds a pending-confirmation interpretation; rejection excludes it from worked-time totals. Leave lifecycle: PENDING → APPROVED/REJECTED/CANCELLED; APPROVED may become NEEDS_REVIEW after attendance conflict. General request lifecycle: DRAFT → PENDING → APPROVED/REJECTED/CANCELLED → FULFILLED when applicable. Notification lifecycle: accepted event → durable inbox/notification → delivery attempts → provider acceptance/failure. Reporting lifecycle: consume fact → guarded upsert → freshness watermark → query/export → retention cleanup.

### 2.4 Business invariants

- All tenant business objects and commands have one trusted tenant context.
- One employee has at most one open effective session; multiple closed sessions per work date are permitted.
- A session's checkout cannot precede check-in.
- The client cannot declare itself an HR actor or select a different employee for self-service.
- Leave availability is entitlement plus carry-over minus used minus pending reservation.
- Balance reservations and request transitions commit together in the leave database.
- Approval transitions check current state and version in the same transaction.
- Business mutation and publication intent commit atomically in the owner database.
- Historical evidence and decision actions are append-only in normal application flows.
- Projection freshness is visible; asynchronous projections do not pretend to be immediately authoritative.

## 3. Quality attributes and acceptance targets

These are proposed acceptance targets, not production guarantees. No uptime commitment or capacity guarantee has been established.

| Attribute | Target or invariant | Measurement and validation |
|---|---|---|
| Check-in latency | p95 <150 ms | Gateway receive to response complete; same-region warm services, 100 concurrent clients; include DB/outbox commit, exclude device network |
| Tail latency | Record p99, no numerical promise yet | Same histogram/window as p95; retain error and timeout samples separately |
| Dashboard propagation | p95 <2 s | Source commit time to visible projection/SSE render; clocks synchronized; report consumer and UI portions |
| Load | 100 concurrent check-ins, zero unexplained errors | Distinct employees plus duplicate-key cases; dataset and hardware recorded |
| Isolation | Zero cross-tenant disclosure or mutation | Two-tenant API/repository/event/export fixtures, ID substitution and role tests |
| Reliability | No acknowledged command lost after tested restart | DB durability + outbox/inbox crash tests; compare resource/event IDs |
| Auditability | Every privileged decision has actor/reason/time | Contract and transaction assertions for approvals/corrections/adjustments |
| Recovery | Recover stopped notification consumer | Stop, check in, restart, observe one in-app notification and caught-up offsets |
| Repeatability | Fresh checkout can reproduce demo | Pinned toolchain, migrations, documented setup and seeded non-sensitive data |
| Cost | Original `$`30 budget alert retained as proposal | Alert threshold is not a cost estimate; forecast compute/database/broker/egress before deployment |
| Adoption | Manual check-in rate ≥70% | Pilot denominator = eligible scheduled days, excluding leave/holidays |
| Inference | Recover ≥90% forgotten days within ±10 min | Separate labeled pilot evaluation; not a launch claim or synthetic metric |
| Battery | <3% added workday battery use | Compare permitted nudge mode with baseline on representative Android/iOS devices |

Availability is tracked initially as successful eligible requests divided by eligible requests in the pilot window. Exclude deliberate maintenance only when disclosed; do not remove dependency failures from error rates. Define a production SLO and error budget after workload and operational ownership are known. Failure of a performance target triggers profiling and scope review, not removal of correctness checks.

## 4. Architectural style and service boundaries

### 4.1 Decision baseline

The existing plan establishes domain-oriented services, Kafka-only business integration, local projections, and service-owned schemas. These are intended architecture, not implemented runtime guarantees. [ADR-001](../adr/001-SERVICE-AND-EVENT-BOUNDARIES.md) records that baseline and proposed delivery safeguards. One physical PostgreSQL instance is an MVP cost simplification; schemas are not independent failure domains.

| Service | Owns | Must not own |
|---|---|---|
| api-gateway | Routes, edge authentication, limits, correlation | Leave balances, attendance decisions |
| identity-org | Tenant/user/org master data and token lifecycle | Other service tables |
| attendance | Evidence, sessions, corrections, statuses, cutoff | Authoritative identity or leave balances |
| leave | Policies, balances, requests, ledger | Attendance sessions |
| request | General requests, advisory materials, approval steps | Leave policies or inventory guarantees |
| notification | Preferences, inbox, device tokens, delivery | Business approval state |
| reporting | Derived facts, summaries, exports | Transactional HR truth |

### 4.2 Component view

```text
flowchart TB
  G[Gateway] --> I[identity-org]
  G --> A[attendance]
  G --> L[leave]
  G --> Q[request]
  G --> N[notification]
  G --> R[reporting]
  I --> O[Outbox and inbox library - planned]
  A --> O
  L --> O
  Q --> O
  O --> K[Kafka]
  K --> N
  K --> R
```


The shared starter contains infrastructure patterns, not shared domain entities or repositories. Each service performs its own authorization and transaction management. Gateway routing to domain APIs is an allowed synchronous client path. Business service-to-service HTTP and cross-service SQL joins are prohibited by the intended baseline. Infrastructure protocols to PostgreSQL/Kafka/storage and external provider calls are allowed. JWT signature verification uses cached JWKS; key discovery is a documented security-control-plane exception, not a synchronous business-data dependency.

### 4.3 Dependency view

```text
flowchart LR
  I[identity-org] -->|org snapshots| K[Kafka]
  L[leave] -->|leave requests and balances| K
  A[attendance] -->|sessions statuses conflicts| K
  Q[request] -->|workflow snapshots| K
  K -->|org and leave| A
  K -->|org and conflicts| L
  K -->|org| Q
  K -->|business facts| N[notification]
  K -->|facts and dimensions| R[reporting]
```


Local projections trade synchronous coupling for freshness and reconciliation work. The user-facing owner API returns authoritative local state immediately after commit. Read models may lag and must expose `asOf` and `projectionLagMs`. If org data needed for a command is missing, return retryable `REFERENCE_NOT_READY`; do not fabricate a shift.

## 5. Runtime, trust, and deployment views

### 5.1 Container and data ownership view

```text
flowchart TB
  subgraph Clients
    W[Web]
    M[Flutter]
  end
  subgraph Application_network_planned
    G[Gateway]
    I[identity-org]
    A[attendance]
    L[leave]
    Q[request]
    N[notification]
    R[reporting]
    K[Kafka]
  end
  subgraph PostgreSQL_instance
    DI[(identity_org)]
    DA[(attendance)]
    DL[(leave)]
    DQ[(request)]
    DN[(notification)]
    DR[(reporting)]
  end
  W --> G
  M --> G
  G --> I & A & L & Q & N & R
  I --> DI
  A --> DA
  L --> DL
  Q --> DQ
  N --> DN
  R --> DR
  I & A & L & Q --> K
  K --> A & L & Q & N & R
```


### 5.2 Trust boundaries

```text
flowchart LR
  U[Untrusted client and supplied headers] -->|TLS| E[Public edge]
  E -->|validated bearer token| G[Gateway]
  G -->|token forwarded and checked again| S[Private service network]
  S -->|service DB role| D[(Private PostgreSQL)]
  S -->|topic ACL identity| K[Private Kafka]
  S -->|restricted provider credential| P[External push email storage]
  O[Privileged operator] -->|separate authenticated channel| C[Control plane]
```


Private networking reduces exposure but does not replace token verification or object authorization. Client-supplied tenant, employee, roles, and forwarded identity headers are removed at the edge. Background jobs derive tenant from a stored job or verified event, never from a thread-local leftover.

### 5.3 Local development view

```text
flowchart LR
  H[Host processes - services and web launched separately] -->|localhost 5432| P[Compose PostgreSQL]
  H -->|localhost 9092| K[Compose Kafka]
  H -->|localhost 8081| S[Compose Registry]
  S -->|kafka 29092 target correction| K
  U[Compose Kafka UI on 8090] -->|kafka 29092| K
  C[Confidence API on 8107 - standalone] -.-> H
```


Compose currently includes no application containers and no automatic topic initialization. The broken SQL mount and Registry listener must be fixed before treating it as reproducible. The topic script creates six partitions per current topic, replication factor one, and compacted org topics. Local plaintext credentials and listeners are local-only defaults.

### 5.4 Production proposal

```text
flowchart TB
  B[Browser and mobile] --> T[TLS ingress]
  T --> G[Gateway containers]
  G --> S[Domain containers]
  S --> D[(Private PostgreSQL with backups)]
  S --> K[Private Kafka persistent storage]
  S --> O[Private object storage]
  S --> P[Push and email providers]
  CI[CI immutable images] --> REG[ECR or selected registry]
  REG --> S
  OPS[Secrets metrics logs backup controls] --> S
```


This is a logical production topology; no AWS or cluster resources exist in the repository. [ADR-005](../adr/005-DEPLOYMENT-AND-CLIENT-BASELINE.md) proposes a single-host/container pilot before Kubernetes. EKS versus k3s versus simple containers remains O04; EKS is not a mandatory MVP milestone. A single host and single broker have explicit downtime/data-loss risks and cannot be marketed as highly available.

## 6. API gateway architecture

### 6.1 Routes and validation

Canonical client base path is `/api/v1`; the production hostname is not yet verified. The previous `api.hrflow.app` example is illustrative, not evidence of domain ownership. Target route prefixes and ports are defined in the [contract conventions](../REST-API-DATABASE-DESIGN.md). JWT validation checks allowed algorithm RS256, signature, issuer, audience, expiration, not-before, and bounded clock skew. Reject unsigned tokens, algorithm substitution, unknown issuer, and tokens for a different audience. Access tokens contain `sub`, `tenantId`, `employeeId` when linked, `roles`, `iat`, `exp`, and `jti`. Services repeat validation and enforce resource-level rules.

Strip `X-Tenant-Id`, `X-Employee-Id`, and `X-Roles` from public requests. Derive tenant context from the verified token; a tenant switch requires a new token, not a header edit. A login includes tenant slug because email is only unique within a tenant. Public routes are login, refresh, and JWKS; liveness exposure is narrowly configured. Tenant provisioning requires a separate platform credential and is never anonymous. No catch-all route may accidentally expose Actuator, debug, Registry, Kafka UI, or internal jobs.

### 6.2 Proposed edge budgets

| Control | Initial proposal | Validation |
|---|---|---|
| JSON body | 256 KiB except evidence batch capped separately | Over-limit request returns 413 before controller |
| Evidence batch | ≤100 observations and ≤256 KiB | Validate each item, all-or-nothing admission |
| Normal API timeout | 5 seconds | Client timeout greater than edge timeout; preserve idempotency on retry |
| Login limiting | Per tenant/account and source IP; thresholds calibrated | 429 and `Retry-After`; no permanent attacker-triggered lockout |
| SSE | Separate long-lived route; heartbeat every 20 seconds proposed | Proxy idle timeout above heartbeat; per-user connection limit 3 proposed |
| Page size | Default 20, maximum 100 | Reject unsupported sort fields |

These limits are not configured today. The gateway does not automatically retry mutating upstream requests. If the service committed but the connection failed, the client repeats the same key. Return normalized error bodies without exposing upstream stack traces. Preserve a validated UUID correlation ID or generate one; return `X-Correlation-Id`. Do not use the correlation ID as authentication or deduplication identity.

### 6.3 Browser and failure behavior

CORS uses an explicit environment-specific origin allowlist. Cookie-based refresh uses HttpOnly, Secure, appropriate SameSite, origin checks, and CSRF protection. Web access tokens remain in memory; do not place refresh secrets in localStorage. Content security policy, frame restrictions, and output escaping are required for the web deployment. Gateway liveness checks process health; readiness checks ability to route, not every provider's availability. Gateway unavailability yields client network/retry feedback; mobile preserves unsent commands locally. Cached valid keys allow verification during identity downtime until cache/key validity policy expires. Unknown key IDs trigger bounded refresh; fail closed if no valid key can be found.

## 7. Identity, organization, and tenancy

### 7.1 Provisioning and organization

Tenant provisioning creates tenant, first OWNER, and audit event in one identity transaction. Initial credential delivery/activation remains part of O08: use controlled synthetic seed credentials for demos; real owner accounts stay disabled until a reviewed one-time activation process exists. Never email or return a permanent plaintext password. An OWNER may grant HR_ADMIN through a reviewed administration flow; no implicit role inheritance is assumed. Manager hierarchy must be acyclic and within the tenant. Changing a manager emits a revised employee snapshot with effective relationship data. Assignments are effective-dated and non-overlapping for the MVP single-schedule-per-date policy. Site and holiday edits publish snapshots sufficient to update local projections. Department/position display fields needed downstream belong in employee snapshots; consumers do not query identity synchronously.

### 7.2 Passwords and token lifecycle

Proposed authentication ownership is identity-org, consistent with the original API draft. Use a vetted Spring Security password encoder with configurable work factor; never reversible password encryption. Login errors do not reveal whether tenant/email/password was wrong. Access lifetime proposal is ten minutes; refresh lifetime proposal is fourteen days, subject to security review. Store only refresh-token hashes; tokens have high entropy and are opaque. Rotation locks the token family, invalidates the old token, creates one replacement, and commits before response. Reuse of a rotated token revokes the family and requires login; clients serialize refresh attempts. Refresh responses cannot be stored as plaintext idempotency results. Logout revokes the refresh family; access revocation is bounded by short expiration unless a revocation cache is added. Password change revokes other families and records an audit action. Termination/suspension enforcement propagation and the maximum access-token window require explicit launch sign-off.

Publish public signing keys via JWKS only; private keys reside in protected secret storage. Introduce a new key before signing with it; retain old verification keys longer than maximum access lifetime plus skew/cache window. Document emergency key compromise response separately from routine rotation. Gateway and services cache JWKS rather than calling identity per request. No business service receives a reusable tenant administrator token for background jobs.

### 7.3 Tenant isolation layers

| Layer | Required control |
|---|---|
| HTTP | Tenant from verified token; foreign tenant IDs return 404 without existence leakage |
| Queries | Repository predicates require tenant; pagination/counts use the same scope |
| Database | Composite tenant FKs/uniques; distinct service roles; RLS defense-in-depth proposed |
| Events | Producer-owned topic ACL, envelope tenant validation, tenant-scoped references |
| Caches | Key includes tenant, subject/scope, resource and revision |
| Storage | Tenant/resource prefix, authorization before issuing short-lived signed URL |
| Exports | Tenant and requester persisted; reauthorize each status/download request |
| Jobs | Explicit tenant in job record; context reset after each task |
| Logs | No sensitive payload; tenant ID only in access-controlled logs, not high-cardinality metrics |

`search_path` or Hikari schema selection alone does not enforce service ownership. Use runtime roles with grants only on owned schema and a separate migration role. RLS is proposed additional defense; it requires transaction-local tenant setup and pool reset tests before adoption. Even with RLS, application authorization remains required.
