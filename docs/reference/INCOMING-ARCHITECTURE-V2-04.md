# Approved architecture detail 04

> **Decision:** This is part of the latest user-approved final system design. It is authoritative design intent, not evidence of implemented behavior. Read the canonical Architecture index for supersession, security gaps and deployment constraints.

## 22. CI/CD

```text
pull request
  ├─ build + unit tests (Maven reactor, Python tests, Nuxt unit tests, Flutter analyse/test)
  ├─ schema compatibility check against Schema Registry (contracts)
  ├─ integration tests (Testcontainers: Postgres + Kafka + Schema Registry)
  ├─ dependency + container image scan
  └─ terraform fmt / validate / plan

merge to main
  ├─ build and tag images (git SHA) → push to ECR
  ├─ register changed schemas
  ├─ deploy to dev (kubectl/kustomize apply) → smoke test
  └─ manual approval → deploy to prod
```


| Topic               | Decision                                                                                                             |
| ------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Image build         | Layered Spring Boot images or buildpacks; non-root user; one image per service                                       |
| Versioning          | Image tag = git SHA; release tags `v0.x.y` for demos                                                                 |
| Database migrations | Flyway runs at service start; migrations must be backward compatible with the previous app version (expand/contract) |
| Rollback            | Redeploy the previous image tag; schema changes are additive so rollback is safe                                     |
| Secrets             | GitHub OIDC to AWS (no long-lived keys in CI)                                                                        |



## 23. Observability

### 23.1 The three signals

| Signal  | Tooling                                           | Conventions                                                                                                                        |
| ------- | ------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Logs    | JSON to stdout, collected by Loki (or CloudWatch) | Fields: `timestamp`, `level`, `service`, `tenantId`, `traceId`, `correlationId`, `eventId`; never log tokens or full personal data |
| Metrics | Micrometer → Prometheus → Grafana                 | Per service: request rate, errors, latency percentiles, JVM, DB pool; per consumer: lag, processing time, retries, DLQ count       |
| Traces  | OpenTelemetry (Java agent)                        | Trace context propagated in HTTP headers and in Kafka headers, so one check-in can be followed from request to notification        |

### 23.2 Correlation

The gateway creates `X-Request-Id`; the service stores it as `correlationId` in the event; consumers copy it forward, so a single id links the HTTP call, the outbox row, every downstream event and the push notification.

### 23.3 Key dashboards

1. API: rate, error ratio, p95 latency per route.
1. Kafka: consumer lag per group and topic, DLQ depth, outbox backlog (rows with `published_at is null`).
1. Attendance: check-ins per minute, share of AUTO_INFER, confirmation rate, scoring latency.
1. Database: connections, slow queries, table sizes (evidence partitions).
1. Business: daily present / late / absent counts per tenant.

### 23.4 Service-level objectives (proposed)

| SLO                                 | Target        |
| ----------------------------------- | ------------- |
| Check-in API availability           | 99.5% monthly |
| Check-in latency                    | p95 < 300 ms  |
| Event-to-read-model delay           | p95 < 1 s     |
| Outbox publish delay                | p99 < 2 s     |
| Push notification delay after event | p95 < 5 s     |

### 23.5 Alerts

Outbox backlog older than 60 s; consumer lag above threshold for 5 minutes; any message in a DLQ; error ratio above 2% for 5 minutes; database connections above 80%; a service not ready for 3 minutes; model version missing or scoring errors.



## 24. Resilience and failure handling

### 24.1 Failure-mode table

| Failure                 | Effect                                                  | Mitigation                                                                                        |
| ----------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| Kafka down              | Events queue in the outbox; user requests still succeed | Outbox + retry; alert on backlog                                                                  |
| Schema Registry down    | Serialisation fails for uncached schemas                | Client schema cache; serialise inside the transaction early; registry run with redundancy in prod |
| One service down        | Only its APIs fail; others keep running                 | Local read models; gateway returns 503 for that route only                                        |
| Postgres down           | All writes fail                                         | Managed multi-AZ in prod; connection retry with backoff                                           |
| Duplicate events        | Double application                                      | Inbox idempotency                                                                                 |
| Out-of-order events     | Stale overwrite                                         | Per-key partition ordering; version checks on upserts                                             |
| Poison message          | Consumer blocked                                        | Retry topic then DLQ; the partition keeps flowing                                                 |
| Confidence service down | No ML scoring                                           | Timeout then fall back to the rules score                                                         |
| Gateway overload        | Cascading slowness                                      | Rate limits, timeouts, circuit breaker per route                                                  |
| Clock skew              | Wrong late/absent                                       | Server time authoritative; NTP on nodes                                                           |
| Push provider outage    | Missed reminders                                        | Retries; in-app inbox still receives the message                                                  |

### 24.2 Timeouts and retries (HTTP, client-facing only)

Gateway connect timeout 1 s, response timeout 5 s; no automatic retries on non-idempotent POSTs; retries only on safe GETs.

### 24.3 Backpressure

Consumers use bounded concurrency; exports and heavy reports run as async jobs; the outbox relay has a fixed batch size so catch-up after an outage does not overwhelm Kafka.

### 24.4 Outbox ordering across multiple instances

`SKIP LOCKED` allows several relay instances, but two instances could publish rows for the same key out of order. Decision: **one active relay per service**, chosen with a PostgreSQL advisory lock (`pg_try_advisory_lock`); other instances stand by. If throughput ever demands more, shard the relay by hash of the event key so each shard has one owner.

### 24.5 Consistency model summary

| Boundary           | Consistency                                            |
| ------------------ | ------------------------------------------------------ |
| Inside one service | Strong (one transaction)                               |
| Between services   | Eventual, typically under a second                     |
| Read models        | Rebuildable from events                                |
| User-visible gaps  | Shown with "as of" timestamps and optimistic UI states |

### 24.6 Disaster recovery

| Scenario              | Plan                                                                                                                                |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Lost service database | Restore snapshot; replay Kafka from the last committed offset for read models                                                       |
| Lost Kafka            | Rebuild read models from the owner services' tables via admin export jobs; outbox rows not yet published are still in each database |
| Bad deploy            | Roll back the image; migrations are additive                                                                                        |
| Leaked signing key    | Rotate key (`kid`), revoke refresh tokens, force re-login                                                                           |



## 25. Performance and capacity

### 25.1 Assumptions (adjust for the demo)

| Parameter                      | Value                                      |
| ------------------------------ | ------------------------------------------ |
| Tenants                        | 20                                         |
| Employees per tenant           | 250 (5,000 total)                          |
| Check-in window                | Most taps within 30 minutes of shift start |
| Evidence rows per employee-day | 20                                         |
| Events per employee-day        | ~15 across topics                          |

### 25.2 Estimates

| Quantity                   | Calculation           | Result                                         |
| -------------------------- | --------------------- | ---------------------------------------------- |
| Peak check-in rate         | 5,000 taps in 30 min  | ≈ 3 per second average, bursts to ≈ 30/s       |
| Evidence writes per day    | 5,000 × 20            | 100,000 rows/day                               |
| Events per day             | 5,000 × 15            | 75,000/day (well within a single small broker) |
| Evidence storage (90 days) | 100,000 × 90 × ~300 B | ≈ 2.7 GB                                       |

These volumes are small; the architecture's value is isolation and correctness, not raw scale. Load tests should still target ten times the estimate to expose bottlenecks.

### 25.3 Latency budget (check-in)

| Step                                     | Budget                             |
| ---------------------------------------- | ---------------------------------- |
| Gateway (JWT + routing)                  | 10 ms                              |
| Controller + validation                  | 5 ms                               |
| Local reads (shift, holiday, leave refs) | 10 ms                              |
| Insert session + upsert status + outbox  | 30 ms                              |
| Commit                                   | 10 ms                              |
| Network (client to cloud)                | variable                           |
| **Server total**                         | **< 100 ms typical, p95 < 300 ms** |

### 25.4 Scaling levers

Stateless services scale horizontally; consumers scale up to the partition count; Postgres scales vertically first, then read replicas for reporting; evidence partitions keep deletes cheap.



## 26. Testing strategy

| Level                 | Tooling                                                    | What it proves                                                                                         |
| --------------------- | ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| Unit                  | JUnit, plain objects                                       | Domain rules: precedence, late calculation, leave balance, state machines                              |
| Slice / component     | Spring tests with Testcontainers (Postgres)                | Repositories and migrations                                                                            |
| Messaging integration | Testcontainers (Kafka + Schema Registry)                   | Outbox → Kafka → inbox → read model, including duplicates and redelivery                               |
| Contract              | Schema compatibility check; consumer reads producer sample | Producers cannot break consumers                                                                       |
| API                   | REST-assured / MockMvc against OpenAPI                     | Status codes, error bodies, auth and role rules                                                        |
| Security              | Tests with forged `X-Tenant-Id`, wrong tenant, wrong role  | Tenant isolation and header stripping                                                                  |
| ML                    | pytest                                                     | Mock location blocked; forgotten-phone case not auto-inferred; metrics above a floor on synthetic data |
| End to end            | Playwright (web) + scripted API scenarios                  | Check-in → status → notification → report                                                              |
| Load                  | k6                                                         | Burst of check-ins; consumer lag recovery after a pause                                                |
| Chaos (light)         | Stop Kafka for 2 minutes, kill a service                   | Outbox backlog drains; no lost events                                                                  |

**Generated Spring tests** (`contextLoads`) are removed until real infrastructure tests replace them, because they fail without Postgres and Kafka.

**Definition of done** for a feature: code + unit tests + API doc updated + schema compatible + dashboard/alert considered + reviewed.



## 27. Privacy and compliance

This section is engineering guidance, not legal advice; confirm obligations for the countries where tenants operate.

| Topic               | Design                                                                                                                                                                                         |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Data minimisation   | Store transitions and summaries; never continuous location tracks                                                                                                                              |
| Purpose             | Location and sensor data used only to support attendance; not for other monitoring                                                                                                             |
| Transparency        | Onboarding screen explains what is collected and why; the employee can see their own evidence                                                                                                  |
| Consent and policy  | Employer policy plus an in-app acknowledgement; collection stops outside work hours                                                                                                            |
| Access              | Evidence visible to HR admins only, audit-logged                                                                                                                                               |
| Retention           | Evidence 90 days by default; configurable per tenant within limits                                                                                                                             |
| Right to correct    | Employees can reject an inferred session; HR can correct with a reason                                                                                                                         |
| Automated decisions | The ML result never penalises on its own: an inferred session needs confirmation, and low confidence never creates an absence by itself without the normal cutoff rules and manager visibility |
| Biometrics          | Only the phone's local biometric unlock is used; no biometric templates leave the device; no face recognition                                                                                  |
| Security            | Encryption in transit and at rest; least-privilege roles                                                                                                                                       |



## 28. Architecture decision records

Keep each decision as a short file in `docs/adr/`.

| ADR | Decision                                        | Alternatives considered                   | Reason                                                               |
| --- | ----------------------------------------------- | ----------------------------------------- | -------------------------------------------------------------------- |
| 001 | Microservices with event-driven integration     | Modular monolith                          | Brief requires microservices and Kafka; events give isolation        |
| 002 | No service-to-service HTTP                      | REST between services                     | Removes runtime coupling and cascading failures                      |
| 003 | Schema per service in one Postgres              | Database per service                      | Same isolation of access, far lower cost and operations              |
| 004 | Transactional outbox + inbox                    | Dual writes; Kafka transactions across DB | Atomic publish without distributed transactions                      |
| 005 | Avro + Schema Registry                          | JSON                                      | Enforced, evolvable contracts; JSON was the earlier simpler choice   |
| 006 | Partition key `tenantId:employeeId`             | Random key                                | Per-employee ordering                                                |
| 007 | JWT RS256, gateway verifies with public key     | Shared HMAC secret                        | Services and gateway never hold the signing key                      |
| 008 | Gateway-injected identity headers               | Each service validates JWT                | One validation point; requires network isolation                     |
| 009 | Tap first, infer if forgotten                   | Fully automatic detection                 | Cheap, robust, defensible; ML supports instead of replacing the user |
| 010 | scikit-learn confidence model                   | YOLO / camera, deep learning              | Cheaper, explainable, matches the data we have                       |
| 011 | JDBC, not JPA                                   | JPA/Hibernate                             | Explicit SQL for outbox and conditional updates                      |
| 012 | Polling outbox relay                            | Debezium CDC                              | Fewer moving parts for a two-person team                             |
| 013 | Nuxt (npm) for web                              | React                                     | Team choice; one lockfile standard                                   |
| 014 | Leave and general requests as separate services | One "requests" service                    | Different models (balance vs form payload)                           |
| 015 | Append-only attendance with precedence          | Mutable records                           | Auditability and safe corrections                                    |



## 29. Risks and open questions

### 29.1 Risks

| Risk                                       | Likelihood | Impact | Mitigation                                                                                                      |
| ------------------------------------------ | ---------- | ------ | --------------------------------------------------------------------------------------------------------------- |
| Scope too large for 12 weeks               | High       | High   | Build attendance, leave and notification first; request and reporting are thin; cut ML to the stretch if behind |
| Boot 4 / Confluent / Jackson compatibility | Medium     | Medium | Spike in week 1: produce and consume one Avro event end to end                                                  |
| Mobile background limits on iOS/Android    | High       | Medium | Rely on taps and OS geofence events; test on real devices early                                                 |
| Too few labels to train the model          | High       | Low    | Rules fallback; synthetic data clearly labelled in the report                                                   |
| Kafka and Kubernetes operations cost       | Medium     | Medium | In-cluster single broker; tear down after demos                                                                 |
| Two-person bus factor                      | Medium     | High   | Document decisions, uniform service template, one-command local setup                                           |
| Privacy objections                         | Medium     | Medium | Minimisation, transparency, HR-only evidence access                                                             |

### 29.2 Open questions

1. Where does the Flutter project live: `mobile/` in this repository or a separate one?
1. Are the Boot version and Confluent serializer 8.1.0 compatible in practice? (spike)
1. Refresh token delivery: cookie versus body for the web app.
1. Is row-level security in Postgres worth adding as a second tenant guard?
1. Cloud budget: EKS versus k3s, MSK versus in-cluster Kafka.
1. Which exact evidence signals will the demo have (Wi-Fi only, or BLE beacons too)?
1. Which retention periods apply for the target customers?



## 30. Delivery plan (12 weeks)

| Weeks | Backend / web / infra                                                                                                 | Mobile                                                     |
| ----- | --------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| 1–2   | Repo standardisation, poms, compose with Kafka + Schema Registry, gateway + JWT, outbox/inbox starter, **Avro spike** | Project setup, design tokens, mock server from the API doc |
| 3     | identity-org: login, employees, shifts, holidays                                                                      | Login, shell, navigation                                   |
| 4–5   | attendance: check-in/out, today, daily status, cutoff                                                                 | Check-in screen, reminders, offline queue                  |
| 6     | leave: types, balances, requests, approvals                                                                           | Leave screens                                              |
| 7     | notification: inbox, device tokens, push, SSE                                                                         | Push registration, inbox                                   |
| 8     | request service; evidence ingestion and rules score                                                                   | Evidence collection (geofence, Wi-Fi)                      |
| 9     | reporting dashboard and exports; confidence service integration                                                       | Confirm/reject inferred sessions                           |
| 10    | Kubernetes + Terraform deployment, CI/CD complete                                                                     | Beta build, device testing                                 |
| 11    | Hardening: tests, load test, dashboards, alerts, security review                                                      | Bug fixing, polishing                                      |
| 12    | Demo data, documentation, report, rehearsal                                                                           | Release candidate                                          |

Milestones: **end of week 2** one event flows end to end; **week 5** attendance usable from the phone; **week 9** all services feature-complete; **week 12** demo.



## 31. Glossary

| Term                | Meaning                                                                 |
| ------------------- | ----------------------------------------------------------------------- |
| Aggregate           | Cluster of data changed together under one consistency rule             |
| Avro                | Binary serialisation format with schemas                                |
| Backward compatible | New schema can read data written with the old one                       |
| Correlation id      | Identifier carried across HTTP and events to trace one business action  |
| DLQ                 | Dead-letter queue for messages that cannot be processed                 |
| Evidence            | A recorded presence signal (geofence, Wi-Fi, BLE, tap)                  |
| Geofence            | Virtual boundary around a work site                                     |
| Idempotent          | Applying the same operation twice has the same effect as once           |
| Inbox               | Table recording processed event ids to ignore duplicates                |
| Inferred session    | Attendance created by rules/ML, pending confirmation                    |
| KRaft               | Kafka's built-in consensus mode (no ZooKeeper)                          |
| Outbox              | Table holding events committed with business data, published afterwards |
| Read model          | Local, rebuildable copy of data owned by another service                |
| Schema Registry     | Service storing Avro schemas and checking compatibility                 |
| Tenant              | One customer company                                                    |



_End of document._
