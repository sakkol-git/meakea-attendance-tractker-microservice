# Contracts reference 16

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## 5. Event contract catalog

### 5.1 Shared envelope and processing contract

Status: proposed normalized contracts. The only existing machine schema is the legacy flat `AttendanceCheckedIn` Avro record. All catalog entries below use this envelope; every field is required except nullable causationId.

| Field | Type | Meaning |
|---|---|---|
| eventId | uuid | Stable publication/deduplication ID, never regenerated for relay/retry |
| type | string | Allowlisted past-tense event fact with `.v1` suffix |
| schemaVersion | integer, exactly 1 | Envelope major version |
| tenantId | uuid | Producer-validated owner tenant |
| aggregateId | string | Stable owner aggregate ID; composite IDs use canonical colon-separated UUID/date components |
| aggregateVersion | positive bigint | Monotonic revision of aggregate, not Kafka offset or timestamp |
| occurredAt | UTC instant | Owner commit/fact time for snapshots; evidence observation time also in payload |
| correlationId | uuid | Originating request/job chain |
| causationId | uuid or null | Triggering event or durable command ID |
| payload | typed object | Exact topic/type payload below; no arbitrary entity serialization |

Every entry inherits: delivery at least once; idempotency is consumer-name + tenantId + eventId inbox; snapshot upserts additionally guard aggregateVersion. Order is only within the keyed topic partition. Cross-topic order is not assumed. Within v1, additive optional fields need defaults and compatibility fixtures; breaking semantics/types require a new major contract. Source owners alone publish authoritative topics. Unknown/malformed types quarantine; transient failures retry under the architecture's bounded policy. Retention proposal: org and balance snapshots compacted with explicit inactive snapshots; other facts thirty days initially, subject to approved privacy/recovery horizon. Current topic script creates all existing business topics with six partitions and RF=1; it does not create proposed leave.balance or retry/DLQ topics. No production topic configuration, schema registration, or compatibility test is claimed to exist.

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
    "effective": true,
    "confidence": null,
    "reason": null,
    "version": 1,
    "supersedesId": null,
    "modelVersion": null
  }
}
```


Following JSON blocks are payload examples inserted in that same envelope, not standalone wire schemas. Fields shown in payload definitions are required unless suffixed `?` or explicitly nullable. Each topic may carry listed type variants; every variant carries the full snapshot unless stated otherwise.

### 5.2 org.employee.v1

- Owner/producer: identity-org; consumers: attendance, leave, request, notification, reporting.
- Types: `org.employee.created.v1`, `org.employee.updated.v1`, `org.employee.terminated.v1`.
- Aggregate: employee UUID; key `tenantId:employeeId`; compaction retains one complete employee snapshot.
- Payload: employeeId/uuid, userId/uuid?, employeeNo/string, fullName/string, departmentId/uuid?, departmentName/string?, position/string?, managerId/uuid?, hireDate/date, terminatedOn/date?, status/enum, deviceId/string?, locale/string, assignments/AssignmentSnapshot[], deletedAt/instant?.
- AssignmentSnapshot: assignmentId/uuid, shiftId/uuid, validFrom/date, validTo/date?; includes all applicable/historical assignments needed for permitted backfill.
- Meaning: authoritative employee eligibility and hierarchy snapshot; manager and assignment changes are updated facts.
- Ordering: employee revision; consumers replace only with newer snapshot and reconcile assignment set transactionally.
- Failure: absent shift snapshot may delay schedule-dependent commands; no synchronous identity fallback.

```json
{
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "userId": null,
  "employeeNo": "E001",
  "fullName": "Demo Employee",
  "departmentId": null,
  "departmentName": null,
  "position": null,
  "managerId": null,
  "hireDate": "2026-10-01",
  "terminatedOn": null,
  "status": "ACTIVE",
  "deviceId": "installation-example",
  "locale": "en",
  "assignments": [
    {
      "assignmentId": "019a0000-0000-7000-8000-000000000021",
      "shiftId": "019a0000-0000-7000-8000-000000000022",
      "validFrom": "2026-10-01",
      "validTo": null
    }
  ],
  "deletedAt": null
}
```


### 5.3 org.shift.v1

- Owner/producer: identity-org; consumer: attendance.
- Types: `org.shift.defined.v1`, `org.shift.updated.v1`, `org.shift.removed.v1`.
- Aggregate/key: shift UUID / `tenantId:shiftId`; compacted full snapshot.
- Payload: shiftId/uuid, name/string, startTime/local-time, endTime/local-time, timezone/IANA, graceMinutes/int, workingDays/int, site/SiteSnapshot, deletedAt/instant?.
- SiteSnapshot: siteId/uuid, latitude/number, longitude/number, radiusM/number, wifiSsid/string?, wifiBssid/string?; restricted to permitted consumers.
- Meaning: complete schedule template including evidence configuration; assignment is in org.employee, not another record sharing this compacted key.
- Ordering/failure: guard shift revision; invalid zone/radius quarantines and blocks only affected schedule use.

```json
{
  "shiftId": "019a0000-0000-7000-8000-000000000022",
  "name": "Day shift",
  "startTime": "08:00",
  "endTime": "17:00",
  "timezone": "Asia/Phnom_Penh",
  "graceMinutes": 10,
  "workingDays": 31,
  "site": {
    "siteId": "019a0000-0000-7000-8000-000000000023",
    "latitude": 11.56,
    "longitude": 104.92,
    "radiusM": 100,
    "wifiSsid": null,
    "wifiBssid": null
  },
  "deletedAt": null
}
```


### 5.4 org.holiday.v1

- Owner/producer: identity-org; consumers: attendance, leave.
- Types: `org.holiday.added.v1`, `org.holiday.updated.v1`, `org.holiday.removed.v1`.
- Aggregate/key: holiday UUID / `tenantId:holidayId`; compacted, stable identity across date correction.
- Payload: holidayId/uuid, date/date, name/string, kind/PUBLIC|COMPANY, deletedAt/instant?.
- Meaning: calendar eligibility fact; removal is explicit inactive snapshot before any eventual Kafka tombstone.
- Ordering/failure: guard holiday revision; recompute impacted dates or flag historical review, never silently discard evidence.

```json
{
  "holidayId": "019a0000-0000-7000-8000-000000000024",
  "date": "2026-11-09",
  "name": "Company holiday",
  "kind": "COMPANY",
  "deletedAt": null
}
```


### 5.5 attendance.evidence.v1

- Owner/producer: attendance; consumer: attendance evidence worker; reporting raw evidence consumption is deferred for privacy.
- Types: `attendance.evidence.recorded.v1`.
- Aggregate: evidence UUID, revision 1; key `tenantId:employeeId`; append-only, not compacted.
- Payload: evidenceId/uuid, employeeId/uuid, clientEventId/uuid, kind/allowed evidence enum, observedAt/instant, receivedAt/instant, deviceId/string?, provenance/string, accuracyM/number?, consentRevision/string?.
- Sensitive raw payload remains in owner evidence table; event carries a minimized signal summary only when permitted. Proposed `signals` object keys are inGeofence/boolean?, wifiConnected/boolean?, bleCount/int?, minutesInZone/number?.
- Meaning: observation accepted, not proof of attendance. Optional vision producer is not authorized by current design.
- Dedup: inbox and unique client observation ID; evidence processing uses stable IDs rather than aggregate revision deltas.
- Failure: invalid/spoofed/unconsented signals rejected/quarantined; no automatic human-present claim.

```json
{
  "evidenceId": "019a0000-0000-7000-8000-000000000025",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "clientEventId": "019a0000-0000-7000-8000-000000000026",
  "kind": "TAP",
  "observedAt": "2026-10-22T01:00:00Z",
  "receivedAt": "2026-10-22T01:00:00Z",
  "deviceId": "installation-example",
  "provenance": "AUTHENTICATED_DEVICE",
  "accuracyM": null,
  "consentRevision": null,
  "signals": {}
}
```


### 5.6 attendance.session.v1

- Owner/producer: attendance; consumer: notification; reporting may add session detail later but daily facts are its MVP source.
- Types: `attendance.session.checked-in.v1`, `checked-out`, `inferred`, `confirmed`, `rejected`, `corrected` using the full `attendance.session.<fact>.v1` spelling.
- Aggregate: session UUID; key `tenantId:employeeId`; non-compacted snapshot stream.
- Payload: Session schema with `id` renamed `sessionId`, plus supersedesId/uuid? and modelVersion/string?; no raw reasons/coordinates in generic push data.
- Meaning: current session interpretation. Corrections emit old session effective=false snapshot and new session snapshot with distinct aggregate revisions/IDs.
- Ordering: guard per-session revision even though different sessions share employee key; different topics remain unordered.
- Failure: delayed supersession must not double-count; daily-status snapshot is authoritative for reporting totals.

```json
{
  "sessionId": "019a0000-0000-7000-8000-000000000030",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "workDate": "2026-10-22",
  "checkInAt": "2026-10-22T01:00:00Z",
  "checkOutAt": null,
  "source": "TAP",
  "confidence": null,
  "reason": null,
  "confirmStatus": "NONE",
  "effective": true,
  "version": 1,
  "supersedesId": null,
  "modelVersion": null
}
```


The event payload uses `sessionId` in place of API Session `id`; this explicit mapping is required in producer adapters. `aggregateId` must equal payload sessionId. API resource IDs remain `id`.

### 5.7 attendance.daily-status.v1

- Owner/producer: attendance; consumers: notification, reporting.
- Type: `attendance.daily-status.calculated.v1`.
- Aggregate: employeeId:workDate; key `tenantId:employeeId`; non-compacted full snapshot.
- Payload: DailyStatus plus departmentId/uuid?, firstIn/instant?, lastOut/instant?, lateMinutes/int, source/TAP|NUDGE|INFERRED|MANUAL|CALENDAR|NONE, sourceCommittedAt/instant, finalizedAt/instant?.
- Meaning: new daily interpretation; flags preserve late-and-early and review conflict instead of collapsing evidence.
- Ordering/failure: higher day revision wins; summary updates replace prior contribution transactionally; lag exposed to clients.

```json
{
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "workDate": "2026-10-22",
  "departmentId": null,
  "status": "PRESENT",
  "dayType": "WORKING",
  "attendanceState": "OPEN",
  "late": false,
  "earlyLeave": false,
  "workedMinutes": null,
  "reviewRequired": false,
  "version": 1,
  "firstIn": "2026-10-22T01:00:00Z",
  "lastOut": null,
  "lateMinutes": 0,
  "source": "TAP",
  "sourceCommittedAt": "2026-10-22T01:00:00Z",
  "finalizedAt": null
}
```


### 5.8 leave.request.v1

- Owner/producer: leave; consumers: attendance, notification, reporting.
- Types: `leave.request.submitted.v1`, `approved`, `rejected`, `cancelled`, `review-required`, `resolved`, each fully prefixed/suffixed.
- Aggregate: leave request UUID; key `tenantId:requestId`; non-compacted full snapshot.
- Payload: requestId/uuid, employeeId/uuid, departmentId/uuid?, leaveTypeId/uuid, startDate/date, endDate/date, days/decimal, status/leave state, decidedAt/instant?, dayPortions/[{date,portion:FULL|AM|PM,units:0.5|1}], policyVersion/int, syncState/enum.
- Meaning: owner approval/balance decision, not distributed attendance completion.
- Ordering: source request revision; cancellation removes only referenced overlay; NEEDS_REVIEW retains debit/overlay under proposed O02 policy.
- Failure: outbox retry; stale conflict must not reverse a newer cancellation; manual resolution emits compensating balance snapshot when needed.

```json
{
  "requestId": "019a0000-0000-7000-8000-000000000031",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "departmentId": null,
  "leaveTypeId": "019a0000-0000-7000-8000-000000000032",
  "startDate": "2026-10-26",
  "endDate": "2026-10-26",
  "days": 1,
  "status": "APPROVED",
  "decidedAt": "2026-10-22T02:00:00Z",
  "dayPortions": [
    {
      "date": "2026-10-26",
      "portion": "FULL",
      "units": 1
    }
  ],
  "policyVersion": 1,
  "syncState": "PENDING"
}
```


### 5.9 attendance.leave-conflict.v1

- Owner/producer: attendance; consumer: leave; notification may consume leave's resulting review-required fact instead.
- Type: `attendance.leave-conflict.detected.v1`.
- Aggregate: conflict UUID; key `tenantId:leaveRequestId`; non-compacted append-only fact.
- Payload: conflictId/uuid, leaveRequestId/uuid, leaveRequestVersion/bigint, employeeId/uuid, dates/date[], sessionIds/uuid[], reasonCode/WORK_OVERLAPS_LEAVE, detectedAt/instant.
- Meaning: evidence/leave overlap requires review; no automatic balance credit.
- Dedup: inbox plus leave current request revision/state guard. Repeated conflicts for the same approval/day are coalesced by owner business key.
- Failure: if reference version is stale, consume as obsolete; if unknown future version, defer/reconcile rather than force state.

```json
{
  "conflictId": "019a0000-0000-7000-8000-000000000033",
  "leaveRequestId": "019a0000-0000-7000-8000-000000000031",
  "leaveRequestVersion": 3,
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "dates": [
    "2026-10-26"
  ],
  "sessionIds": [
    "019a0000-0000-7000-8000-000000000030"
  ],
  "reasonCode": "WORK_OVERLAPS_LEAVE",
  "detectedAt": "2026-10-26T01:00:00Z"
}
```


### 5.10 request.workflow.v1

- Owner/producer: request; consumers: notification, reporting.
- Types: `request.workflow.submitted.v1`, `step-approved`, `approved`, `rejected`, `cancelled`, `fulfilled` fully prefixed/suffixed.
- Aggregate: request UUID; key `tenantId:requestId`; non-compacted full snapshot.
- Payload: requestId/uuid, requesterId/uuid, departmentId/uuid?, typeId/uuid, typeCode/string, status/request state, currentStep/int?, submittedAt/instant?, decidedAt/instant?, steps/[{stepNo,approverId,approverRole,status}], cycleHours/decimal?.
- Meaning: current workflow state; excludes sensitive form/attachment bodies.
- Ordering: request revision; consumer guards state snapshot, notification recipient dedupe by event ID.
- Failure: retries cannot reapprove or fulfill twice because owner transition is already committed and locked.

```json
{
  "requestId": "019a0000-0000-7000-8000-000000000034",
  "requesterId": "019a0000-0000-7000-8000-000000000020",
  "departmentId": null,
  "typeId": "019a0000-0000-7000-8000-000000000035",
  "typeCode": "MATERIAL",
  "status": "PENDING",
  "currentStep": 1,
  "submittedAt": "2026-10-22T01:00:00Z",
  "decidedAt": null,
  "steps": [
    {
      "stepNo": 1,
      "approverId": "019a0000-0000-7000-8000-000000000036",
      "approverRole": "MANAGER",
      "status": "WAITING"
    }
  ],
  "cycleHours": null
}
```


### 5.11 time.shift-cutoff.v1

- Owner/producer: attendance scheduler; consumer: attendance cutoff worker.
- Type: `time.shift-cutoff.reached.v1`.
- Aggregate: durable cutoff job UUID; key `tenantId:employeeId`; append-only trigger, non-compacted.
- Payload: cutoffId/uuid, employeeId/uuid, workDate/date, shiftRevision/bigint, cutoffAt/instant, kind/CHECK_IN|CHECK_OUT.
- Meaning: evaluate a specific employee/day using local projections, not proof that absence has already been determined.
- Dedup: inbox plus unique cutoff business key; scheduler restarts reuse durable job ID.
- Failure: stale/missing references defer calculation; replayed cutoffs recompute safely without duplicating status revision when no change.

```json
{
  "cutoffId": "019a0000-0000-7000-8000-000000000037",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "workDate": "2026-10-22",
  "shiftRevision": 1,
  "cutoffAt": "2026-10-22T10:30:00Z",
  "kind": "CHECK_OUT"
}
```


### 5.12 leave.balance.v1 — proposed extension

- Justification: the preserved `/reports/leave/balances` endpoint cannot be built from request events alone (adjustments/accrual/rollover also change balances).
- Owner/producer: leave; consumer: reporting.
- Type: `leave.balance.changed.v1`; aggregate employeeId:leaveTypeId:year; key `tenantId:employeeId:leaveTypeId:year`.
- Payload: Balance schema; aggregateVersion follows balance version.
- Compacted full snapshot; retention/compatibility/dedup follow shared snapshot rules.
- Meaning: authoritative materialized balance after one ledger transaction, including pending and used deltas.
- Failure: report is marked stale until caught up; no cross-schema read as fallback.

```json
{
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "leaveTypeId": "019a0000-0000-7000-8000-000000000032",
  "year": 2026,
  "entitled": 18,
  "carriedOver": 0,
  "used": 2,
  "pending": 1,
  "available": 15,
  "version": 4
}
```


### 5.13 Legacy schema and optional events

Existing `AttendanceCheckedIn` Avro fields: eventId/UUID logical string, tenantId/string, employeeId/string, occurredAt/timestamp-millis, source/MANUAL|NUDGE|INFERRED, reason/nullable string. It lacks generic envelope fields and its MANUAL versus API TAP semantics differ. Do not silently reinterpret MANUAL. Before publication, choose a new normalized schema/topic migration or preserve legacy producer/consumer adapters with explicit mapping. Optional `notification.sent.v1` is deferred: provider acceptance is not delivery, and no MVP consumer needs the event. Vision/camera producers and ML scoring topics are not approved; a future ADR must define consent, payloads, and asynchronous result behavior.

## 6. Contract authority and machine-readable evolution

No OpenAPI/AsyncAPI files or generators exist in the inspected repository. These Markdown contracts define planned behavior; no mock server is claimed to have been generated. Proposed source strategy: API OpenAPI files per owning service and Avro event schemas under hrflow-contracts, with an AsyncAPI routing/catalog index if Avro is retained. Choose serialization in O06 before expanding schemas; avoid JSON and Avro catalogs that disagree on field authority. Week 1 delivers machine-readable contracts and mock examples for auth/check-in/today first; remaining endpoints expand alongside features. CI should parse schemas, validate examples, run compatibility fixtures, and verify controller/event producers against those contracts. This documentation revision does not add speculative machine contracts for all 112 routes before domain decisions are approved.

## 7. Cross-contract correctness gates

- Tenant row predicates, composite constraints, resource authorization, and safe background context are all necessary; tenant_id alone is insufficient.
- Guard/lock and state predicates enforce balance/approval atomicity; outbox is in that same local transaction.
- Offset acknowledgment follows local commit; broker idempotence does not create exactly-once database/provider effects.
- Outbox cleanup and inbox retention account for replay; full snapshots plus owner exports permit rebuild.
- Historical evidence and audit are append-only in normal use; controlled privacy retention is separately authorized.
- Attachment presign is not attachment approval; quarantine/READY is mandatory before sensitive download/use.
- Full snapshot payloads and higher revisions protect projections from out-of-order events.
- `syncState=APPLIED` requires actual attendance acknowledgment/reconciliation evidence; until that protocol exists use PENDING/UNKNOWN/CONFLICT, never guess APPLIED.
- Asynchronous exports/year-end have status endpoints and persisted jobs.
- O02/O03/O05/O06/O08 must be resolved or the dependent capability remains disabled before real-user launch.
