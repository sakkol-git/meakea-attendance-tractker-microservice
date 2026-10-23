# Approved architecture detail 03

> **Decision:** This is part of the latest user-approved final system design. It is authoritative design intent, not evidence of implemented behavior. Read the canonical Architecture index for supersession, security gaps and deployment constraints.

## 13. Request workflow domain

### 13.1 Model

A `request` has a type and a configurable **approval chain** (for example `["MANAGER", "HR_ADMIN"]`). Each chain element becomes an `approval_step` row when the request is submitted.

### 13.2 State machine

```text
DRAFT ──submit──► PENDING ──all steps approved──► APPROVED ──fulfil (materials)──► FULFILLED
  │                  │ any step rejected
  │ cancel           ▼
  ▼               REJECTED
CANCELLED ◄──cancel before final decision── PENDING
```


### 13.3 Step resolution

- `MANAGER` resolves to the requester's `manager_id` from `employee_ref`.
- `HR_ADMIN` resolves to any user holding the role in the tenant (inbox visible to all HR admins; first decision wins).
- If the requester has no manager, the step is skipped (`SKIPPED`) and recorded.

### 13.4 Why a separate service from leave

Leave has balance accounting and a calendar effect on attendance; general requests have a form-driven payload (`data jsonb`) and no balance. Merging them would force one model to carry both; separate services keep each simple and let the leave policy evolve independently.



## 14. Notification pipeline

```text
domain event ─► rule: who should be told? which category? ─► render template (locale)
      │                                                           │
      │                          ┌────────────────────────────────┤
      │                          ▼                                ▼
      │                   in-app inbox row               channel decision (preferences)
      │                          │                          │            │
      │                  SSE stream to web           FCM/APNs push      email
      ▼                                                    ▼             ▼
  inbox guard                                       delivery_attempt rows (retry on failure)
```


| Topic                | Decision                                                                                                                                   |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Recipient resolution | From the local `employee_ref` (employee, manager, HR)                                                                                      |
| Templates            | Per tenant, per channel, per locale, with `{{placeholders}}`                                                                               |
| Preferences          | Per category and channel; critical categories (approval needed) cannot be fully disabled                                                   |
| Retries              | Exponential backoff up to 5 attempts, then mark failed and alert                                                                           |
| Idempotency          | Inbox on `eventId` plus a unique `(eventId, recipient, channel)` so redelivery never double-sends                                          |
| Quiet hours          | Optional per-user window; reminders still allowed                                                                                          |
| Web real time        | Server-Sent Events from `/notifications/stream`, fed by an in-memory broadcaster per instance (scale-out would use Kafka or Redis fan-out) |
| Reminder timing      | Driven by `time.*` events and the tenant timezone, never by a cron that reads attendance tables                                            |



## 15. Reporting and read models

### 15.1 Approach

Reporting is a pure **projection**: it consumes events and maintains denormalised tables so dashboards never query transactional services.

| Read model        | Source                       | Rebuild                                                   |
| ----------------- | ---------------------------- | --------------------------------------------------------- |
| `employee_dim`    | `org.employee.v1`            | Replay compacted topic                                    |
| `attendance_fact` | `attendance.daily-status.v1` | Replay retained topic (30 d) or re-export from attendance |
| `leave_fact`      | `leave.request.v1`           | Replay                                                    |
| `request_fact`    | `request.workflow.v1`        | Replay                                                    |
| `daily_summary`   | derived from facts           | Recomputed by a nightly job and on change                 |

### 15.2 Consistency

Dashboards are **eventually consistent** (typically under a second). The UI shows a "data as of" timestamp from the latest processed event.

### 15.3 Exports

`POST /reports/exports` creates an `export_job`; a worker builds CSV/XLSX, stores it in S3, and updates the job with a presigned download URL. Limits: row cap, one running export per tenant.

### 15.4 Retention gap

Kafka keeps only 30 days of `daily-status`; older history is retained in `attendance_fact` itself. If reporting needs a full rebuild beyond retention, it re-exports from attendance via a one-off admin job (documented exception to "events only").



## 16. ML confidence service

### 16.1 Role

ML is the **intelligence layer around the rules**, not the attendance system. It outputs a probability that the employee was truly present, which the attendance service turns into an inferred session or a prompt.

### 16.2 Decision bands

| Confidence                                                                      | Decision           | Effect                                  |
| ------------------------------------------------------------------------------- | ------------------ | --------------------------------------- |
| ≥ auto threshold (≥ 0.80; chosen to reach about 95% precision on hold-out data) | `AUTO_INFER`       | Inferred session, next-day confirmation |
| 0.50 to auto threshold                                                          | `ASK_EMPLOYEE`     | Prompt the employee                     |
| < 0.50                                                                          | `NO_ACTION`        | Remains absent unless a tap arrives     |
| mock location                                                                   | forced `NO_ACTION` | Rule overrides the model                |

### 16.3 Features (one row per employee-day)

`in_geofence`, `gps_accuracy_m`, `minutes_in_zone`, `wifi_connected`, `ble_beacons_seen`, `movement_events`, `interaction_events`, `minutes_after_shift_start`, `arrival_deviation_min`, `is_usual_device`, `mock_location_flag`, `present_days_last_14`.

The key signal against a forgotten phone is **movement and interaction**: a person moves and touches the phone; a phone left on a desk does not.

### 16.4 Training loop

```text
inferred sessions ──► next-day confirm/reject tap ──► label (1 or 0)
        │                                                  │
        └── feature snapshot saved at decision time ───────┘
                              │
                nightly/weekly export to CSV
                              ▼
        train: logistic regression vs gradient boosting (cross-validation)
               prefer the simple model unless boosting is clearly better (> 0.01 AUC)
               calibrate (sigmoid), choose threshold for target precision on hold-out
                              ▼
        models/model.joblib + meta.json (version, thresholds, baseline, metrics)
```


### 16.5 Integration (event-based)

| Step            | Event                                                                                                       |
| --------------- | ----------------------------------------------------------------------------------------------------------- |
| attendance → ML | `attendance.scoring-requested.v1` with the features                                                         |
| ML → attendance | `attendance.confidence.v1` with confidence, decision, reasons, `modelVersion`                               |
| Labels          | attendance publishes `attendance.session.v1` (Confirmed/Rejected); ML consumes it to build the training set |

An HTTP `/score` endpoint exists for local demos and debugging only. It is **not** used by attendance in production because of the no-service-to-service-HTTP rule.

### 16.6 Explainability

For every score the service returns the top three features by an **occlusion test** (replace one feature with its training median and measure the probability change). The reasons are stored with the inferred session and shown to HR.

### 16.7 Operations

| Topic          | Approach                                                                                                                 |
| -------------- | ------------------------------------------------------------------------------------------------------------------------ |
| Cold start     | No model file → rules score used; the service never blocks attendance                                                    |
| Bias of labels | Only inferred sessions get confirm/reject; tapped days can be added as positives; document the limitation                |
| Drift          | Monitor the share of AUTO_INFER, the confirmation rate and the feature means weekly; alert on large changes              |
| Versioning     | `modelVersion` stored with every decision for auditability                                                               |
| Safety         | A hard rule (mock location) and the human confirmation step bound the model's authority                                  |
| Privacy        | No images or biometrics; features are aggregates, no raw GPS tracks                                                      |
| Not used       | Camera-based detection (YOLO) was evaluated and rejected: it cannot identify people, adds hardware cost and privacy risk |



## 17. Data architecture

### 17.1 Database layout

One PostgreSQL instance, **one schema per service**, one database role per service with rights only on its schema. Services never query another schema; this is what keeps a later split into separate databases a configuration change.

```text
hrflow db
├── identity      (identity-org role)
├── attendance    (attendance role)
├── leave  request  notification  reporting   (each with its own role)
```


Connection: `jdbc:postgresql://host:5432/hrflow?currentSchema=<svc>`; Flyway `spring.flyway.schemas=<svc>`, one `V<n>__*.sql` sequence per service.

### 17.2 Common conventions

| Convention    | Rule                                                                          |
| ------------- | ----------------------------------------------------------------------------- |
| Keys          | UUID v7 (time-ordered, index friendly)                                        |
| Tenant        | `tenant_id uuid not null` + leading column in every composite index           |
| Timestamps    | `timestamptz`, UTC; business dates as `date` plus the tenant timezone         |
| Audit columns | `created_at`, `updated_at`, `version bigint` (optimistic lock)                |
| Soft delete   | `deleted_at` on master data only                                              |
| Money / days  | `numeric`, never float                                                        |
| JSON          | `jsonb` only for genuinely variable data (request `data`, evidence `payload`) |

### 17.3 Hot tables and indexes

| Table                           | Expected growth                         | Index / partitioning                                                                                |
| ------------------------------- | --------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `attendance.evidence`           | highest (several rows per employee-day) | Range partition by month; index `(tenant_id, employee_id, at)`; drop old partitions after retention |
| `attendance.attendance_session` | ~1–3 per employee-day                   | `(tenant_id, employee_id, work_date)`; unique `(tenant_id, employee_id, check_in_at)`               |
| `attendance.daily_status`       | 1 per employee-day                      | PK `(tenant_id, employee_id, work_date)`; index `(tenant_id, work_date, status)` for dashboards     |
| `*.outbox`                      | transient                               | Partial index on `created_at where published_at is null`                                            |
| `*.inbox`                       | grows with events                       | PK `(consumer, event_id)`; purge older than the retention of the topic                              |
| `reporting.attendance_fact`     | 1 per employee-day                      | Same shape as `daily_status`, plus `(tenant_id, department_id, work_date)`                          |

### 17.4 Data ownership and duplication

Some data is intentionally duplicated as **read models** (`employee_ref`, `shift_ref`, `holiday_ref`, `leave_ref`). Rules:

- The owner service is the only writer of the truth; the copy is rebuilt from events and may be dropped and replayed.
- Copies store only the fields the service needs, nothing more.
- A copy is never exposed to clients as if it were the source.

### 17.5 Retention and backup

| Data                                | Retention                                                                                    |
| ----------------------------------- | -------------------------------------------------------------------------------------------- |
| Evidence                            | 90 days (configurable), then partition drop                                                  |
| Sessions, daily status, corrections | Employment duration plus the legal period required by the company **[confirm local rules]**  |
| Audit log                           | 1 year minimum                                                                               |
| Notifications (inbox)               | 90 days                                                                                      |
| Kafka topics                        | See section 10.1                                                                             |
| Backups                             | Managed Postgres automated snapshots + point-in-time recovery; restore drill before the demo |



## 18. API architecture

### 18.1 Style

REST over HTTPS, JSON, under `/api/v1`. Resources are plural nouns; actions that are not CRUD use a sub-resource verb (`POST /leave/requests/{id}/approve`).

### 18.2 Conventions (summary)

| Concern       | Convention                                                                          |
| ------------- | ----------------------------------------------------------------------------------- |
| Versioning    | URL major version; additive changes stay in `v1`                                    |
| Auth          | `Authorization: Bearer <JWT>`                                                       |
| Pagination    | `page`, `size`, `sort`; envelope `{items, page, size, total}`                       |
| Idempotency   | `Idempotency-Key` on creating POSTs; replay returns the first response              |
| Status codes  | 200, 201, 202 (async), 204, 400, 401, 403, 404, 409 (conflict), 422 (business rule) |
| Error body    | `{code, message, details[], traceId}`; `code` is a stable machine string            |
| Time          | ISO-8601 UTC                                                                        |
| Concurrency   | `ETag`/`version` on updates; 409 on stale write                                     |
| Documentation | OpenAPI generated per service and merged at the gateway                             |

### 18.3 Command versus query

Commands return `201/202` with the resource and **never wait for downstream effects**. Queries read the service's own tables or read models. A client that needs "my leave reflected on my calendar" subscribes to a notification or polls the read endpoint; it does not expect cross-service consistency in the same response.

### 18.4 Gateway route table

| Prefix                                                                                                                                                  | Target       | Public                    |
| ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ | ------------------------- |
| `/api/v1/auth/**`, `/tenants/**`, `/employees/**`, `/departments/**`, `/positions/**`, `/work-sites/**`, `/shifts/**`, `/holidays/**`, `/audit-logs/**` | identity-org | login, refresh, JWKS only |
| `/api/v1/attendance/**`                                                                                                                                 | attendance   | none                      |
| `/api/v1/leave/**`                                                                                                                                      | leave        | none                      |
| `/api/v1/requests/**`, `/materials/**`                                                                                                                  | request      | none                      |
| `/api/v1/notifications/**`                                                                                                                              | notification | none                      |
| `/api/v1/reports/**`                                                                                                                                    | reporting    | none                      |

### 18.5 Backward compatibility with mobile

Mobile apps cannot be force-updated instantly. Rules: never remove or rename a response field in `v1`; add fields only; ship a minimum-supported-version check at login (`426 UPGRADE_REQUIRED`) as an emergency lever.



## 19. Web client (Nuxt)

| Topic          | Decision                                                                                                                         |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| Rendering      | SPA-style app behind login; SSR is not needed for authenticated pages                                                            |
| Language       | TypeScript, `npm` (lockfile `package-lock.json`)                                                                                 |
| State          | Pinia stores per domain (auth, attendance, leave, requests, notifications)                                                       |
| API access     | One typed client module generated from the merged OpenAPI; one place to attach the token and handle 401 → refresh → retry        |
| Token storage  | Access token in memory; refresh token in an `HttpOnly`, `Secure`, `SameSite` cookie set by the gateway **[decision to confirm]** |
| Routing guards | Route middleware checks role claims; the server remains the authority                                                            |
| Real time      | `EventSource` on `/notifications/stream` for the badge and live approvals                                                        |
| Modules        | `/dashboard`, `/attendance`, `/leave`, `/requests`, `/employees`, `/org`, `/reports`, `/settings`                                |
| Design system  | Tokens (colour, spacing, radius) shared in a small package; light and dark mode                                                  |
| Accessibility  | WCAG AA targets, keyboard-navigable tables                                                                                       |
| Testing        | Vitest for logic, Playwright for the main flows (login, approve leave, view dashboard)                                           |



## 20. Mobile client (Flutter)

### 20.1 Responsibilities

Check-in/out, reminders, evidence collection, leave and request forms, notification inbox, offline tolerance.

### 20.2 Evidence collection without killing the battery

| Signal                 | Mechanism                                                         | Cost control                                            |
| ---------------------- | ----------------------------------------------------------------- | ------------------------------------------------------- |
| Geofence               | OS geofencing APIs (enter/exit events)                            | No continuous GPS; the OS wakes the app on a transition |
| Wi-Fi                  | Compare connected BSSID with the site's configured BSSID          | Check only on app wake or at shift start                |
| BLE                    | Short scan for office beacons after a geofence enter              | Scan window of a few seconds                            |
| Movement / interaction | Counters from the OS activity recogniser and screen-unlock events | Aggregated counts, not raw sensor streams               |

Mobile OSes restrict background work; the design therefore relies on **event wake-ups and the user's tap**, never on a constantly running service.

### 20.3 Offline behaviour

- Check-in taps are written to a local queue with a client-generated `Idempotency-Key` and `client_at`.
- The queue drains when the network returns; the server records its own receive time and keeps `client_at` as evidence.
- The UI shows "pending sync" for queued items.

### 20.4 Security on the device

Biometric unlock before the check-in action; secure storage for tokens; device id bound to the employee; root/jailbreak and mock-location signals reported as evidence flags (not blockers).

### 20.5 Contract with the backend team

The mobile developer works against this document's API and a generated mock server from the OpenAPI files, so work starts in week 2 without waiting for the backend.



## 21. Infrastructure and deployment

### 21.1 Local development

```text
docker compose (infra/docker/docker-compose.yaml)
  postgres:5432   kafka:9092 (host) / kafka:19092 (containers)   schema-registry:8081
  kafka-ui:8090
services run from the IDE with: spring.kafka.bootstrap-servers=localhost:9092
```


Kafka has two listeners so that tools inside containers and applications on the host both connect correctly.

### 21.2 Target cloud topology (AWS) cost and sizing to confirm against current pricing

```text
Internet ─► Route 53 ─► CloudFront (web static)
                    └─► ALB / ingress ─► Kubernetes cluster
                                          ├─ api-gateway
                                          ├─ 6 Spring services, confidence-service
                                          └─ (optional) Kafka via Strimzi
        private subnets:  RDS PostgreSQL (Multi-AZ optional)   MSK or in-cluster Kafka
        S3 (attachments, exports, web build)   ECR (images)   Secrets Manager   CloudWatch /
  Prometheus
```


| Component | Options                                          | Recommendation for the project                                                  |
| --------- | ------------------------------------------------ | ------------------------------------------------------------------------------- |
| Compute   | EKS, ECS Fargate, single-node k3s on EC2         | EKS or k3s for the demo to satisfy the Kubernetes requirement; choose by budget |
| Kafka     | Amazon MSK, MSK Serverless, in-cluster (Strimzi) | In-cluster Kafka for the demo; MSK shown as the production option               |
| Database  | RDS PostgreSQL                                   | One small instance, schema per service                                          |
| Registry  | Confluent Schema Registry container              | Same cluster                                                                    |
| Web       | S3 + CloudFront                                  | Static build output of Nuxt                                                     |

### 21.3 Kubernetes layout

| Item       | Design                                                                                                                       |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Namespaces | `hrflow-apps`, `hrflow-data` (if Kafka/registry in-cluster), `monitoring`                                                    |
| Workloads  | One `Deployment` per service; `HorizontalPodAutoscaler` on CPU and Kafka lag for consumers                                   |
| Probes     | Liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness` (readiness DOWN until read models bootstrapped) |
| Config     | `ConfigMap` for non-secret settings, `Secret` (synced from Secrets Manager) for credentials                                  |
| Network    | `NetworkPolicy` default deny; allow gateway → services, services → Kafka/DB                                                  |
| Ingress    | One ingress to the gateway only                                                                                              |
| Resources  | Requests/limits per pod; JVM `-XX:MaxRAMPercentage=75`                                                                       |
| Rollouts   | Rolling update, `maxUnavailable: 0`, `PodDisruptionBudget`                                                                   |
| Manifests  | Kustomize base + `dev` / `prod` overlays                                                                                     |

### 21.4 Terraform structure

```text
infra/terraform/
├── modules/  network  eks  rds  s3  ecr  iam  secrets
└── envs/     dev  prod        (remote state in S3, locking in DynamoDB)
```


Rules: state is remote and locked; no credentials in code; every resource tagged `project=hrflow`, `env=<env>`; `terraform plan` runs on pull requests.

### 21.5 Environments

| Environment | Purpose            | Data                        |
| ----------- | ------------------ | --------------------------- |
| local       | Development        | Docker Compose, seed script |
| dev         | Shared integration | Synthetic data              |
| prod (demo) | Final presentation | Seeded demo tenant          |
