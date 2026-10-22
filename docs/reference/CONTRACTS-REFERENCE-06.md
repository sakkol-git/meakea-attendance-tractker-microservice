# Contracts reference 06

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### API-031: GET /api/v1/holidays

- Owner: `identity-org`; status: planned.
- Purpose and validation: List tenant calendar holidays.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query year.
- Request schema: Empty; response schema: Page`<Holiday>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","date":"2026-11-09","name":"Company holiday","kind":"COMPANY","version":1}],"page":0,"size":20,"total":1}`

### API-032: POST /api/v1/holidays

- Owner: `identity-org`; status: planned.
- Purpose and validation: Create unique tenant date; repeat conflicting create 409.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: HolidayInput; response schema: Holiday.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.holiday.v1.
- Request example: `{"date":"2026-11-09","name":"Company holiday","kind":"COMPANY"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","date":"2026-11-09","name":"Company holiday","kind":"COMPANY","version":1}`

### API-033: DELETE /api/v1/holidays/{id}

- Owner: `identity-org`; status: planned.
- Purpose and validation: Soft remove with explicit inactive snapshot, not only Kafka tombstone.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: REMOVE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Empty.
- Success: 204; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: org.holiday.v1.
- Request example: no body.
- Response example: no body.

### API-034: GET /api/v1/audit-logs

- Owner: `identity-org`; status: planned.
- Purpose and validation: Identity audit only; other owner audits remain service-owned; global audit search deferred.
- Authorization and tenant scope: OWNER tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query actorId,entity,from,to.
- Request schema: Empty; response schema: Page`<Audit>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","actorId":"019a0000-0000-7000-8000-000000000020","action":"EMPLOYEE_UPDATED","entity":"employee","entityId":"019a0000-0000-7000-8000-000000000020","diff":{"fullName":"Updated Employee"},"at":"2026-10-22T01:00:00Z"}],"page":0,"size":20,"total":1}`

### API-035: POST /api/v1/attendance/check-in

- Owner: `attendance`; status: planned.
- Purpose and validation: Commit evidence/session/outbox atomically; existing open session 409; missing projection 503 REFERENCE_NOT_READY.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: CheckIn; response schema: Session.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: attendance.evidence.v1,attendance.session.v1,attendance.daily-status.v1.
- Request example: `{"deviceId":"installation-example","occurredAt":"2026-10-22T01:00:00Z","source":"TAP"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","checkInAt":"2026-10-22T01:00:00Z","checkOutAt":null,"source":"TAP","confidence":null,"reason":null,"confirmStatus":"NONE","effective":true,"version":1}`

### API-036: POST /api/v1/attendance/check-out

- Owner: `attendance`; status: planned.
- Purpose and validation: If-Match applies to sessionId; lock open session; no open session 409; invalid chronology 422.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: CheckOut; response schema: Session.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: attendance.evidence.v1,attendance.session.v1,attendance.daily-status.v1.
- Request example: `{"sessionId":"019a0000-0000-7000-8000-000000000020","occurredAt":"2026-10-22T10:00:00Z","deviceId":"installation-example"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","checkInAt":"2026-10-22T01:00:00Z","checkOutAt":null,"source":"TAP","confidence":null,"reason":null,"confirmStatus":"NONE","effective":true,"version":1}`

### API-037: POST /api/v1/attendance/evidence

- Owner: `attendance`; status: planned.
- Purpose and validation: 201 receipt; stable item event IDs; no CAMERA_PRESENCE until separate review; no synchronous ML.
- Authorization and tenant scope: EMPLOYEE self with permitted consent/device; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: EvidenceBatch; response schema: EvidenceReceipt.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: attendance.evidence.v1.
- Request example: `{"items":[{"eventId":"019a0000-0000-7000-8000-000000000020","kind":"TAP","occurredAt":"2026-10-22T01:00:00Z","deviceId":"installation-example","payload":{}}]}`
- Response example: `{"acceptedIds":["019a0000-0000-7000-8000-000000000020"],"duplicates":[]}`

### API-038: GET /api/v1/attendance/today

- Owner: `attendance`; status: planned.
- Purpose and validation: Resolve local work date and indicate missing references/review; no guessed shift.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Empty; response schema: Today.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"workDate":"2026-10-22","shift":null,"sessions":[],"dailyStatus":null,"nextAction":"REVIEW"}`

### API-039: GET /api/v1/attendance/me

- Owner: `attendance`; status: planned.
- Purpose and validation: Own effective and historical sessions; default current month.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query from,to.
- Request schema: Empty; response schema: Page`<Session>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","checkInAt":"2026-10-22T01:00:00Z","checkOutAt":null,"source":"TAP","confidence":null,"reason":null,"confirmStatus":"NONE","effective":true,"version":1}],"page":0,"size":20,"total":1}`

### API-040: GET /api/v1/attendance/daily-status/me

- Owner: `attendance`; status: planned.
- Purpose and validation: Own calendar; YYYY-MM month required/default current local month.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query month.
- Request schema: Empty; response schema: Page`<DailyStatus>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","status":"PRESENT","dayType":"WORKING","attendanceState":"OPEN","late":false,"earlyLeave":false,"workedMinutes":null,"reviewRequired":false,"version":1}],"page":0,"size":20,"total":1}`

### API-041: POST /api/v1/attendance/sessions/{id}/confirm

- Owner: `attendance`; status: planned.
- Purpose and validation: Confirm once; manual sessions cannot use this transition.
- Authorization and tenant scope: EMPLOYEE owning pending inferred session; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Session.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: attendance.session.v1,attendance.daily-status.v1.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","checkInAt":"2026-10-22T01:00:00Z","checkOutAt":null,"source":"TAP","confidence":null,"reason":null,"confirmStatus":"NONE","effective":true,"version":1}`

### API-042: POST /api/v1/attendance/sessions/{id}/reject

- Owner: `attendance`; status: planned.
- Purpose and validation: Append rejection and exclude inferred work; retain evidence.
- Authorization and tenant scope: EMPLOYEE owning pending inferred session; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Reason; response schema: Session.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: attendance.session.v1,attendance.daily-status.v1.
- Request example: `{"reason":"Incorrect recorded time"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","checkInAt":"2026-10-22T01:00:00Z","checkOutAt":null,"source":"TAP","confidence":null,"reason":null,"confirmStatus":"NONE","effective":true,"version":1}`

### API-043: POST /api/v1/attendance/corrections

- Owner: `attendance`; status: planned.
- Purpose and validation: Create pending correction and audit; no session mutation before approval.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: CorrectionInput; response schema: Correction.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"sessionId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","newCheckIn":"2026-10-22T01:00:00Z","newCheckOut":"2026-10-22T10:00:00Z","reason":"Recorded checkout was missing"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","sessionId":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","newCheckIn":"2026-10-22T01:00:00Z","newCheckOut":null,"reason":"Recorded time was incorrect","status":"PENDING","version":1,"decisions":[]}`

### API-044: GET /api/v1/attendance/corrections

- Owner: `attendance`; status: planned.
- Purpose and validation: Default PENDING; manager excludes self approval.
- Authorization and tenant scope: MANAGER team or HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query status,from,to.
- Request schema: Empty; response schema: Page`<Correction>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","sessionId":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","newCheckIn":"2026-10-22T01:00:00Z","newCheckOut":null,"reason":"Recorded time was incorrect","status":"PENDING","version":1,"decisions":[]}],"page":0,"size":20,"total":1}`

### API-045: POST /api/v1/attendance/corrections/{id}/approve

- Owner: `attendance`; status: planned.
- Purpose and validation: Append immutable decision and replacement manual revision in same transaction.
- Authorization and tenant scope: MANAGER current direct report or HR_ADMIN; not requester; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Decision; response schema: Correction.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: attendance.session.v1,attendance.daily-status.v1.
- Request example: `{"note":"Reviewed supporting details"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","sessionId":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","newCheckIn":"2026-10-22T01:00:00Z","newCheckOut":null,"reason":"Recorded time was incorrect","status":"PENDING","version":1,"decisions":[]}`

### API-046: POST /api/v1/attendance/corrections/{id}/reject

- Owner: `attendance`; status: planned.
- Purpose and validation: Non-empty note; append rejection decision, preserve original session.
- Authorization and tenant scope: MANAGER current direct report or HR_ADMIN; not requester; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Decision; response schema: Correction.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"note":"Reviewed supporting details"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","sessionId":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","newCheckIn":"2026-10-22T01:00:00Z","newCheckOut":null,"reason":"Recorded time was incorrect","status":"PENDING","version":1,"decisions":[]}`

### API-047: GET /api/v1/attendance/team

- Owner: `attendance`; status: planned.
- Purpose and validation: Default today; scope before count/aggregation.
- Authorization and tenant scope: MANAGER own direct reports; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query date.
- Request schema: Empty; response schema: Page`<DailyStatus>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","status":"PRESENT","dayType":"WORKING","attendanceState":"OPEN","late":false,"earlyLeave":false,"workedMinutes":null,"reviewRequired":false,"version":1}],"page":0,"size":20,"total":1}`

### API-048: GET /api/v1/attendance/daily-status

- Owner: `attendance`; status: planned.
- Purpose and validation: Company daily status; explicit projection freshness.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query date,departmentId,status.
- Request schema: Empty; response schema: Page`<DailyStatus>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","status":"PRESENT","dayType":"WORKING","attendanceState":"OPEN","late":false,"earlyLeave":false,"workedMinutes":null,"reviewRequired":false,"version":1}],"page":0,"size":20,"total":1}`

### API-049: POST /api/v1/attendance/manual

- Owner: `attendance`; status: planned.
- Purpose and validation: Add audited manual revision; leave/holiday overlap flags review rather than silently overriding.
- Authorization and tenant scope: HR_ADMIN tenant; reason required; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: ManualSession; response schema: Session.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: attendance.session.v1,attendance.daily-status.v1.
- Request example: `{"employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","checkInAt":"2026-10-22T01:00:00Z","checkOutAt":"2026-10-22T10:00:00Z","reason":"Approved missing record"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","checkInAt":"2026-10-22T01:00:00Z","checkOutAt":null,"source":"TAP","confidence":null,"reason":null,"confirmStatus":"NONE","effective":true,"version":1}`

### API-050: GET /api/v1/attendance/sessions/{id}/evidence

- Owner: `attendance`; status: planned.
- Purpose and validation: Return minimized evidence/reasons; no unrestricted location history.
- Authorization and tenant scope: HR_ADMIN tenant; access audited; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Page`<Evidence>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","eventId":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","kind":"TAP","occurredAt":"2026-10-22T01:00:00Z","receivedAt":"2026-10-22T01:00:00Z","deviceId":"installation-example","payload":{},"provenance":"AUTHENTICATED_DEVICE"}],"page":0,"size":20,"total":1}`

### API-051: GET /api/v1/leave/types

- Owner: `leave`; status: planned.
- Purpose and validation: List enabled types; policy details limited to needed fields.
- Authorization and tenant scope: any authenticated employee/user; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query active.
- Request schema: Empty; response schema: Page`<LeaveType>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","code":"ANNUAL","name":"Annual leave","paid":true,"requiresAttachment":false,"maxDaysPerRequest":20,"allowHalfDay":true,"version":1}],"page":0,"size":20,"total":1}`

### API-052: GET /api/v1/leave/balances/me

- Owner: `leave`; status: planned.
- Purpose and validation: Authoritative own balances, not reporting projection.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query year.
- Request schema: Empty; response schema: Page`<Balance>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","year":2026,"entitled":18,"carriedOver":0,"used":2,"pending":1,"available":15,"version":1}],"page":0,"size":20,"total":1}`

### API-053: POST /api/v1/leave/requests

- Owner: `leave`; status: planned.
- Purpose and validation: Expand eligible days; lock employee/balances; reserve pending; 422 balance/overlap/policy.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: LeaveInput; response schema: LeaveRequest.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: leave.request.v1,leave.balance.v1.
- Request example: `{"leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","halfDay":"NONE","reason":"Personal leave"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}`

### API-054: GET /api/v1/leave/requests/me

- Owner: `leave`; status: planned.
- Purpose and validation: List own leave history.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query status,from,to.
- Request schema: Empty; response schema: Page`<LeaveRequest>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}],"page":0,"size":20,"total":1}`

### API-055: GET /api/v1/leave/requests/{id}

- Owner: `leave`; status: planned.
- Purpose and validation: Hide medical attachment/reason from unrelated viewers.
- Authorization and tenant scope: self, assigned MANAGER, or HR_ADMIN; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: LeaveRequest.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}`

### API-056: POST /api/v1/leave/requests/{id}/cancel

- Owner: `leave`; status: planned.
- Purpose and validation: Release reservation or compensate used ledger exactly once; historical cancellation requires HR review.
- Authorization and tenant scope: EMPLOYEE owning pending or future approved leave; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Reason; response schema: LeaveRequest.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: leave.request.v1,leave.balance.v1.
- Request example: `{"reason":"Incorrect recorded time"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}`

### API-057: GET /api/v1/leave/requests/pending

- Owner: `leave`; status: planned.
- Purpose and validation: Assigned/current-scope pending approvals; excludes self.
- Authorization and tenant scope: MANAGER team or HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query from,to.
- Request schema: Empty; response schema: Page`<LeaveRequest>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}],"page":0,"size":20,"total":1}`

### API-058: POST /api/v1/leave/requests/{id}/approve

- Owner: `leave`; status: planned.
- Purpose and validation: PENDING to APPROVED; pending to used in same transaction; attendance application remains async.
- Authorization and tenant scope: MANAGER assigned current direct report or HR_ADMIN; not requester; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Decision; response schema: LeaveRequest.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: leave.request.v1,leave.balance.v1.
- Request example: `{"note":"Reviewed supporting details"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}`

### API-059: POST /api/v1/leave/requests/{id}/reject

- Owner: `leave`; status: planned.
- Purpose and validation: Required note; PENDING to REJECTED; release pending once.
- Authorization and tenant scope: MANAGER assigned current direct report or HR_ADMIN; not requester; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Decision; response schema: LeaveRequest.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: leave.request.v1,leave.balance.v1.
- Request example: `{"note":"Reviewed supporting details"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}`

### API-060: GET /api/v1/leave/calendar

- Owner: `leave`; status: planned.
- Purpose and validation: Minimized absence calendar; manager cannot expand scope with departmentId.
- Authorization and tenant scope: MANAGER team or HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query from,to,departmentId.
- Request schema: Empty; response schema: Page`<LeaveRequest>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}],"page":0,"size":20,"total":1}`
