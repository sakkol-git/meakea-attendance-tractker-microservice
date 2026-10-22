# Architecture reference 03

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## 23. Reliability and recovery runbooks

| Failure | Detect | Immediate behavior | Recover and verify |
|---|---|---|---|
| Gateway down | Health/HTTP failure | Clients retain pending keys | Restore container/config; replay same key, verify one resource |
| PostgreSQL down | Connection/commit errors | No false success; 503/retry | Restore DB; inspect uncertain commits by key |
| Kafka down | Relay errors/backlog | Local commands commit while bounded capacity remains | Recover broker; drain ordered outbox; compare IDs |
| Consumer killed | Lag/no heartbeat | Other services continue | Restart; inbox dedupe; verify one logical effect |
| Provider down | Attempt failures | Inbox persists, external delivery pending | Retry with backoff; distinguish unknown acceptance |
| Duplicate event | Inbox unique conflict | No repeated mutation | Acknowledge; compare state/ledger |
| Delayed event | Revision/time lag | Guard snapshot; flag stale | Apply newer revision/recompute; preserve history |
| Poison schema | Consumer error/quarantine | Block affected aggregate or quarantine safely | Fix reader/schema; audited replay original event ID |
| Outbox backlog | Age/disk growth | Degraded async status; bounded admission | Fix broker/head; reconcile status counts |
| Failed migration | Deployment job fails | Do not deploy incompatible code | Forward fix or compatible image rollback; clean DB test |
| Corrupt projection | Reconciliation mismatch | Mark stale/disable affected export | Shadow rebuild, compare, switch atomically |
| Object storage down | Upload/export errors | No READY file claim | Retry job; verify checksum/access/expiry |
| Bad deployment | Smoke/error regression | Stop rollout | Known digest/config rollback if schema compatible |
| Host/region loss | Infrastructure monitoring | Pilot outage; disclose scope | Restore backups to new target; rehydrate broker/projections |

Proposed pilot recovery objectives are RPO ≤24 hours and RTO ≤4 hours, pending business agreement and successful restore exercise. They are recommendations, not existing guarantees; actual backup frequency/restore speed determines feasibility. Broker replay cannot recover business facts that were never backed up in the owner database. Restore exercises verify tenant counts, ledger consistency, object references, outbox/inbox state, and access-key validity. Do not replay old notifications to real users during recovery validation.

## 24. Performance and capacity engineering

Keep check-in to bounded local queries, employee guard/session write, idempotency write, and outbox insert. No provider call, cross-service lookup, model prediction, or report aggregation belongs in that transaction. Index tenant/employee/open-session and employee/work-date lookups. Measure lock contention separately for duplicate submissions from the same employee. Export workers use a separate bounded pool/queue so CSV generation cannot starve attendance connections. Kafka throughput depends on partition skew, serialization, broker I/O, DB writes, and consumer batch behavior. Size partitions from measured throughput and future migration cost, not solely current script defaults. Evidence batch limits prevent one phone from monopolizing a transaction.

Load fixture: at least two tenants, distinct employees, active assignments, holidays, and leave cases. Run warm-up followed by a recorded steady-state window; report sample count, p50/p95/p99, throughput, error codes, hardware, pool sizes, and payload size. Include network loss after commit, duplicate keys, same-employee races, stopped consumers, and broker downtime. Dashboard propagation measurement correlates source event commit with applied projection and client render acknowledgment in test tooling. No existing benchmark establishes the original targets.

## 25. Testing strategy and ownership

| Layer | Primary owner | Required evidence |
|---|---|---|
| Domain unit | A | Shift/date/status, transitions, balance arithmetic, overlap |
| Repository/migration | A | PostgreSQL constraints, locks, clean/upgrade migration |
| Kafka contract | A with B review | Envelope/payload fixtures, compatibility, keys, revisions |
| API integration | A | Role/resource/tenant, idempotency, If-Match, errors |
| Mobile unit/offline | B | Stable queued keys, refresh serialization, permission denial |
| Web interaction/accessibility | A | Empty/error/stale states, filters, keyboard workflows |
| End-to-end | A+B | Login → check-in → projection → approval → export |
| Security | A, independent review before launch | Tenant substitution, token/header abuse, file access |
| Recovery/load | A with B device validation | Kill consumer, restart relay, restore, 100-client run |

Use Testcontainers for PostgreSQL/Kafka integration where practical, not mocks for transaction/offset guarantees. Duplicate test: deliver one event twice, assert one ledger/projection/notification effect. Out-of-order test: revision 4 then 3, assert revision 4 remains. Saga test: approve, stop attendance, cancel, resume; older approval must not resurrect leave. Kill-consumer test: stop notification, check in successfully, restart, catch up, assert one logical inbox item. Crash-relay test: send acknowledgment then kill before published mark; verify duplicate is harmless. Tenant test includes count endpoints, reports, SSE reconnect, attachments, and background jobs. Current coverage is only five confidence API tests; no percentages are claimed.

## 26. Evolution, simplifications, and decision gates

MVP keeps one database instance, one local broker, polling outbox, and a narrow end-to-end slice. Do not add Redis, service mesh, Debezium, Kafka Streams, or EKS merely to match an enterprise checklist. Merge reporting into a notification projections module if delivery pressure warrants, preserving logical ownership and contracts. Extract services only when independent load, ownership, deployment cadence, or fault isolation justifies operational overhead. Scale broker/DB topology after measured saturation or agreed availability requirements. Adopt evidence partitioning after measured retention/index pressure. Adopt ML only after permitted evidence, real labels, calibration, and policy review.

### 26.1 Confidence prototype disposition

`ml/confidence-service` implements `/health`, `/model`, and `/score` with twelve features and a mock-location hard block. It loads a joblib model if both artifacts exist; missing artifacts use rules, but corrupt artifacts/prediction errors are not handled as a complete fallback. Five tests pass in its own directory; root invocation needs import-path packaging work. Training selects threshold on the same holdout used to report automatic precision; a separate final evaluation set is required. If no threshold meets target precision, fallback 0.95 does not establish safe automatic inference. Saved threshold rounding and unpinned dependencies can change deployed decisions. Metadata does not establish real-data provenance; synthetic demo metrics are not pilot accuracy. The Python 3.12 image and local Python 3.14 environment require a pinned artifact compatibility check. No Kafka integration exists; asynchronous scoring result contracts would be a separate proposed extension. A vision service is optional future work and is removed from mandatory week-12 delivery.

### 26.2 Open questions

| ID | Decision | Owner and deadline | Safe interim behavior |
|---|---|---|---|
| O01 | Nuxt versus original React plan | A+B, week 1 | Keep scaffold; no framework rewrite |
| O02 | Holiday/leave/manual precedence, late+early mapping, conflict balance handling | Product/HR+A, before leave integration | Preserve evidence, flag conflicts, no automatic resolution |
| O03 | Correction window, pending inference expiry, overnight/DST support | Product/HR+A+B, week 2 | Same-day shifts; explicit review; no guessed finalization |
| O04 | Pilot hosting and operational budget | A/operator, week 5 | Local demo; no EKS mandate |
| O05 | Privacy basis, consent, retention/erasure, recovery objectives | Tenant owner/security/operator, before real data | Synthetic data only |
| O06 | Avro envelope/Registry versus simpler JSON contract | A+B, week 1 | Existing Avro remains prototype; no incompatible publication |
| O07 | Final Boot parent inheritance and tested toolchain | A, week 1 | Do not alter versions for documentation |
| O08 | Auth lifetimes, revocation window, platform provisioning credential | Security+A, week 2 | Proposed short access tokens; block launch without review |

## 27. Appendices and traceability

### 27.1 Decision index

- [ADR-001: Service and event boundaries](../adr/001-SERVICE-AND-EVENT-BOUNDARIES.md).
- [ADR-002: Tenant and persistence isolation](../adr/002-TENANT-AND-PERSISTENCE-ISOLATION.md).
- [ADR-003: Event delivery and contracts](../adr/003-EVENT-DELIVERY-AND-CONTRACTS.md).
- [ADR-004: Attendance and leave conflicts](../adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md).
- [ADR-005: Deployment and client baseline](../adr/005-DEPLOYMENT-AND-CLIENT-BASELINE.md).

### 27.2 Requirement traceability

| ID | Requirement | Design | Acceptance evidence |
|---|---|---|---|
| R01 | Auth and tenant isolation | Sections 6–7, 17 | Tenant/header/JWT/object tests |
| R02 | Organization/calendar | Section 7 | Effective assignment and holiday projection fixtures |
| R03 | Durable check-in/out | Section 8 | Atomic write, duplicate key, concurrent open-session tests |
| R04 | Event recovery | Sections 13–14 | Relay crash, inbox duplicate, order/kill-consumer tests |
| R05 | Leave approvals and balances | Section 9 | Locking/reservation/saga/cancel tests |
| R06 | General requests | Section 10 | Chain/version/participant/fulfillment tests |
| R07 | Notifications/live views | Sections 11–12 | Inbox dedupe, SSE reconnect, freshness assertions |
| R08 | Audit/corrections | Sections 8, 15, 17 | Append-only decisions and actor/reason tests |
| R09 | Reports/exports | Section 12 | Projection rebuild, authorized CSV, URL expiry |
| R10 | Reproducible deployment | Sections 20–23 | Fresh setup, smoke, rollback, restore rehearsal |
| R11 | Nudges/inference | Sections 8, 18, 26 | Permission/device/battery/labeled evaluation; should/stretch |
| R12 | Performance | Sections 3, 24 | Recorded 100-client latency/propagation report |

### 27.3 Configuration inventory

| Setting | Current evidence | Target handling |
|---|---|---|
| Java | POM target 21; review shell Java 17 | CI/dev Java 21 toolchain gate |
| Boot | Root 4.1.1, service 4.0.8 | One reviewed inheritance policy |
| Cloud | Gateway 2025.1.3 | Verify compatible Boot baseline and test gateway |
| PostgreSQL | Compose 16, `hrflow` local credentials | Secret-managed distinct runtime/migration roles |
| Kafka | Compose 3.8.0, host 9092/internal 29092 | Private secure listener in deployment |
| Registry | Compose 8.0.0; serializer property 8.1.0 | Verify image/client compatibility, fix listener |
| Avro | 1.11.4; wrong generation directory | Correct path and compatibility fixtures |
| Web | Nuxt ^4.6.1, Vue ^3.5.43, router ^5.4.0 | Existing npm lockfile governs resolved build |
| Confidence | Unbounded Python minima, Python 3.12 image | Pin serving/training versions if included |
| Ports | Attendance currently 8082 | Target gateway 8080, services 8101–8106, confidence 8107 |
| Secrets | Hard-coded local DB values | External environment/secret store; never commit production values |

Environment variable names are not standardized today; implementation must document actual Spring binding/secret mounts rather than assume these target controls exist.

### 27.4 Glossary

ADR: architecture decision record. ACL: access-control list. API: application programming interface. DLQ: dead-letter queue/topic. DTO: data-transfer object. IAM: identity and access management. JWT: JSON Web Token. JWKS: JSON Web Key Set. RBAC: role-based access control. RLS: row-level security. RPO: recovery point objective. RTO: recovery time objective. SLO: service-level objective. SSE: Server-Sent Events. UTC: coordinated universal time. A projection is derived data, an aggregate is a consistency boundary, and an inbox is a durable consumer deduplication record.

### 27.5 External compatibility references

Checked 2026-10-22: [Spring Cloud compatibility matrix](https://spring.io/projects/spring-cloud/) lists 2025.1.x with Boot 4.0.x and 4.1.x starting with 2025.1.2. [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html) lists Java 17 as the framework minimum for 4.1.1; this repository independently targets Java 21. The observed parent mismatch is therefore a governance/reproducibility issue, not proof that Cloud 2025.1.3 is incompatible. Repository dependencies were not upgraded, and full application startup compatibility remains unverified.
