# Contracts reference 08

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### API-091: GET /api/v1/requests/admin/types

- Owner: `request`; status: planned.
- Purpose and validation: List type revisions.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query active.
- Request schema: Empty; response schema: Page`<RequestType>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","code":"MATERIAL","name":"Material request","formSchema":{"type":"object"},"approvalChain":["MANAGER","HR_ADMIN"],"version":1}],"page":0,"size":20,"total":1}`

### API-092: POST /api/v1/requests/admin/types

- Owner: `request`; status: planned.
- Purpose and validation: Validate bounded form schema and approval roles; version future submissions.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: RequestTypeInput; response schema: RequestType.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"code":"MATERIAL","name":"Material request","formSchema":{"type":"object"},"approvalChain":["MANAGER","HR_ADMIN"]}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","code":"MATERIAL","name":"Material request","formSchema":{"type":"object"},"approvalChain":["MANAGER","HR_ADMIN"],"version":1}`

### API-093: POST /api/v1/notifications/devices

- Owner: `notification`; status: planned.
- Purpose and validation: Upsert own installation/token under unique binding; 200 if existing override; tokens never echoed.
- Authorization and tenant scope: any authenticated linked employee; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: DeviceInput; response schema: Device.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"platform":"ANDROID","token":"example-provider-token","installationId":"installation-example"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","platform":"ANDROID","active":true,"lastSeenAt":"2026-10-22T01:00:00Z","version":1}`

### API-094: DELETE /api/v1/notifications/devices/{id}

- Owner: `notification`; status: planned.
- Purpose and validation: Deactivate token on logout; cannot remove another employee token.
- Authorization and tenant scope: own employee installation; shared token/tenant/object rules apply.
- Profile: REMOVE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Empty.
- Success: 204; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: no body.

### API-095: GET /api/v1/notifications

- Owner: `notification`; status: planned.
- Purpose and validation: Own durable inbox; accepted event does not imply device delivery.
- Authorization and tenant scope: own employee; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query unread.
- Request schema: Empty; response schema: Page`<Notification>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","category":"ATTENDANCE","title":"Attendance updated","body":"Open HRFlow to review.","data":{},"readAt":null,"createdAt":"2026-10-22T01:00:00Z"}],"page":0,"size":20,"total":1}`

### API-096: GET /api/v1/notifications/unread-count

- Owner: `notification`; status: planned.
- Purpose and validation: Count scoped to own unread inbox.
- Authorization and tenant scope: own employee; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Empty; response schema: Count.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"count":2}`

### API-097: POST /api/v1/notifications/{id}/read

- Owner: `notification`; status: planned.
- Purpose and validation: Monotonic read timestamp; no If-Match needed; same key safe.
- Authorization and tenant scope: own employee; shared token/tenant/object rules apply.
- Profile: MARK; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Notification.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","category":"ATTENDANCE","title":"Attendance updated","body":"Open HRFlow to review.","data":{},"readAt":null,"createdAt":"2026-10-22T01:00:00Z"}`

### API-098: POST /api/v1/notifications/read-all

- Owner: `notification`; status: planned.
- Purpose and validation: Server captures cutoff once per idempotency key; newly arriving items remain unread.
- Authorization and tenant scope: own employee; shared token/tenant/object rules apply.
- Profile: MARK; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Empty; response schema: ReadReceipt.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"updated":2,"cutoff":"2026-10-22T01:00:00Z"}`

### API-099: GET /api/v1/notifications/preferences

- Owner: `notification`; status: planned.
- Purpose and validation: Own category/channel/locale preferences.
- Authorization and tenant scope: own employee; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Empty; response schema: Preferences.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"locale":"en","categories":[{"category":"ATTENDANCE","push":true,"email":false,"inApp":true}],"version":1}`

### API-100: PUT /api/v1/notifications/preferences

- Owner: `notification`; status: planned.
- Purpose and validation: Version full preference set; mandatory security categories cannot be disabled without policy.
- Authorization and tenant scope: own employee; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Preferences; response schema: Preferences.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"locale":"en","categories":[{"category":"ATTENDANCE","push":true,"email":false,"inApp":true}],"version":1}`
- Response example: `{"locale":"en","categories":[{"category":"ATTENDANCE","push":true,"email":false,"inApp":true}],"version":1}`

### API-101: GET /api/v1/notifications/stream

- Owner: `notification`; status: planned.
- Purpose and validation: SSE id durable cursor; data contains notificationId/category; Last-Event-ID scoped; fetch-streaming bearer auth.
- Authorization and tenant scope: own employee; shared token/tenant/object rules apply.
- Profile: STREAM; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: Empty; response schema: Notification.
- Success: 200 text/event-stream; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `id: 019a0000-0000-7000-8000-000000000020; event: notification; data: {"notificationId":"019a0000-0000-7000-8000-000000000020","category":"ATTENDANCE"}` (three SSE lines, then a blank line).

### API-102: GET /api/v1/notifications/admin/templates/{code}

- Owner: `notification`; status: planned.
- Purpose and validation: Read exact template locale/channel; no global template mutation.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; query channel,locale.
- Request schema: Empty; response schema: Template.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"code":"ATTENDANCE_UPDATED","channel":"IN_APP","locale":"en","subject":null,"body":"Open HRFlow to review attendance.","version":1}`

### API-103: PUT /api/v1/notifications/admin/templates/{code}

- Owner: `notification`; status: planned.
- Purpose and validation: Allowlisted placeholders; reject scripts/header injection; version existing or If-None-Match:* create 201.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; query channel,locale.
- Request schema: Template; response schema: Template.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"code":"ATTENDANCE_UPDATED","channel":"IN_APP","locale":"en","subject":null,"body":"Open HRFlow to review attendance.","version":1}`
- Response example: `{"code":"ATTENDANCE_UPDATED","channel":"IN_APP","locale":"en","subject":null,"body":"Open HRFlow to review attendance.","version":1}`

### API-104: GET /api/v1/reports/dashboard

- Owner: `reporting`; status: planned.
- Purpose and validation: Counts plus explicit projection freshness; no transactional join.
- Authorization and tenant scope: HR_ADMIN tenant or MANAGER team; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query date,departmentId.
- Request schema: Empty; response schema: Dashboard.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"workDate":"2026-10-22","counts":{"present":10,"late":1,"absent":0,"onLeave":2},"asOf":"2026-10-22T01:00:00Z","projectionLagMs":500,"isStale":false}`

### API-105: GET /api/v1/reports/attendance/summary

- Owner: `reporting`; status: planned.
- Purpose and validation: groupBy day or month; scope before aggregate.
- Authorization and tenant scope: HR_ADMIN tenant or MANAGER team; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query from,to,departmentId,groupBy.
- Request schema: Empty; response schema: Page`<ReportRow>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"dimension":"ANNUAL","period":"2026-10","count":2,"units":2,"asOf":"2026-10-22T01:00:00Z","projectionLagMs":500,"isStale":false}],"page":0,"size":20,"total":1}`

### API-106: GET /api/v1/reports/attendance/employees/{id}

- Owner: `reporting`; status: planned.
- Purpose and validation: Employee history; manager scope current and audited.
- Authorization and tenant scope: self, MANAGER direct report, HR_ADMIN; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; query from,to.
- Request schema: Empty; response schema: Page`<DailyStatus>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"employeeId":"019a0000-0000-7000-8000-000000000020","workDate":"2026-10-22","status":"PRESENT","dayType":"WORKING","attendanceState":"OPEN","late":false,"earlyLeave":false,"workedMinutes":null,"reviewRequired":false,"version":1}],"page":0,"size":20,"total":1}`

### API-107: GET /api/v1/reports/attendance/late-ranking

- Owner: `reporting`; status: planned.
- Purpose and validation: Ranking by total late minutes; stable ID tiebreaker.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query from,to,departmentId.
- Request schema: Empty; response schema: Page`<ReportRow>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"dimension":"ANNUAL","period":"2026-10","count":2,"units":2,"asOf":"2026-10-22T01:00:00Z","projectionLagMs":500,"isStale":false}],"page":0,"size":20,"total":1}`

### API-108: GET /api/v1/reports/leave/summary

- Owner: `reporting`; status: planned.
- Purpose and validation: Leave units grouped by type/department; exclude rejected/cancelled.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query from,to,departmentId,leaveTypeId.
- Request schema: Empty; response schema: Page`<ReportRow>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"dimension":"ANNUAL","period":"2026-10","count":2,"units":2,"asOf":"2026-10-22T01:00:00Z","projectionLagMs":500,"isStale":false}],"page":0,"size":20,"total":1}`

### API-109: GET /api/v1/reports/leave/balances

- Owner: `reporting`; status: planned.
- Purpose and validation: Requires proposed leave.balance.v1 projection; cannot query leave schema.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query year,departmentId.
- Request schema: Empty; response schema: Page`<Balance>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","year":2026,"entitled":18,"carriedOver":0,"used":2,"pending":1,"available":15,"version":1}],"page":0,"size":20,"total":1}`

### API-110: GET /api/v1/reports/requests/summary

- Owner: `reporting`; status: planned.
- Purpose and validation: Counts and cycle-hours via workflow snapshot; average duration fields specified below.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query from,to,typeId.
- Request schema: Empty; response schema: Page`<ReportRow>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"dimension":"ANNUAL","period":"2026-10","count":2,"units":2,"asOf":"2026-10-22T01:00:00Z","projectionLagMs":500,"isStale":false}],"page":0,"size":20,"total":1}`

### API-111: POST /api/v1/reports/exports

- Owner: `reporting`; status: planned.
- Purpose and validation: Persist export job and scope; CSV MVP; 202 does not promise immediate file.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: JOB; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: ExportInput; response schema: Job.
- Success: 202; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"kind":"ATTENDANCE","format":"CSV","from":"2026-10-01","to":"2026-10-22"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","status":"QUEUED","statusUrl":"/api/v1/reports/exports/019a0000-0000-7000-8000-000000000020"}`

### API-112: GET /api/v1/reports/exports/{id}

- Owner: `reporting`; status: planned.
- Purpose and validation: Poll status; READY includes newly authorized short-lived URL; EXPIRED observable; no URL for failed jobs.
- Authorization and tenant scope: original requester still authorized HR_ADMIN; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Job.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","status":"QUEUED","statusUrl":"/api/v1/reports/exports/019a0000-0000-7000-8000-000000000020"}`

### 3.1 Report schema refinements and critical failures

ReportRow is a shared envelope, not permission to return arbitrary shapes. Route-specific columns:

| Report | Additional typed fields |
|---|---|
| Attendance summary | workDate/date or month/string, departmentId/uuid?, present/int, late/int, absent/int, onLeave/int, workedMinutes/int |
| Late ranking | employeeId/uuid, fullName/string, lateMinutes/int, lateDays/int |
| Leave summary | leaveTypeId/uuid, departmentId/uuid?, days/decimal, requestCount/int |
| Request summary | typeId/uuid, status/string, count/int, averageCycleHours/decimal? |

`ReportRow.dimension/period/count` is the common projection shape; route refinements above are required in machine contracts, not inferred by clients. A post-commit connection failure on check-in is retried with the same key and yields the same session ID. A second key with an open session returns SESSION_ALREADY_OPEN, not a second interval. Leave approval while balance/request version changes yields 412/409, never a second debit. Provider/Registry downtime cannot change a committed resource response into a rolled-back business fact. If required local org projections are unavailable/stale, return 503 REFERENCE_NOT_READY and Retry-After; do not call identity for fallback. Cancellation after a newer approval/conflict revision requires fresh state and a new explicit command. Static paths such as `/requests/inbox` and `/leave/requests/pending` take precedence over `{id}` routes.

### 3.2 Implemented confidence prototype

Not part of the public gateway catalog. Source: `ml/confidence-service/confidence/api.py`. `GET /health` returns status/modelVersion; `GET /model` returns metadata; `POST /score` returns IDs/date plus confidence/decision/source/modelVersion/reasons. Evidence input is tenantId/employeeId/workDate strings and the twelve numeric features listed in `confidence/features.py`. Current validation includes numeric bounds but not authenticated tenant scope or typed date/UUID semantics; invalid numeric inputs return FastAPI 422, not the business error envelope. No auth, gateway route, outbox, or Kafka integration exists; do not deploy it publicly as-is. See [prototype limitations](../Architecture.MD).
