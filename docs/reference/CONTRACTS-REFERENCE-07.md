# Contracts reference 07

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### API-061: GET /api/v1/leave/admin/types

- Owner: `leave`; status: planned.
- Purpose and validation: Administration view includes inactive types.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query active.
- Request schema: Empty; response schema: Page`<LeaveType>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","code":"ANNUAL","name":"Annual leave","paid":true,"requiresAttachment":false,"maxDaysPerRequest":20,"allowHalfDay":true,"version":1}],"page":0,"size":20,"total":1}`

### API-062: POST /api/v1/leave/admin/types

- Owner: `leave`; status: planned.
- Purpose and validation: Create type; unique tenant code.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: LeaveTypeInput; response schema: LeaveType.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"code":"ANNUAL","name":"Annual leave","paid":true,"requiresAttachment":false,"maxDaysPerRequest":20,"allowHalfDay":true}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","code":"ANNUAL","name":"Annual leave","paid":true,"requiresAttachment":false,"maxDaysPerRequest":20,"allowHalfDay":true,"version":1}`

### API-063: PATCH /api/v1/leave/admin/types/{id}

- Owner: `leave`; status: planned.
- Purpose and validation: Version type; preserve old request policy snapshots.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: LeaveTypeInput; response schema: LeaveType.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"code":"ANNUAL","name":"Annual leave","paid":true,"requiresAttachment":false,"maxDaysPerRequest":20,"allowHalfDay":true}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","code":"ANNUAL","name":"Annual leave","paid":true,"requiresAttachment":false,"maxDaysPerRequest":20,"allowHalfDay":true,"version":1}`

### API-064: PUT /api/v1/leave/admin/policies/{typeId}

- Owner: `leave`; status: planned.
- Purpose and validation: Replace policy; If-Match required for existing, If-None-Match:* for initial create; 201 initial override.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: LeavePolicy; response schema: LeavePolicy.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"leaveTypeId":"019a0000-0000-7000-8000-000000000020","annualDays":18,"accrual":"UPFRONT","carryOverMax":5,"version":1}`
- Response example: `{"leaveTypeId":"019a0000-0000-7000-8000-000000000020","annualDays":18,"accrual":"UPFRONT","carryOverMax":5,"version":1}`

### API-065: POST /api/v1/leave/admin/balances/{employeeId}/adjust

- Owner: `leave`; status: planned.
- Purpose and validation: If-Match balance revision; ledger delta only; reason mandatory; no negative availability by default.
- Authorization and tenant scope: HR_ADMIN tenant; not self without independent approval; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: BalanceAdjustment; response schema: Balance.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: leave.balance.v1.
- Request example: `{"leaveTypeId":"019a0000-0000-7000-8000-000000000020","year":2026,"delta":1,"reason":"Approved entitlement adjustment"}`
- Response example: `{"employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","year":2026,"entitled":18,"carriedOver":0,"used":2,"pending":1,"available":15,"version":1}`

### API-066: POST /api/v1/leave/admin/year-end

- Owner: `leave`; status: planned.
- Purpose and validation: Durable per-tenant job; per-period ledger unique key; partial progress observable.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: JOB; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: YearEndInput; response schema: Job.
- Success: 202; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: leave.balance.v1 per applied employee.
- Request example: `{"fromYear":2026,"toYear":2027,"policyRevision":1}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","status":"QUEUED","statusUrl":"/api/v1/leave/admin/year-end/019a0000-0000-7000-8000-000000000020"}`

### API-067: GET /api/v1/leave/admin/year-end/{id}

- Owner: `leave`; status: planned; **proposed extension**.
- Purpose and validation: Proposed extension: observe existing asynchronous year-end command; statusUrl uses this route.
- Authorization and tenant scope: HR_ADMIN owning tenant; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Job.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","status":"QUEUED","statusUrl":"/api/v1/leave/admin/year-end/019a0000-0000-7000-8000-000000000020"}`

### API-068: POST /api/v1/leave/requests/{id}/resolve-conflict

- Owner: `leave`; status: planned; **proposed extension**.
- Purpose and validation: Proposed extension: resolve NEEDS_REVIEW against current conflict/revision; retained debit until resolution.
- Authorization and tenant scope: HR_ADMIN tenant; not requester; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: ConflictResolution; response schema: LeaveRequest.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: leave.request.v1,leave.balance.v1 if cancelled.
- Request example: `{"action":"CANCEL_LEAVE","conflictId":"019a0000-0000-7000-8000-000000000020","reason":"Employee worked; approved leave cancelled"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","employeeId":"019a0000-0000-7000-8000-000000000020","leaveTypeId":"019a0000-0000-7000-8000-000000000020","startDate":"2026-10-26","endDate":"2026-10-26","days":1,"status":"PENDING","syncState":"PENDING","version":1,"dayPortions":[{"date":"2026-10-26","portion":"FULL","units":1}]}`

### API-069: POST /api/v1/leave/attachments/presign

- Owner: `leave`; status: planned; **proposed extension**.
- Purpose and validation: Proposed extension: private evidence upload bound to own employee; necessary for existing attachment requirement.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: LeaveUploadInput; response schema: UploadTicket.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"fileName":"support.pdf","contentType":"application/pdf","size":1024,"sha256":"0000000000000000000000000000000000000000000000000000000000000000"}`
- Response example: `{"attachmentId":"019a0000-0000-7000-8000-000000000020","uploadUrl":"https://storage.example.test/signed-upload","requiredHeaders":{"Content-Type":"application/pdf"},"expiresAt":"2026-10-22T01:05:00Z","status":"PENDING"}`

### API-070: POST /api/v1/leave/attachments/{id}/complete

- Owner: `leave`; status: planned; **proposed extension**.
- Purpose and validation: Proposed extension: verify storage metadata/checksum; quarantine until security checks; no request event.
- Authorization and tenant scope: EMPLOYEE own upload; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Attachment.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","fileName":"support.pdf","contentType":"application/pdf","size":1024,"status":"QUARANTINED","version":1}`

### API-071: GET /api/v1/leave/attachments/{id}/download

- Owner: `leave`; status: planned; **proposed extension**.
- Purpose and validation: Proposed extension: attachment only for authorized leave; access audited; no arbitrary object key.
- Authorization and tenant scope: own employee or authorized leave approver/HR; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Download.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"downloadUrl":"https://storage.example.test/signed-download","expiresAt":"2026-10-22T01:05:00Z"}`

### API-072: GET /api/v1/requests/types

- Owner: `request`; status: planned.
- Purpose and validation: List published forms/chain roles without exposing private approver data.
- Authorization and tenant scope: any authenticated user; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query active.
- Request schema: Empty; response schema: Page`<RequestType>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","code":"MATERIAL","name":"Material request","formSchema":{"type":"object"},"approvalChain":["MANAGER","HR_ADMIN"],"version":1}],"page":0,"size":20,"total":1}`

### API-073: GET /api/v1/materials

- Owner: `request`; status: planned.
- Purpose and validation: Advisory active catalogue; not a stock reservation API.
- Authorization and tenant scope: any authenticated user; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query q.
- Request schema: Empty; response schema: Page`<Material>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","sku":"PEN-01","name":"Pen","unit":"piece","stockQty":100,"active":true,"version":1}],"page":0,"size":20,"total":1}`

### API-074: POST /api/v1/requests

- Owner: `request`; status: planned.
- Purpose and validation: Create draft or submit; validate lines/type schema; snapshot chain on submission.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: RequestInput; response schema: Request.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: request.workflow.v1 if submit=true.
- Request example: `{"typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"lines":[{"itemId":"019a0000-0000-7000-8000-000000000020","qty":2}],"submit":false}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}`

### API-075: GET /api/v1/requests/me

- Owner: `request`; status: planned.
- Purpose and validation: Own requests with stable pagination.
- Authorization and tenant scope: EMPLOYEE self; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query status,from,to.
- Request schema: Empty; response schema: Page`<Request>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}],"page":0,"size":20,"total":1}`

### API-076: GET /api/v1/requests/{id}

- Owner: `request`; status: planned.
- Purpose and validation: Detail with authorized steps/comments; attachment URL requested separately.
- Authorization and tenant scope: requester, assigned approver, HR_ADMIN; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Request.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}`

### API-077: PATCH /api/v1/requests/{id}

- Owner: `request`; status: planned.
- Purpose and validation: No requester/type/status mass assignment; cannot edit submitted snapshot.
- Authorization and tenant scope: requester while DRAFT; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: RequestInput; response schema: Request.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"lines":[{"itemId":"019a0000-0000-7000-8000-000000000020","qty":2}],"submit":false}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}`

### API-078: POST /api/v1/requests/{id}/submit

- Owner: `request`; status: planned.
- Purpose and validation: Validate schema/lines; resolve independent approvers; no missing-chain auto-approval.
- Authorization and tenant scope: requester while DRAFT; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Request.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: request.workflow.v1.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}`

### API-079: POST /api/v1/requests/{id}/cancel

- Owner: `request`; status: planned.
- Purpose and validation: DRAFT/PENDING to CANCELLED; waiting steps skipped with audit.
- Authorization and tenant scope: requester before final decision; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Reason; response schema: Request.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: request.workflow.v1.
- Request example: `{"reason":"Incorrect recorded time"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}`

### API-080: POST /api/v1/requests/{id}/comments

- Owner: `request`; status: planned.
- Purpose and validation: Append comment; visible only to participants; sanitize on display.
- Authorization and tenant scope: participant or HR_ADMIN; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: CommentInput; response schema: Comment.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"body":"Please review the quantity."}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","authorId":"019a0000-0000-7000-8000-000000000020","body":"Please review the quantity.","at":"2026-10-22T01:00:00Z"}`

### API-081: POST /api/v1/requests/{id}/attachments/presign

- Owner: `request`; status: planned.
- Purpose and validation: Persist pending metadata then issue bounded signed upload; key replay may refresh only URL, same attachment ID; exception to exact response replay for expiring URL.
- Authorization and tenant scope: requester editable draft; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: UploadInput; response schema: UploadTicket.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"fileName":"support.pdf","contentType":"application/pdf","size":1024,"sha256":"0000000000000000000000000000000000000000000000000000000000000000"}`
- Response example: `{"attachmentId":"019a0000-0000-7000-8000-000000000020","uploadUrl":"https://storage.example.test/signed-upload","requiredHeaders":{"Content-Type":"application/pdf"},"expiresAt":"2026-10-22T01:05:00Z","status":"PENDING"}`

### API-082: POST /api/v1/requests/{id}/attachments/{attachmentId}/complete

- Owner: `request`; status: planned; **proposed extension**.
- Purpose and validation: Proposed extension: verify stored object and enqueue quarantine scan; If-Match attachment version.
- Authorization and tenant scope: requester owning draft upload; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Attachment.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","fileName":"support.pdf","contentType":"application/pdf","size":1024,"status":"QUARANTINED","version":1}`

### API-083: GET /api/v1/requests/{id}/attachments/{attachmentId}/download

- Owner: `request`; status: planned; **proposed extension**.
- Purpose and validation: Proposed extension: READY only; short-lived signed URL and download audit.
- Authorization and tenant scope: participant or HR_ADMIN; shared token/tenant/object rules apply.
- Profile: READ; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Empty; response schema: Download.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"downloadUrl":"https://storage.example.test/signed-download","expiresAt":"2026-10-22T01:05:00Z"}`

### API-084: GET /api/v1/requests/inbox

- Owner: `request`; status: planned.
- Purpose and validation: Current step must target caller; role alone cannot approve every request.
- Authorization and tenant scope: MANAGER or HR_ADMIN assigned approver; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query status.
- Request schema: Empty; response schema: Page`<Request>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}],"page":0,"size":20,"total":1}`

### API-085: POST /api/v1/requests/{id}/approve

- Owner: `request`; status: planned.
- Purpose and validation: Lock step/request; advance one step; final APPROVED only when chain complete.
- Authorization and tenant scope: current assigned approver; not requester; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Decision; response schema: Request.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: request.workflow.v1.
- Request example: `{"note":"Reviewed supporting details"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}`

### API-086: POST /api/v1/requests/{id}/reject

- Owner: `request`; status: planned.
- Purpose and validation: Required note; terminal REJECTED; later steps skipped.
- Authorization and tenant scope: current assigned approver; not requester; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Decision; response schema: Request.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: request.workflow.v1.
- Request example: `{"note":"Reviewed supporting details"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}`

### API-087: POST /api/v1/requests/{id}/fulfill

- Owner: `request`; status: planned.
- Purpose and validation: APPROVED to FULFILLED; material handover audit; no authoritative inventory deduction guarantee.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CHANGE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: Decision; response schema: Request.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: request.workflow.v1.
- Request example: `{"note":"Reviewed supporting details"}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","requesterId":"019a0000-0000-7000-8000-000000000020","typeId":"019a0000-0000-7000-8000-000000000020","title":"Office materials","data":{},"status":"DRAFT","currentStep":null,"version":1,"steps":[]}`

### API-088: GET /api/v1/materials/admin

- Owner: `request`; status: planned.
- Purpose and validation: Catalogue administration including inactive items.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: LIST; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; query q,active.
- Request schema: Empty; response schema: Page`<Material>`.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: no body.
- Response example: `{"items":[{"id":"019a0000-0000-7000-8000-000000000020","sku":"PEN-01","name":"Pen","unit":"piece","stockQty":100,"active":true,"version":1}],"page":0,"size":20,"total":1}`

### API-089: POST /api/v1/materials/admin

- Owner: `request`; status: planned.
- Purpose and validation: Unique SKU; advisory stock nonnegative.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: CREATE; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: no path parameters; no query parameters.
- Request schema: MaterialInput; response schema: Material.
- Success: 201; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"sku":"PEN-01","name":"Pen","unit":"piece","stockQty":100,"active":true}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","sku":"PEN-01","name":"Pen","unit":"piece","stockQty":100,"active":true,"version":1}`

### API-090: PATCH /api/v1/materials/admin/{id}

- Owner: `request`; status: planned.
- Purpose and validation: Update advisory quantity with version; audit reason required when stockQty changes.
- Authorization and tenant scope: HR_ADMIN tenant; shared token/tenant/object rules apply.
- Profile: EDIT; shared headers, errors, transaction, pagination and retry behavior from section 2.2 apply.
- Parameters: UUID path identifiers as named; no query parameters.
- Request schema: MaterialInput; response schema: Material.
- Success: 200; explicit overrides in purpose take precedence. Errors: shared profile codes; endpoint validation yields 422 and state conflict 409.
- Side effects/events: no Kafka business event; local mutation/audit only for command profiles.
- Request example: `{"sku":"PEN-01","name":"Pen","unit":"piece","stockQty":100,"active":true}`
- Response example: `{"id":"019a0000-0000-7000-8000-000000000020","sku":"PEN-01","name":"Pen","unit":"piece","stockQty":100,"active":true,"version":1}`
