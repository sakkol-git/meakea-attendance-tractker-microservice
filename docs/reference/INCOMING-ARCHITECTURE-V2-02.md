# Approved architecture detail 02

> **Decision:** This is part of the latest user-approved final system design. It is authoritative design intent, not evidence of implemented behavior. Read the [canonical Architecture index](../Architecture.md) for supersession, security limits and deployment constraints. Cloud chapters remain the approved future topology; the latest user decision defers their execution beyond this twelve-week local finish line.

## 9. Service catalogue

| Service            | Port | Schema            | Owns                                             | Publishes                                                                                                                                  | Consumes                                                                                   |
| ------------------ | ---- | ----------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------ |
| api-gateway        | 8080 | none              | Routing, auth enforcement                        | none                                                                                                                                       | none                                                                                       |
| identity-org       | 8101 | `identity`        | Tenants, users, employees, org, shifts, holidays | `org.employee.v1`, `org.shift.v1`, `org.holiday.v1`                                                                                        | none                                                                                       |
| attendance         | 8102 | `attendance`      | Evidence, sessions, corrections, daily status    | `attendance.evidence.v1`, `attendance.session.v1`, `attendance.daily-status.v1`, `time.shift-cutoff.v1`, `attendance.scoring-requested.v1` | `org.*`, `leave.request.v1`, `time.shift-cutoff.v1`, `attendance.confidence.v1`            |
| leave              | 8103 | `leave`           | Leave types, balances, requests                  | `leave.request.v1`                                                                                                                         | `org.employee.v1`, `org.holiday.v1`                                                        |
| request            | 8104 | `request`         | Requests, approval steps, material catalogue     | `request.workflow.v1`                                                                                                                      | `org.employee.v1`                                                                          |
| notification       | 8105 | `notification`    | Templates, preferences, device tokens, inbox     | `notification.sent.v1` (optional)                                                                                                          | `attendance.*`, `leave.request.v1`, `request.workflow.v1`, `org.employee.v1`               |
| reporting          | 8106 | `reporting`       | Fact tables, exports                             | none                                                                                                                                       | `org.employee.v1`, `attendance.daily-status.v1`, `leave.request.v1`, `request.workflow.v1` |
| confidence-service | 8107 | none (file model) | Model and thresholds                             | `attendance.confidence.v1`                                                                                                                 | `attendance.scoring-requested.v1`, `attendance.session.v1` (labels)                        |

The detailed table and endpoint lists are in _HRFlow – REST API & Core Database Design_.



## 10. Event-driven backbone

### 10.1 Topic catalogue

| Topic                             | Key                   | Partitions (dev / prod suggestion) | Retention        | Purpose                                  |
| --------------------------------- | --------------------- | ---------------------------------- | ---------------- | ---------------------------------------- |
| `org.employee.v1`                 | `tenantId:employeeId` | 3 / 6                              | compacted + 30 d | Employee master data                     |
| `org.shift.v1`                    | `tenantId:shiftId`    | 3 / 3                              | compacted        | Shift definitions and assignments        |
| `org.holiday.v1`                  | `tenantId:date`       | 1 / 3                              | compacted        | Holidays                                 |
| `attendance.evidence.v1`          | `tenantId:employeeId` | 6 / 12                             | 14 d             | Raw presence signals                     |
| `attendance.session.v1`           | `tenantId:employeeId` | 6 / 12                             | 30 d             | Check-in, check-out, inferred, confirmed |
| `attendance.daily-status.v1`      | `tenantId:employeeId` | 6 / 12                             | 30 d             | Final status per day                     |
| `attendance.scoring-requested.v1` | `tenantId:employeeId` | 3 / 6                              | 3 d              | Feature snapshot for the ML worker       |
| `attendance.confidence.v1`        | `tenantId:employeeId` | 3 / 6                              | 3 d              | Model score and decision                 |
| `leave.request.v1`                | `tenantId:employeeId` | 3 / 6                              | 30 d             | Leave lifecycle                          |
| `request.workflow.v1`             | `tenantId:requestId`  | 3 / 6                              | 30 d             | Request lifecycle                        |
| `time.shift-cutoff.v1`            | `tenantId:employeeId` | 3 / 6                              | 3 d              | "The shift window has closed" trigger    |
| `<topic>.retry`                   | same                  | same                               | 3 d              | Delayed retry                            |
| `<topic>.dlq`                     | same                  | 1 / 1                              | 14 d             | Poison messages                          |

Compacted topics let a new or rebuilt service replay the latest state of every key (a **bootstrap** of its read model).

The two scoring topics are a **proposal** that extends the earlier plan; they are not yet in the API document.

### 10.2 Why this partition key

`tenantId:employeeId` guarantees that all events about one employee are processed **in order** by one consumer instance, which matters for check-in then check-out and for inferred then confirmed. It also spreads a large tenant across partitions.

Trade-off: a very hot employee cannot be parallelised; this is acceptable because one person generates few events.

### 10.3 Event envelope

Every event carries the same header fields followed by a payload:

| Field           | Type             | Meaning                                               |
| --------------- | ---------------- | ----------------------------------------------------- |
| `eventId`       | uuid             | Unique per event; the idempotency key                 |
| `type`          | string           | `AttendanceCheckedIn.v1` (name + major version)       |
| `tenantId`      | string           | Tenant                                                |
| `aggregateId`   | string           | Entity the event is about                             |
| `occurredAt`    | timestamp-millis | When it happened (business time)                      |
| `correlationId` | string           | Carried from the originating HTTP request for tracing |
| `causationId`   | string, nullable | The event that caused this one                        |
| payload fields  | typed            | Specific to the event                                 |

In Avro, each event is its own record containing these header fields; consumers do not parse a generic envelope.

### 10.4 Avro and Schema Registry

| Topic             | Decision                                                                                      |
| ----------------- | --------------------------------------------------------------------------------------------- |
| Subject naming    | `TopicNameStrategy`: subject `<topic>-value`                                                  |
| Compatibility     | `BACKWARD` by default (new schema can read old data), set globally and per subject            |
| Registration      | CI registers schemas; services run with `auto.register.schemas=false` in non-dev environments |
| Wire format       | Magic byte + 4-byte schema id + Avro binary                                                   |
| Generated classes | `avro-maven-plugin` in `hrflow-contracts`, `stringType=String`                                |
| Logical types     | `uuid`, `timestamp-millis`, `date`                                                            |
| Python            | `confluent-kafka` with the Avro serializer reading the same `.avsc`                           |

### 10.5 Schema evolution rules

| Change                | Allowed?                                     | How                                                                          |
| --------------------- | -------------------------------------------- | ---------------------------------------------------------------------------- |
| Add an optional field | Yes                                          | Give it a default (`null` union or value)                                    |
| Remove a field        | Yes under BACKWARD, only if it had a default | Prefer deprecating first                                                     |
| Rename a field        | Avoid                                        | Use `aliases` or add new + deprecate old                                     |
| Change a type         | No                                           | New major version of the event                                               |
| Add an enum symbol    | Dangerous                                    | Old readers break unless the enum has a `default` symbol; always declare one |
| Breaking change       | Major version                                | Publish to `...v2` topic, run both during migration, retire `v1`             |

CI step: `mvn -pl libs/hrflow-contracts verify` plus a compatibility check against the registry before merging any schema change.

### 10.6 Transactional outbox

```text
HTTP request ──► [ business table write  +  outbox insert ]  (one DB transaction)
                                │
            relay (every ~200 ms, @Scheduled)
                                │ SELECT ... WHERE published_at IS NULL
                                │ ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED
                                ▼
                         Kafka send (wait for ack)
                                │
                  UPDATE outbox SET published_at = now()
```


| Aspect               | Decision                                                                                                                |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Table                | `outbox(id uuid, topic, event_key, payload bytea, created_at, published_at)` — `bytea` holds the Avro wire-format bytes |
| Serialisation moment | At insert time (inside the business transaction) using `KafkaAvroSerializer`                                            |
| Ordering             | Relay orders by `created_at`; per-key order holds because one row per event is sent in order within a batch             |
| Multiple instances   | `SKIP LOCKED` lets several relays run safely; strict per-key ordering across instances needs care (see 24.4)            |
| Delivery guarantee   | At-least-once (duplicates possible if the relay crashes after send before marking)                                      |
| Cleanup              | Nightly delete of rows published more than one day ago                                                                  |
| Poll vs CDC          | Polling chosen for simplicity; Debezium is a later optimisation                                                         |

### 10.7 Inbox (idempotent consumer)

```text
consume(event):
  begin transaction
    inserted = INSERT INTO inbox(consumer, event_id) ON CONFLICT DO NOTHING
    if inserted == 0: commit, skip            -- duplicate
    apply the change to the local read model / aggregate
  commit
  acknowledge the Kafka offset
```


The inbox insert and the business change share one transaction, so a crash between them cannot lose or double-apply an event.

### 10.8 Retry and dead-letter strategy

| Failure                                      | Handling                                                                                      |
| -------------------------------------------- | --------------------------------------------------------------------------------------------- |
| Transient (DB timeout, deadlock)             | In-process retry 3 times with exponential backoff                                             |
| Still failing                                | Publish to `<topic>.retry` with a delay header; a retry consumer re-processes after the delay |
| Deserialisation or validation error (poison) | Straight to `<topic>.dlq` with the error and original bytes                                   |
| DLQ                                          | Alert; an admin endpoint or script can replay messages after a fix                            |
| Consumer lag                                 | Alert when lag stays above a threshold for 5 minutes                                          |

### 10.9 Read-model bootstrap

When a service starts with an empty read model (new service, wiped database):

1. Reset the consumer group offsets to the beginning of the compacted topics.
1. Consume until caught up (lag zero).
1. Mark the service ready (`/actuator/health/readiness` stays DOWN until bootstrapped).

### 10.10 Time-based triggers

Absence and "leave early" are the **absence of events**, so something must create an event at a given time. A scheduler in attendance emits `time.shift-cutoff.v1` per employee when the shift window (shift end + buffer) closes, derived from the local `shift_ref` and the tenant timezone. The attendance consumer of that event finalises the daily status. This keeps the rule "no cron job touches another service's data".



## 11. Attendance domain (deep dive)

### 11.1 Concept: tap first, infer if forgotten

```text
              Lane A  (primary, cheap, reliable)              Lane B (fallback, only if Lane A is
  empty)
 ┌─────────────────────────────────────────────┐      ┌───────────────────────────────────────────┐
 │ geofence enter / office Wi-Fi seen          │      │ evidence log (append-only)                │
 │        ▼                                    │      │        ▼                                  │
 │ nudge notification "Tap to check in"        │      │ rules score ─► ML confidence              │
 │        ▼                                    │      │        ▼                                  │
 │ one tap (+ biometric unlock on the phone)   │      │ ≥ auto threshold → INFERRED session       │
 │        ▼                                    │      │ between thresholds → ask the employee     │
 │ reminders at +5 and +15 minutes             │      │ next-day confirmation / rejection         │
 └─────────────────────────────────────────────┘      └───────────────────────────────────────────┘
```


Lane A is the product. Lane B exists so that a forgotten tap does not become a false absence.

### 11.2 Aggregates and their invariants

| Aggregate              | Invariants                                                                                       |
| ---------------------- | ------------------------------------------------------------------------------------------------ |
| `AttendanceSession`    | `check_out_at` is null or after `check_in_at`; `source` is immutable; a session is never deleted |
| `Evidence`             | Immutable, append-only; belongs to one employee and one device                                   |
| `DailyStatus`          | Exactly one row per `(tenant, employee, work_date)`; derived, never edited directly              |
| `AttendanceCorrection` | Append-only record of a requested change; approval appends a `MANUAL` session                    |

### 11.3 Session state machine

```text
                 tap / manual            confirm
   (none) ───────────────────► OPEN ───────────────► CLOSED  (check_out set)
      │                           ▲
      │ inference ≥ auto          │ reject inferred session → void marker (appended), status
  recomputed
      ▼                           │
  INFERRED (confirm_status=PENDING) ──confirm──► INFERRED (CONFIRMED)
                                    ──reject───► INFERRED (REJECTED)  (ignored in precedence)
```


### 11.4 Precedence (decides the daily status)

Highest priority first:

1. **Holiday** (from `holiday_ref`)
1. **Approved leave** (from `leave_ref`)
1. **Manual** session
1. **Tap** session
1. **Inferred** session (not rejected)
1. **Absent** (nothing else applies on a working day)

Day-off (not a working day for the employee's shift) short-circuits to `DAY_OFF`.

### 11.5 Daily status calculation

Inputs: shift (start, end, grace), sessions of the day in tenant-local time, leave and holiday references.

```text
function computeStatus(day, shift, sessions, leave, holiday):
    if holiday: return HOLIDAY
    if not shift.worksOn(day): return DAY_OFF
    if leave covers the whole day: return ON_LEAVE
    active = sessions excluding rejected inferred ones
    if active is empty:
        return leave covers half the day ? ON_LEAVE (half) : ABSENT
    first_in  = min(check_in_at)
    last_out  = max(check_out_at, if any)
    late      = first_in > shift.start + grace          (grace default 10 min)
    early     = last_out is not null and last_out < shift.end - tolerance
    worked    = sum of session durations
    status    = LATE if late else LEAVE_EARLY if early else PRESENT
    return status with late_minutes, worked_minutes
```


Rules decided with the product owner:

- **Late** = first check-in after shift start plus grace.
- A half-day leave shortens the expected window instead of removing it.
- Multiple sessions per day are allowed (lunch out, field work); the worked time is their sum.

### 11.6 Day lifecycle and who triggers what

```text
06:00 local   daily-status rows may be pre-created as PENDING for working employees (optional)
shift start   geofence/Wi-Fi nudge window opens (client-side)
+5 / +15 min  reminders if no check-in (notification service reacts to time events)
cutoff        time.shift-cutoff.v1 emitted per employee
cutoff        attendance consumer finalises daily status, publishes attendance.daily-status.v1
next morning  unresolved INFERRED sessions get a confirmation prompt
```


### 11.7 Check-in command: sequence

```text
Flutter            gateway            attendance (one DB transaction)                Kafka
  │ POST /check-in    │                      │
  │──────────────────►│  validate JWT        │
  │                   │─────────────────────►│ check Idempotency-Key (cache or unique key)
  │                   │                      │ load shift_ref, holiday_ref, leave_ref (local)
  │                   │                      │ insert attendance_session (source=TAP)
  │                   │                      │ upsert daily_status (provisional)
  │                   │                      │ insert outbox: SessionCheckedIn, DailyStatusChanged
  │                   │                      │ COMMIT
  │◄──────────────────│◄─────────────────────│ 201 { session, status }
  │                   │                      │ ...asynchronously...
  │                   │                      │ relay ───────────────────────────────────►
  attendance.session.v1
  │                   │                      │
  notification, reporting consume
```


The user receives their answer after one local commit. Nothing downstream can make the request fail.

### 11.7.1 Validation done at check-in

| Check                                                   | Result if it fails                                               |
| ------------------------------------------------------- | ---------------------------------------------------------------- |
| Employee is `ACTIVE`                                    | 422 `EMPLOYEE_NOT_ACTIVE`                                        |
| Device matches the bound device (or first use binds it) | 422 `DEVICE_MISMATCH` (still recorded as evidence, flagged)      |
| `mock_location_flag` true                               | Session saved but flagged `REVIEW`; manager notified             |
| Already checked in with an open session                 | 409 `ALREADY_CHECKED_IN`                                         |
| Day is a holiday or approved leave                      | Allowed; recorded, but precedence keeps the holiday/leave status |

Business rule: **never block a genuine employee from tapping**; flag and review instead.

### 11.8 Evidence ingestion

Mobile batches evidence (geofence transitions, Wi-Fi BSSID matches, BLE sightings, sensor summaries) and posts `POST /attendance/evidence`. Attendance stores rows in `evidence` (partitioned by month) and publishes `attendance.evidence.v1`. Raw GPS tracks are **not** stored; only transition events and an accuracy value, to limit privacy exposure.

### 11.9 Inference pipeline

```text
cutoff approaches (e.g. 60 min after shift start, no TAP session)
   │
   ▼
attendance builds a feature snapshot for the employee-day (see 16.3)
   │ publishes attendance.scoring-requested.v1
   ▼
confidence-service scores ──► attendance.confidence.v1 { confidence, decision, reasons,
  modelVersion }
   │
   ▼
attendance consumer:
   AUTO_INFER   → insert INFERRED session (source=INFERRED, confidence, reason,
  confirm_status=PENDING)
                  publish attendance.session.v1 (Inferred); notify the employee to confirm tomorrow
   ASK_EMPLOYEE → publish a prompt event; notification asks "Were you at work today?"
   NO_ACTION    → nothing; day will resolve to ABSENT at cutoff unless a tap arrives
```


If the confidence service is down, attendance falls back to the **rules score** after a timeout on the scoring request, so the platform degrades gracefully.

### 11.10 Corrections and manual entries

1. Employee submits a correction with a reason.
1. A manager or HR approves or rejects.
1. Approval appends a `MANUAL` session and a correction row; nothing is edited or removed.
1. Daily status is recomputed and `attendance.daily-status.v1` is published again with a higher version number.

### 11.11 Idempotency and concurrency

| Concern                                            | Approach                                                                                                                                    |
| -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| Duplicate check-in tap (double click, retry)       | `Idempotency-Key` plus a unique index on `(tenant_id, employee_id, check_in_at)`                                                            |
| Two devices at once                                | Partition key serialises event handling; DB unique constraint guards the HTTP path                                                          |
| Late event arriving after the status was finalised | Status is recomputed (idempotent function of inputs); `version` on `daily_status` prevents older recomputations from overwriting newer ones |
| Clock skew on the phone                            | Server uses its own receive time for `check_in_at`; phone time is kept as `client_at` evidence                                              |



## 12. Leave domain

### 12.1 Request state machine

```text
          submit                 approve
(new) ──────────► PENDING ───────────────► APPROVED ──cancel (future days)──► CANCELLED
                     │  reject                 │
                     ▼                         └─ day passes ──► (terminal)
                  REJECTED
                     │ employee cancels while pending
                     ▼
                  CANCELLED
```


### 12.2 Balance model

`available = entitled + carried_over − used − pending`

| Action                   | Balance effect                      | Ledger row                |
| ------------------------ | ----------------------------------- | ------------------------- |
| Submit                   | `pending += days`                   | `-days`, reason `SUBMIT`  |
| Approve                  | `pending -= days`, `used += days`   | reason `APPROVE`          |
| Reject / cancel pending  | `pending -= days`                   | `+days`, reason `RELEASE` |
| Cancel approved (future) | `used -= days`                      | `+days`, reason `CANCEL`  |
| Year-end                 | carry-over up to the policy maximum | reason `ROLLOVER`         |
| HR adjustment            | entitled changes                    | reason `ADJUST` with note |

The **ledger is the audit trail**; the balance row is a cache that can be rebuilt from it.

### 12.3 Validation

- Dates are working days of the employee's shift, excluding holidays (`holiday_ref`).
- No overlap with another pending or approved request for the same employee.
- Balance must cover `days` for paid types (`422 LEAVE_BALANCE_INSUFFICIENT`).
- Attachment required if `requires_attachment` (e.g. sick leave beyond N days).
- Half-day only when `allow_half_day`.

### 12.4 Concurrency

Two simultaneous submissions could both pass a balance check. The service updates the balance with a **conditional update** (`UPDATE leave_balance SET pending = pending + :d WHERE … AND entitled + carried_over - used - pending >= :d`) and treats zero rows updated as insufficient balance. No distributed lock is needed.

### 12.5 Events and effects

| Event                                   | Effect elsewhere                                                                                                 |
| --------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `LeaveSubmitted`                        | notification → manager                                                                                           |
| `LeaveApproved` (with the per-day list) | attendance writes `leave_ref` rows; recomputes statuses; notification → employee; reporting updates `leave_fact` |
| `LeaveRejected` / `LeaveCancelled`      | attendance removes `leave_ref` rows (soft), recomputes statuses                                                  |

"Special leave" is a leave type (`SPECIAL`) with its own policy, not a separate service.
