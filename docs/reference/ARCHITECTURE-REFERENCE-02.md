# Architecture reference 02

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## 8. Attendance architecture

### 8.1 Evidence and interpretation

Permitted MVP evidence is explicit TAP/MANUAL; geofence/Wi-Fi/BLE is should-have and consent-gated. Record event ID, device binding, occurred time, received time, accuracy, evidence kind, and bounded payload. Do not collect continuous location trails; store only minimum evidence necessary for the permitted workflow. Client claims are untrusted signals; mock-location flags, movement, and device IDs do not prove human identity. A local biometric unlock authorizes access to a stored credential/device interaction; the backend still authenticates the bearer token. Reject evidence from inactive employees, wrong devices where binding is required, or invalid schemas. Quarantine excessive clock drift rather than silently overwriting trustworthy timestamps.

### 8.2 Session and day computation

Resolve a shift from the local assignment valid on work date. A shift carries IANA time zone, local start/end, grace minutes, working weekdays, and site reference. Store resolved UTC shift bounds with the day calculation so later master-data changes do not silently rewrite history. Late = first effective check-in after resolved start plus grace; default grace is ten minutes. Early leave = final effective checkout before resolved end, excluding approved partial leave intervals. Proposed status fields separate `dayType` from `attendanceState` and flags such as `late`/`earlyLeave`. The compatibility `status` uses PRESENT/LATE/ABSENT/LEAVE_EARLY/ON_LEAVE/HOLIDAY/DAY_OFF. If late and early both apply, flags preserve both; proposed compatibility priority LATE before LEAVE_EARLY needs O02 approval. Worked minutes sum non-overlapping effective closed intervals, clipped according to approved policy. An open session reports `MISSING_CHECKOUT`; do not invent a checkout for payroll-like totals. Multiple sessions are allowed; a partial unique index prevents two open effective sessions.

Overnight shifts are a proposed extension; MVP rejects end ≤start until DST/overnight policy is tested and approved. For supported overnight shifts, work date remains the local shift-start date and end resolves on the following date. Ambiguous/nonexistent DST local times require a stored explicit offset resolution; never rely on server default time zone.

### 8.3 Precedence and unresolved policy

Original requirements contain both “Holiday > Approved leave > Manual > Inferred > Absent” and “manual records always win.” These cannot define one total order without a product interpretation. Proposed resolution in [ADR-004](../adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md): calendar eligibility and observed work are separate dimensions. A holiday/leave date retains evidence and can show work as an exception requiring HR review. Approved manual correction supersedes inferred session interpretation, not the legal/calendar meaning of leave or holiday. When manual evidence overlaps approved leave, record conflict and block automatic finalization; do not discard either fact. Until O02 is approved, implementations must not automatically resolve such conflicts or claim the legacy precedence is complete.

### 8.4 Check-in and check-out transactions

Check-in locks employee/day or equivalent guard, validates active projection and schedule, inserts evidence/session/outbox/idempotency result, and commits. It returns 201 after durable commit; Kafka/provider availability is not on the request path. If no active schedule exists, return `REFERENCE_NOT_READY` or an explicit business error, not guessed normal attendance. Check-out locks the open effective session, validates timestamp/version, appends evidence, updates its checkout/state/version, and writes outbox. Raw evidence is immutable; the current session row is a mutable projection with an auditable evidence trail. Corrections append a new session revision linked to the superseded session; old evidence remains. Daily status may be recomputed in the same owner transaction or by local asynchronous worker, but response identifies calculation state.

```text
sequenceDiagram
  participant C as Client
  participant G as Gateway
  participant A as Attendance
  participant D as Attendance DB
  participant K as Kafka
  C->>G: POST check-in + key + token
  G->>A: Verified request
  A->>D: Begin, lock, evidence + session + outbox + result
  D-->>A: Commit
  A-->>C: 201 session
  A->>K: Relay publishes after commit
```


```text
sequenceDiagram
  participant C as Client
  participant A as Attendance
  participant D as Attendance DB
  C->>A: POST check-out + key + If-Match
  A->>D: Lock open session, validate time and version
  A->>D: Evidence + checkout + outbox + key result
  D-->>A: Commit
  A-->>C: 200 closed session
```


### 8.5 Idempotency and duplicates

The same key and canonical body return the original status/body/resource within the retention window. The key scope is tenant + actor + method + canonical path; different payload is `409 IDEMPOTENCY_MISMATCH`. Concurrent duplicates serialize through the unique key and return stored success after first commit. A repeated check-in with a different key while already open returns `409 SESSION_ALREADY_OPEN` with safe own-resource reference. Retain command idempotency at least thirty days proposed, exceeding the proposed seven-day mobile retry horizon. After expiry, domain constraints still prevent duplicate open sessions; clients must reconcile history before resubmitting old commands.

```text
sequenceDiagram
  participant C as Client
  participant A as Attendance
  participant D as Attendance DB
  C->>A: Retry same check-in key and body
  A->>D: Read tenant actor route key and body hash
  D-->>A: Previously committed response
  A-->>C: Original 201 and same session ID
  Note over A,D: No new evidence or outbox event
```


### 8.6 Inference, confirmation, and reminders

Nudges are proposed at shift start +5 and +15 minutes if no effective check-in exists. A reminder dedupe key includes tenant/employee/work-date/kind/level. Geofence and Wi-Fi are nudge context, not automatic proof of presence. Inference aggregates permitted evidence asynchronously and creates a pending session with reason/source/confidence/model version. No ML call belongs in the manual check-in transaction. Employees confirm or reject the next day; rejection appends a decision and excludes inferred worked time. Pending inference is visible as pending and does not silently become finalized truth after a timeout. The next-day expiration/escalation policy remains O03.

```text
sequenceDiagram
  participant M as Mobile
  participant A as Attendance
  participant D as Attendance DB
  participant E as Evidence worker
  M->>A: Evidence batch with stable observation IDs
  A->>D: Persist evidence and outbox
  E->>D: Read permitted evidence and rules
  E->>D: Pending inferred revision + reason + outbox
  A-->>M: Next-day confirmation prompt through notification
  M->>A: Confirm or reject + key
  A->>D: Append decision, recompute, outbox
```


### 8.7 Corrections and finalization

Correction requests include proposed times, original session if present, reason, and expected version. Manager authority is checked at decision time; requester cannot self-approve. Decision action is immutable and uniquely tied to one pending correction version. Approval creates a replacement manual session revision and updates effective pointers in one transaction. An HR manual entry requires a reason and the same audit controls. Finalization freezes a revision for reports, not evidence ingestion forever. Late valid data creates a new revision/review event; reports show the new version and recalculation time. Historical edit windows and approval escalation are O03, not hard-coded assumptions.

```text
sequenceDiagram
  participant E as Employee
  participant A as Attendance
  participant M as Manager
  participant D as Attendance DB
  E->>A: Request correction with reason
  A->>D: Pending correction + audit + outbox
  M->>A: Approve + key + version
  A->>D: Authorize, lock, append decision and manual revision
  A->>D: Recompute daily status + audit + outbox
  A-->>M: 200 approved revision
```


### 8.8 Cutoff and late events

Cutoff is an attendance-owned scheduled job; durable key is tenant/employee/work-date/shift-revision/cutoff-kind. At cutoff, use local employee, assignment, holiday, leave, and session projections. If required projections are stale or missing, postpone and alert; missing data must not become ABSENT. No evidence on an eligible working day yields ABSENT only after the defined cutoff. An open session yields missing-checkout review rather than zero hours or guessed end time.

```text
sequenceDiagram
  participant S as Scheduler
  participant K as Kafka
  participant A as Attendance worker
  participant D as Attendance DB
  S->>K: time.shift-cutoff.v1
  K->>A: Employee day cutoff
  A->>D: Inbox + validate projections + calculate
  A->>D: Status revision + outbox, commit
  A->>K: Acknowledge after DB commit
```


```text
sequenceDiagram
  participant K as Kafka
  participant A as Attendance
  participant D as Attendance DB
  participant R as Reporting
  K->>A: Delayed leave or evidence event
  A->>D: Dedupe and check source revision
  A->>D: Append history, recompute or flag review
  A->>K: Revised daily-status snapshot
  K->>R: Higher revision
  R->>R: Upsert only if newer
```


## 9. Leave architecture

### 9.1 Policy and balance model

Leave owns types ANNUAL/SICK/SPECIAL/UNPAID, policies, balances, requests, expanded days, and immutable ledger. Special leave remains a leave type, not a general request type. A policy revision specifies annual entitlement, UPFRONT or MONTHLY accrual, carry-over cap, half-day support, and attachment requirements. MVP may use upfront allocation; monthly accrual and year-end automation require explicit policy fixtures before enabling. Each accrual/rollover job has an idempotent employee/type/year/period ledger key. Half-day AM/PM expansion is allowed only for one eligible date; multi-day half-day combinations are rejected initially. Expand dates using local working-day/holiday projections and store exact day portions with policy revision. Client `days` is not authoritative; the service computes chargeable units.

Reservation is made at PENDING creation to prevent concurrent overbooking. Lock balance rows in deterministic employee/type/year order across year boundaries. Validate overlap by employee/date/portion under an employee guard lock and active request constraints. Available balance cannot become negative unless an explicitly versioned policy permits an overdraft; default forbids it. Approval moves reserved units from pending to used; rejection/cancellation releases or reverses them exactly once. Ledger entries and materialized balances commit together; periodic reconciliation compares the two. Manual adjustments require reason, actor, and independent audit; no silent balance overwrite.

### 9.2 Saga state and compensation

APPROVED means leave-service approval and balance debit committed, not synchronous attendance acceptance. Publish a full request snapshot with request revision, employee ID, expanded days, and approval state. Attendance maintains leave references by request and day, not only a single employee/date row. Attendance detects overlapping effective manual work and emits a conflict referencing request ID and approval revision. Leave changes APPROVED → NEEDS_REVIEW only if conflict matches the current applicable revision. NEEDS_REVIEW retains the debit and approved attendance overlay with an exception marker until HR resolves it. This conservative proposal avoids silently refunding leave or deleting attendance; product approval O02 is required. Resolution keeps leave, cancels affected portions with a compensating ledger credit, or corrects attendance with its own audit action. A cancellation emits a newer full snapshot; attendance removes only that request's overlay and recomputes the day. Do not automatically reapprove a NEEDS_REVIEW request on duplicate approval input.

```text
sequenceDiagram
  participant L as Leave
  participant LD as Leave DB
  participant K as Kafka
  participant A as Attendance
  L->>LD: Approve, reserve to used, audit, outbox
  L->>K: leave.request.v1 APPROVED revision 3
  K->>A: Approved days
  A->>A: Inbox + leave refs + conflict check
  A->>K: attendance.leave-conflict.v1 references revision 3
  K->>L: Conflict
  L->>LD: If still revision 3: NEEDS_REVIEW + audit + outbox
```


### 9.3 Failure and reconciliation

Duplicates are inbox-deduplicated; transitions also require expected state/revision. Out-of-order snapshots below stored revision are recorded as consumed but do not overwrite state. Conflict events after cancellation are recorded as stale, not used to resurrect NEEDS_REVIEW. Use revision comparisons rather than occurrence time for these decisions. Kafka downtime leaves approval durable with pending propagation; response exposes `syncState`. A reconciliation job compares approval revision against attendance-applied revision using event-based snapshots/acknowledgments if required. No direct database comparison across schemas or HTTP repair call is allowed. If replay retention is insufficient, export authorized owner snapshots to a controlled rebuild stream. Expose unresolved conflicts and aging propagation to HR; manual replay is audited.

## 10. General requests and approvals

Request owns MATERIAL/CERTIFICATE/OVERTIME/OTHER type forms and approval chains. Expense was mentioned in the original description but lacks a defined policy; represent it as a proposed type, not a reimbursement subsystem. Type forms use a bounded JSON schema subset; reject unknown/oversized fields and arbitrary executable template content. On submission, snapshot the form schema, chain, requester hierarchy, and resolved approver IDs. Revalidate each decision against the current assigned approver and employment state; reassignment is an audited HR action. One transaction locks request/step, checks version and current_step, records decision, advances chain, and writes outbox. Concurrent decisions cannot both succeed; the loser receives 409 or a replay of the same idempotent result. Rejection requires note; terminal transitions cannot be undone by PATCH. Cancellation before final decision cancels waiting steps; approved fulfillment cancellation requires a future explicit policy. Material stock is advisory, matching the original draft: approval does not reserve physical inventory. Fulfillment records what was handed over; authoritative inventory would need a separate reservation/compensation design.

Comments are participant-only and append-only; sanitize when rendering. Attachments use pending metadata, private object keys, content/size allowlists, and a quarantine/validation step. Presign upload requires requester ownership and editable state; no client-selected bucket or arbitrary object key. New proposed attachment completion and download endpoints close the original upload-only contract gap. Download requires current request participation or HR authority and a short-lived signed URL. Provider callback or client completion never bypasses size/type verification and malware policy. Do not place sensitive attachment data in Kafka payloads or push text.

## 11. Notification architecture

Consume committed business events, resolve recipients from local employee projections, and persist notifications with inbox in one DB transaction. A uniqueness key of tenant/source-event/recipient/category prevents duplicate logical notifications during replay. Provider work is a separate durable delivery queue; Kafka offsets do not wait for FCM/email. Template lookup uses event category, tenant, locale, channel, and template revision. Locale fallback proposal: employee preference → tenant locale → English; Khmer content needs reviewed translations. User preferences affect optional channels; mandatory security messages have an explicitly documented exception. Register tokens only for the authenticated employee; redact provider tokens from logs and encrypt them at rest. Invalid/unregistered provider responses deactivate the token; transient errors schedule bounded retries with jitter. Delivery attempt states are PENDING/PROVIDER_ACCEPTED/FAILED/UNKNOWN, not a misleading universal SENT. A provider timeout may have delivered: retries can duplicate external notifications; use provider idempotency if supported. No delivery receipt means “provider accepted,” not “device displayed” or “employee read.” Only explicit app read actions set `read_at`.

### 11.1 SSE

SSE is a notification-service endpoint behind the gateway, authenticated using a bearer-capable streaming client. Browser native EventSource does not conveniently add Authorization; prefer fetch streaming or a reviewed same-origin cookie strategy. Never put long-lived bearer tokens in query strings. Stream messages contain notification ID/category and bounded routing data, not sensitive HR content. Use durable notification sequence/cursor IDs; `Last-Event-ID` replays only the authenticated employee's retained records. If cursor expired, send reset instruction and client refetches inbox/dashboard. Disconnect does not lose the inbox record; SSE is an accelerator, not a delivery guarantee. Heartbeats keep proxies alive; client reconnects with jitter and refreshes authentication on expiry. Bound connections and buffers; slow consumers reconnect rather than consuming unlimited server memory. Preserve per-recipient sequence, not global Kafka ordering across all topics.

## 12. Reporting and read models

Reporting owns employee dimensions, attendance/leave/request facts, daily summaries, and export jobs. It never joins transactional service schemas. Snapshot events upsert only when `aggregateVersion` exceeds the stored source revision. If a newer record arrives first, the older record is consumed as stale without reverting it. Employee dimensions include names/department labels required for authorized display. Historical department reporting uses event-time dimension snapshots or effective dates; do not silently relabel past work after a transfer. `leave.balance.v1` is a proposed additional fact needed for the existing balance-report endpoint. A projection transaction updates fact, affected summary, inbox, and watermark together. Summaries must subtract prior contribution before adding a new one, or be recomputed from facts transactionally.

Queries filter by authenticated tenant and HR/team scope before aggregation and pagination. Manager scope changes invalidate cached dashboards immediately or fail closed when projection is too stale. Responses include `asOf`, `projectionLagMs`, and `isStale`; proposal marks stale after ten seconds of known unapplied source work in pilot, calibrated by tests. Idle topics do not imply stale data: combine partition high-water marks/consumer progress with heartbeat/checkpoint age; last business event age alone is not projection lag. Exports capture filters, authorization scope, requested revision/time, format, and requester. CSV is MVP; XLSX is should-have and must not block core delivery. Escape CSV formula-leading fields; bound rows and date range. Small reports can stream; larger exports return 202 with a queryable job. Workers write private objects, then atomically mark READY with retention timestamp. Status/download calls reauthorize ownership and current employment/role; object URL lifetime proposal is five minutes. Expired or failed jobs remain observable without exposing storage internals.

Rebuild into shadow tables with a distinct consumer group and publication suppressed. Use complete retained snapshots plus subsequent events, then validate counts/checksums and switch views. Do not replay into live notifications/provider jobs. A retention gap requires an owner snapshot export, not pretending Kafka is a permanent event store. Record rebuild source offsets, time, schema version, and operator.

## 13. Kafka and event contracts

### 13.1 Delivery and envelope

Canonical catalog and payloads live in [event contracts](../REST-API-DATABASE-DESIGN.md). The existing flat Avro check-in record is legacy prototype evidence, not the generic envelope implementation. Proposed envelope fields: `eventId`, `type`, `schemaVersion`, `tenantId`, `aggregateId`, `aggregateVersion`, `occurredAt`, `correlationId`, `causationId`, `payload`. Event IDs remain stable across relay/retry/replay attempts; causation identifies the command/event that triggered the fact. A topic is a domain-owned stream, and `type` is a past-tense fact within it. Version suffix `v1` is wire contract major version, not entity revision. Payloads for projection streams are full aggregate snapshots; evidence and audit facts are append-only observations.

```json
{
  "eventId": "019a0000-0000-7000-8000-000000000001",
  "type": "attendance.session.checked-in.v1",
  "schemaVersion": 1,
  "tenantId": "019a0000-0000-7000-8000-000000000010",
  "aggregateId": "019a0000-0000-7000-8000-000000000030",
  "aggregateVersion": 1,
  "occurredAt": "2026-10-22T01:00:00Z",
  "correlationId": "019a0000-0000-7000-8000-000000000040",
  "causationId": null,
  "payload": {
    "sessionId": "019a0000-0000-7000-8000-000000000030",
    "employeeId": "019a0000-0000-7000-8000-000000000020",
    "workDate": "2026-10-22",
    "checkInAt": "2026-10-22T01:00:00Z",
    "checkOutAt": null,
    "source": "TAP",
    "confirmStatus": "NONE",
    "confidence": null,
    "reason": null,
    "effective": true,
    "version": 1,
    "supersedesId": null,
    "modelVersion": null
  }
}
```


This is a proposed JSON view; Avro wire serialization remains under ADR-003 review. Do not publish this envelope onto the legacy flat record schema without an explicit migration.

### 13.2 Partitioning and compaction

| Stream | Key | Reason |
|---|---|---|
| org.employee.v1 | tenantId:employeeId | Latest full employee including assignment snapshot |
| org.shift.v1 | tenantId:shiftId | Separate shifts must not compact each other away |
| org.holiday.v1 | tenantId:holidayId | Stable identity for correction/removal |
| attendance evidence/session/status/cutoff | tenantId:employeeId | Preserve per-employee processing within each topic |
| leave.request.v1 | tenantId:requestId | Approval/cancellation order per request |
| leave.balance.v1 proposed | tenantId:employeeId:leaveTypeId:year | Latest balance snapshot |
| request.workflow.v1 | tenantId:requestId | Workflow ordering per request |
| attendance.leave-conflict.v1 | tenantId:leaveRequestId | Conflict sequencing; compare referenced approval revision |

Kafka order is per partition, not global and not across topics. Employee snapshots contain all effective assignments; independent assignment records cannot share a compacted employee key unless they are a complete replacement snapshot. Each consumer group has independent offsets; use stable groups such as `attendance-org-v1` and `reporting-attendance-v1`. Concurrency cannot exceed assigned partitions usefully; sequentially process each aggregate or guard revision in DB. Increasing partitions can remap keys and break assumptions during migration; drain/cut over deliberately. Six partitions in the current script is a local setting, not a measured production sizing decision.

### 13.3 Retention, replay, and compatibility

Org topics use compaction with full latest snapshots and explicit `deletedAt`/inactive records. A Kafka tombstone is a later retention action, not the only business deletion signal. Proposed initial fact retention is thirty days subject to privacy/storage review; owner databases hold durable business history. Inbox dedupe retention must cover the largest supported replay horizon; archive event IDs or restrict replay to shadow projections after cleanup. Additive optional fields with defaults are preferred; changing meaning/type or removing required fields requires a new major contract and consumer migration. Compatibility policy proposal is backward-transitive within a major version, with fixtures for old readers/new writers and new readers/old writers as required by rollout direction. Schema Registry is present in Compose but not functional integration; do not claim compatibility enforcement exists. Avro enum extension can break older readers without a suitable default; test it explicitly.

### 13.4 Retry, DLQ, and poison events

Transient DB/network failure rolls back consumer work and leaves offset unacknowledged. Proposed in-partition bounded retry: exponential jitter from 1 to 30 seconds, maximum five attempts before quarantine. Moving to retry topics can reorder later events; use revision guards and an aggregate block/reconciliation policy for state machines. A DLQ is per source topic and consumer group, with event ID, original bytes/schema ID, failure code, attempt count, and safe diagnostic metadata. Commit source offset only after durable quarantine publication/record succeeds; broker failure cannot silently discard poison data. Malformed envelopes with unknown tenant are quarantined outside tenant business processing and access-restricted. DLQ replay requires authorization, corrected cause, original event ID, and an audit record. Do not automatically loop a poison record forever or consider DLQ placement successful business processing. Alerts distinguish consumer lag from quarantined business work.

### 13.5 Broker security and observability

Production uses private listeners, TLS, authenticated service principals, and least-privilege topic/group ACLs. Only the owner writes authoritative domain topics; consumers cannot forge org or leave facts. Record event ID, correlation/causation, type, source topic/partition/offset, aggregate revision, and processing outcome. Do not put raw coordinates, tokens, reasons, or attachments in metric labels or unrestricted error logs. Track publish latency, retry count, consumer lag, quarantine count, and source-to-projection age. Single local KRaft broker is not a high-availability production claim.

## 14. Transactional outbox and inbox

### 14.1 Guarantee and dual-write boundary

A direct DB write followed by Kafka send can lose publication if the process crashes between them. A send followed by DB commit can publish a fact whose transaction later fails. The outbox inserts durable publication intent with the business mutation in the same PostgreSQL transaction. This gives atomic local intent and eventual at-least-once publication, not a PostgreSQL/Kafka distributed transaction. The outbox includes owner aggregate revision/key, immutable serialized payload/schema metadata, state, next attempt, lease owner/expiry, attempts, and last safe error. Critical DDL is in [shared persistence](../REST-API-DATABASE-DESIGN.md).

### 14.2 Relay algorithm

1. Select eligible aggregate heads with `FOR UPDATE SKIP LOCKED` in a short claim transaction.
1. Claim with a bounded lease/token and commit; do not hold DB row locks over unbounded broker I/O.
1. Preserve publication order for each aggregate; no later unpublished revision may bypass a claimed/retrying predecessor.
1. Serialize/validate before send; invalid schema is quarantined and alerts owner.
1. Publish with durable broker acknowledgments and producer idempotence configured where supported.
1. After acknowledgment, mark published only if lease token still owns the row.
1. On failure, release/reschedule with attempts/error; expired leases are reclaimable.
1. Bound batches and publishing deadlines; monitor oldest pending age and blocked aggregates.

`SKIP LOCKED` alone does not preserve aggregate order: workers must claim only each aggregate's earliest unpublished row. A failed head can block that aggregate without blocking unrelated employees. If a lease expires while a send completes, duplicates or late deliveries can occur; consumer revisions remain the correctness backstop. A crash after broker acknowledgment and before marking published causes republish with the same event ID. A successful HTTP command remains committed when Kafka is down; outbox grows and operations alert. Admission control under disk/backlog pressure must return a clear retryable error before commit, never claim success and drop intent.

### 14.3 Consumer transaction

1. Validate schema, producer/topic identity, tenant, and aggregate reference.
1. Begin a local DB transaction.
1. Insert inbox `(consumer_name, tenant_id, event_id)` using a unique constraint.
1. On duplicate, no business changes; commit and acknowledge.
1. Apply a revision-guarded snapshot or a valid state transition.
1. Write derived outbox facts in the same transaction when needed.
1. Commit DB, then acknowledge Kafka offset.

A crash before DB commit reprocesses safely; a crash after commit before offset acknowledgment hits the inbox on retry. Never acknowledge offsets before the transaction commits. For batch consumers, commit offsets only through the highest contiguous successfully handled offset per partition. Provider delivery is outside the consumer DB transaction and has its own durable attempt state. Inbox uniqueness and domain constraints complement each other; neither is replaced by Kafka producer idempotence.

### 14.4 Cleanup and recovery

Delete published outbox payloads only after retention and replay/audit requirements are met. Do not clean pending/quarantined rows as normal retention. Inbox cleanup must account for offset resets; an old event may otherwise produce effects again. Shadow rebuilds use separate consumer identities and suppress side effects. Operational runbook records source offsets, oldest intent, per-aggregate blockage, and reconcile results.

## 15. PostgreSQL architecture

One PostgreSQL database hosts `identity_org`, `attendance`, `leave`, `request`, `notification`, and `reporting` schemas. Service ownership is logical and permission-enforced, not a physical availability boundary. No cross-service foreign keys, joins, triggers, or shared mutable tables. Foreign IDs are UUID references validated from local projections. Tenant-scoped composite foreign keys prevent accidental same-service cross-tenant associations. Master data is soft-deleted where history needs stable IDs; evidence/ledger/audit are append-only under normal roles. Mutable resources carry `version`; immutable facts do not need meaningless `updated_at` columns. Use `timestamptz` for instants, `date` for work dates, `time` plus IANA zone for templates, and `numeric` for leave units. UTC storage does not eliminate the need for explicit shift-local calendar semantics.

Default transaction isolation proposal is READ COMMITTED with explicit row locks for commands and optimistic versions for user edits. Use unique/check constraints for final integrity, not only preflight application checks. Lock employee guard before balance/request/session rows in a consistent order; bounded deadlocks retry only with the same idempotency key. Indexes follow tenant-scoped list/query patterns and relay due-work access. Avoid premature monthly evidence partitioning: measure volume and retention cost first; partitioning complicates global unique constraints.

Flyway owns service migrations; runtime Hibernate uses validation, not automatic DDL. A separate deployment job applies additive schema before new code; avoid all replicas racing long migrations. Expand-contract migrations preserve old/new binary compatibility during rollout. Destructive migration waits until old readers/writers and backfill are complete. Rollback application images only if schema remains compatible; prefer forward repair over blind down-migration. Per-service pool maxima must sum below DB connection budget with reserve for migrations/operations. Set statement/lock timeouts and export limits so reports cannot exhaust transactional pools.

Backups include database, object metadata, configuration/key references, and broker recovery strategy. Encrypt storage and transport; restrict backup access and exercise restoration into an isolated environment. Point-in-time recovery capability depends on the chosen provider/configuration; it is not currently implemented. Retention is category-specific and tenant/legal-policy aware; exact periods are O05. Controlled erasure minimizes or pseudonymizes personal data while preserving legally required audit facts.

## 16. API standards and concurrency

[The contract document](../REST-API-DATABASE-DESIGN.md) is authoritative for paths, schemas, and examples. Use `/api/v1`, plural kebab-case resources, camelCase JSON, UUID identifiers, and ISO-8601 UTC instants. Responses from durable synchronous creates use 201 with Location; command transitions use 200; deletes can use 204. Use 202 only for queued jobs with a durable job/status resource, such as exports and year-end processing. Publishing an event asynchronously does not make the local write a 202 operation. Lists return bounded `items/page/size/total` plus freshness metadata when projected. Sort/filter allowlists prevent SQL field injection and unbounded queries. Validation returns field paths and stable error codes without personal data.

Mutating POSTs use idempotency except login/refresh with their explicitly different credential semantics. Mutable PATCH/PUT/DELETE and decision POSTs use If-Match version preconditions. Missing precondition returns 428; stale version returns 412; invalid business transition returns 409. PATCH is a typed partial update, not arbitrary entity binding. Date interval limits and evidence payload caps are validated server-side. API evolution is additive within v1; incompatible changes get a new major route and migration window. OpenAPI/AsyncAPI do not exist yet; Markdown plus bounded JSON examples is current design source. Week 1 creates mockable machine contracts for the first slice; later controllers must contract-test against them. Do not maintain generated and handwritten definitions with conflicting authority.

## 17. Security threat model

| Threat | Control | Verification |
|---|---|---|
| Cross-tenant ID substitution | Token tenant + scoped repositories + composite constraints | Tenant B resource IDs through every read/write/export |
| Broken object authorization | Owner/direct-report/assigned-step checks | Manager unrelated employee and self-approval cases |
| Forged internal headers | Strip and independently verify JWT | Send spoofed X-Tenant/Employee/Roles |
| Refresh theft/replay | Hash, rotation family lock, reuse revocation | Concurrent and reused refresh tests |
| Privilege escalation | Typed DTOs, explicit grants, platform separation | Mass-assignment/role injection tests |
| Kafka event forgery | Topic ACL and envelope validation | Wrong producer/topic and cross-tenant refs |
| Duplicate approvals | State/version/key locks | Parallel decision requests |
| Attachment abuse | Private keys, type/size verification, quarantine | Cross-tenant key, polyglot/oversize files |
| Export leakage | Query scope + job ownership + URL expiry | Role revoked between create and download |
| Location surveillance | Consent, minimal observations, retention | Permission denial and erasure checks |
| Secret exposure | Secret store, redaction, no tokens in URLs/logs | Log review and repository scanning |
| Denial of service | Rate/body/page/connection limits | Large payload and slow SSE clients |

Sensitive audit captures who/what/when/reason and safe before/after fields; omit password/token/raw location. Audit writers have insert-only permissions; privileged retention operations are separately logged. Production launch requires secret rotation procedures, dependency/image scanning, TLS, least privilege, and a documented incident owner. This is an engineering threat model, not a claim of regulatory certification.

## 18. Privacy and evidence governance

Consent is specific to location/Wi-Fi/BLE collection and can be refused without breaking explicit check-in where policy permits. Explain purpose, fields, retention, recipients, and the correction process before collection. Collect events around permitted attendance actions/nudges; continuous GPS tracking remains out of scope. Wi-Fi presence and geofence proximity can be spoofed or reflect a phone left at work. Evidence confidence is uncertain, not a disciplinary conclusion. Managers see necessary attendance exceptions, not unrestricted raw location history. Raw evidence access is HR-restricted and audited; reports use derived fields. No biometric template is sent to HRFlow as part of local device biometric confirmation. Camera/face evidence is disabled unless a separate necessity, consent/legal, security, bias, and retention review approves it. Data subject export/erasure procedures and legal retention periods remain O05 before real pilot data. Use synthetic demo data until those decisions are resolved.

## 19. Web and mobile architecture

### 19.1 Web

Current Nuxt/Vue source is a welcome page; React/Tailwind/shadcn were previous plans, not installed features. Proposed MVP keeps Nuxt to avoid a framework rewrite; O01 requires team agreement. If React is chosen, record migration before building features; keep REST contracts unchanged. Use feature folders for auth, organization, attendance, leave, requests, notifications, and reports. One typed API client handles bearer tokens, correlation, errors, If-Match, and idempotency. Role-based navigation improves usability; service authorization remains authoritative. Server pagination and URL filters prevent downloading whole-tenant datasets. Forms validate early but repeat constraints on the server. Handle empty/loading/stale/permission-denied states explicitly; distinguish retryable failures from business rejection. SSE triggers narrow query invalidation; reconnect refetches canonical state. EN/KH localization is should-have; format dates in selected locale and preserve work-site timezone labels. Keyboard navigation, focus, accessible validation messages, and adequate contrast are release requirements.

### 19.2 Flutter

No Flutter repository is verified here; B must confirm location/build instructions in week 1. Feature modules mirror stable contracts, with shared auth/network/storage layers. Secure token storage uses platform facilities; access refresh is serialized and logout clears credentials. Offline queue stores command UUID/key, canonical payload, timestamp, route, and retry status atomically. Retries preserve the original key/body; edited payload becomes a new command after user reconciliation. Proposed automatic retry horizon is seven days; queue older commands for review instead of silent resubmission. 401 refreshes once, 429 honors Retry-After, 5xx/network errors use bounded jitter, 409/412 require reconciliation. A network timeout after commit is not proof of failure; query or repeat the same key. Device binding and push token registration are distinct; logout unregisters the current token when possible. Local biometric unlock is a device access control, not a replacement for server authentication. Permission denial uses explicit check-in fallback; background execution is best-effort and platform-constrained. Nudge delivery timing is measured on real devices, not promised by server schedules alone.

## 20. Infrastructure and deployment

### 20.1 Environments

| Environment | Purpose | Data and topology |
|---|---|---|
| Local | Feature development and reproducible dependencies | Compose, synthetic data, host apps |
| Test | Automated integration/contract checks | Ephemeral PostgreSQL/Kafka, no production secrets |
| Staging | Release rehearsal and restore/load tests | Production-like config, anonymized/synthetic data |
| Pilot production | Limited approved users | Selected container target, protected DB/broker/storage |
| Future scaled production | Availability/capacity-driven evolution | Kubernetes/multi-broker only when justified |

No environment beyond local configuration is implemented in the repository. The confidence Dockerfile exists; Java/web Dockerfiles do not.

### 20.2 Proposed delivery baseline

Build immutable non-root containers with pinned base images and health commands. Use a registry such as ECR only if AWS is selected; store image digests and provenance. Restrict public ingress to TLS gateway/web; database, Kafka, Registry, and metrics remain private. Use least-privilege IAM roles for object storage and deployment; avoid static cloud keys in images. S3 attachments/exports use private buckets, encryption, lifecycle rules, and tenant-prefixed keys. RDS is a proposal for managed PostgreSQL, not existing provisioning. Network/security-group boundaries prevent services reaching unauthorized stores where practical. Terraform is planned; remote state needs encryption, locking, access control, and reviewed plan/apply separation.

A single host can demonstrate deployment at lower operational cost but remains a shared failure domain. k3s adds cluster lifecycle/storage responsibilities; EKS adds control-plane/network/cost overhead. Select target using measured resource demand, operational skill, restore requirements, and actual cost forecast. Retain the original `$`30 budget alert as a warning threshold, not a claim that EKS/RDS fit within it.

### 20.3 Runtime safeguards

Configure CPU/memory limits and bounded JVM heaps/pools based on measured pilot usage. Liveness must not restart healthy processes solely because Kafka/provider is unavailable. Readiness reflects ability to accept durable commands; dependency degradation is separately visible. Graceful shutdown stops admission, drains HTTP within deadline, finishes/rolls back DB transactions, and commits only safe offsets. Outbox leases expire after a killed worker; jobs resume idempotently. Rolling deployment keeps old/new event readers compatible. Rollback records image digest, schema compatibility, config revision, and smoke results. Persistent volumes are not backups; broker/DB data durability and host-loss recovery must be tested.

## 21. CI/CD and software delivery

Current `.github/workflows` is empty; the following pipeline is planned. PR checks: formatting/static analysis, documentation validation, unit/domain tests, schema/example validation, dependency scan. Integration stage: service migrations on clean PostgreSQL, Kafka contracts, duplicate/order/tenant tests. Build stage: Java 21 compilation/package, locked web build, pinned confidence environment if included. Image stage: scan and publish immutable commit/digest tags; no mutable `latest` promotion as release identity. Staging stage: expand migrations, deploy, readiness checks, first-slice smoke, event propagation, rollback rehearsal. Production stage: reviewed release decision, separate credentials, same tested images, smoke and monitored rollout. Terraform plan attaches to review; apply uses protected environment identity and explicit authorization. Do not pass untrusted fork code production secrets. Protect main branch and require reviewer/required-check policy appropriate to two contributors. Artifact signing/provenance is a launch recommendation; do not claim it exists.

## 22. Observability and operations

Structured logs include service/environment/version, correlation, event ID where relevant, safe tenant ID, outcome, duration. Propagate correlation/causation through outbox and consumers; distributed tracing is later unless debugging justifies it. Actuator dependency exists, but endpoint exposure/configuration and Prometheus registry wiring are incomplete. Do not expose management endpoints publicly or include sensitive environment properties in health.

| Signal | Interpretation | Initial response |
|---|---|---|
| Oldest unpublished outbox age | Durable facts not reaching broker | Check broker/serialization/blocked aggregate |
| Consumer lag + event age | Backlog or poisoned partition | Compare throughput, errors, quarantines |
| Inbox duplicate count | Retry/replay activity | Investigate surge, not every duplicate |
| Projection watermark age | Dashboard stale | Mark stale, recover/reconcile consumer |
| Balance ledger mismatch | Financial-like leave inconsistency | Stop affected transitions, reconcile audited |
| Provider failure rate | External delivery degraded | Retry queue, disable invalid tokens, retain inbox |
| DB pool wait/lock time | Saturation or contention | Query/transaction inspection; avoid blind pool increase |
| 401/403/429 anomalies | Abuse or auth/config issue | Correlate safely, review limits/key rollout |
| Export queue age | Worker/storage issue | Status remains observable; retry idempotently |

Initial alerts are sustained/backlog-based and calibrated in staging; no invented paging thresholds are treated as established SLOs. Dashboards show HTTP latency/error, DB contention, broker lag, outbox age, projection freshness, and job failures. Incident notes capture timeline, affected tenants, last safe offsets/revisions, recovery action, and verification. A and B designate an operator/backup before pilot; no unattended production guarantee is assumed.
