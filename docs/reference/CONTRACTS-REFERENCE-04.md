# Contracts reference 04

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### RequestTypeInput

code:MATERIAL|CERTIFICATE|OVERTIME|OTHER; name:string; formSchema:bounded JSON schema object; approvalChain:role[]; no executable expressions.

```json
{
  "code": "MATERIAL",
  "name": "Material request",
  "formSchema": {
    "type": "object"
  },
  "approvalChain": [
    "MANAGER",
    "HR_ADMIN"
  ]
}
```


### RequestType

RequestTypeInput plus id:uuid and version:int.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "code": "MATERIAL",
  "name": "Material request",
  "formSchema": {
    "type": "object"
  },
  "approvalChain": [
    "MANAGER",
    "HR_ADMIN"
  ],
  "version": 1
}
```


### MaterialInput

sku:string; name:string; unit:string; stockQty:decimal >=0 advisory only; active:boolean; reason?:string, required for stockQty changes on PATCH.

```json
{
  "sku": "PEN-01",
  "name": "Pen",
  "unit": "piece",
  "stockQty": 100,
  "active": true
}
```


### Material

MaterialInput plus id:uuid and version:int; stock does not imply reservation guarantee.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "sku": "PEN-01",
  "name": "Pen",
  "unit": "piece",
  "stockQty": 100,
  "active": true,
  "version": 1
}
```


### RequestInput

typeId:uuid; title:string; description?:string; data:object matching type revision; lines?:[{itemId:uuid,qty:decimal >0,note?:string}]; submit?:boolean default false.

```json
{
  "typeId": "019a0000-0000-7000-8000-000000000020",
  "title": "Office materials",
  "data": {},
  "lines": [
    {
      "itemId": "019a0000-0000-7000-8000-000000000020",
      "qty": 2
    }
  ],
  "submit": false
}
```


### Request

id:uuid; requesterId:uuid; typeId:uuid; title:string; data:object; status:DRAFT|PENDING|APPROVED|REJECTED|CANCELLED|FULFILLED; currentStep:int?; version:int; steps:object[].

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "requesterId": "019a0000-0000-7000-8000-000000000020",
  "typeId": "019a0000-0000-7000-8000-000000000020",
  "title": "Office materials",
  "data": {},
  "status": "DRAFT",
  "currentStep": null,
  "version": 1,
  "steps": []
}
```


### CommentInput

body:string non-empty ≤2000; append-only participant comment.

```json
{
  "body": "Please review the quantity."
}
```


### Comment

id:uuid; authorId:uuid; body:string; at:instant.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "authorId": "019a0000-0000-7000-8000-000000000020",
  "body": "Please review the quantity.",
  "at": "2026-10-22T01:00:00Z"
}
```


### UploadInput

fileName:string safe display name; contentType:allowlisted string; size:int 1..10485760; sha256:string; object key server-generated.

```json
{
  "fileName": "support.pdf",
  "contentType": "application/pdf",
  "size": 1024,
  "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
}
```


### UploadTicket

attachmentId:uuid; uploadUrl:string short-lived; requiredHeaders:object; expiresAt:instant; status:PENDING.

```json
{
  "attachmentId": "019a0000-0000-7000-8000-000000000020",
  "uploadUrl": "https://storage.example.test/signed-upload",
  "requiredHeaders": {
    "Content-Type": "application/pdf"
  },
  "expiresAt": "2026-10-22T01:05:00Z",
  "status": "PENDING"
}
```


### Attachment

id:uuid; fileName:string; contentType:string; size:int; status:PENDING|QUARANTINED|READY|REJECTED; version:int.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "fileName": "support.pdf",
  "contentType": "application/pdf",
  "size": 1024,
  "status": "QUARANTINED",
  "version": 1
}
```


### Download

downloadUrl:string; expiresAt:instant; no public object key.

```json
{
  "downloadUrl": "https://storage.example.test/signed-download",
  "expiresAt": "2026-10-22T01:05:00Z"
}
```


### DeviceInput

platform:ANDROID|IOS|WEB; token:string provider secret; installationId:string; never logged.

```json
{
  "platform": "ANDROID",
  "token": "example-provider-token",
  "installationId": "installation-example"
}
```


### Device

id:uuid; platform:string; active:boolean; lastSeenAt:instant; version:int; no raw token echo.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "platform": "ANDROID",
  "active": true,
  "lastSeenAt": "2026-10-22T01:00:00Z",
  "version": 1
}
```


### Notification

id:uuid; category:string; title:string; body:string; data:bounded object; readAt:instant?; createdAt:instant.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "category": "ATTENDANCE",
  "title": "Attendance updated",
  "body": "Open HRFlow to review.",
  "data": {},
  "readAt": null,
  "createdAt": "2026-10-22T01:00:00Z"
}
```


### Count

count:int >=0.

```json
{
  "count": 2
}
```


### ReadReceipt

updated:int >=0; cutoff:instant prevents marking newly arriving items read.

```json
{
  "updated": 2,
  "cutoff": "2026-10-22T01:00:00Z"
}
```


### Preferences

locale:en|km; categories:[{category:string,push:boolean,email:boolean,inApp:boolean}]; version:int.

```json
{
  "locale": "en",
  "categories": [
    {
      "category": "ATTENDANCE",
      "push": true,
      "email": false,
      "inApp": true
    }
  ],
  "version": 1
}
```


### Template

code:string; channel:PUSH|EMAIL|IN_APP; locale:en|km; subject?:string; body:string; version:int; bounded allowlisted placeholders only.

```json
{
  "code": "ATTENDANCE_UPDATED",
  "channel": "IN_APP",
  "locale": "en",
  "subject": null,
  "body": "Open HRFlow to review attendance.",
  "version": 1
}
```


### Dashboard

workDate:date; counts:{present:int,late:int,absent:int,onLeave:int}; asOf:instant; projectionLagMs:int; isStale:boolean.

```json
{
  "workDate": "2026-10-22",
  "counts": {
    "present": 10,
    "late": 1,
    "absent": 0,
    "onLeave": 2
  },
  "asOf": "2026-10-22T01:00:00Z",
  "projectionLagMs": 500,
  "isStale": false
}
```


### ReportRow

dimension:string; period:string; count:int; units?:decimal; minutes?:int; asOf:instant; projectionLagMs:int; isStale:boolean; concrete dimensions depend on report route.

```json
{
  "dimension": "ANNUAL",
  "period": "2026-10",
  "count": 2,
  "units": 2,
  "asOf": "2026-10-22T01:00:00Z",
  "projectionLagMs": 500,
  "isStale": false
}
```


### ExportInput

kind:ATTENDANCE|LEAVE|REQUESTS; format:CSV|XLSX (CSV MVP); from:date; to:date; departmentId?:uuid; bounded allowed filters.

```json
{
  "kind": "ATTENDANCE",
  "format": "CSV",
  "from": "2026-10-01",
  "to": "2026-10-22"
}
```


### ConflictResolution

action:KEEP_LEAVE|CANCEL_LEAVE; conflictId:uuid; reason:string; partial resolution deferred; attendance correction uses its own flow.

```json
{
  "action": "CANCEL_LEAVE",
  "conflictId": "019a0000-0000-7000-8000-000000000020",
  "reason": "Employee worked; approved leave cancelled"
}
```


### LeaveUploadInput

UploadInput fields; upload is bound to own employee and can only attach to own leave request.

```json
{
  "fileName": "support.pdf",
  "contentType": "application/pdf",
  "size": 1024,
  "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
}
```


### 2.2 Reusable behavior profiles

Each endpoint entry below inherits the shared conventions and its named profile. These are normative parts of every entry, avoiding repeated security and transaction boilerplate.

| Profile | Headers and request | Success and response | Transaction, side effects, errors |
|---|---|---|---|
| READ | Bearer, correlation; no body | 200 named resource; ETag if mutable | Scoped owner/projection read; no events; common errors |
| LIST | READ plus endpoint filters and page/size/sort | 200 Page`<Resource>` | Scoped count and items; freshness if projected; no events |
| CREATE | Bearer, JSON, key | 201 resource + Location | Owner DB + audit + key result + listed outbox; 409/422 |
| CHANGE | Bearer, JSON, key, If-Match | 200 resource + new ETag | Lock current aggregate, authorize transition; atomic audit/outbox/key; 409/412/422/428 |
| EDIT | Bearer, JSON, If-Match | 200 resource + ETag | Typed update plus version increment/audit/outbox; retries use same precondition; 412/428 |
| REMOVE | Bearer, If-Match; no body | 204, no body | Soft delete/revoke and audit/outbox; repeat after disappearance 404; 412/428 |
| JOB | Bearer, JSON, key | 202 Job + Location=statusUrl | Persist owner job + key result; worker transaction later; 409/422 |
| MARK | Bearer, key; no version required | 200 receipt/resource | Monotonic own notification update; no business event; replay-safe |
| STREAM | Bearer, Accept:text/event-stream, Last-Event-ID optional | 200 event stream | Read own durable notifications; no mutation; reconnect reset on expired cursor |
| AUTH | Public JSON credential body; no key/If-Match | 200 Tokens | Rate-limited credential verification/refresh-family locking; 400/401/429/503 |

PATCH fields are the optional subset of the corresponding input schema unless a dedicated patch schema is named; required-on-create fields cannot be set null unless declared nullable. Path `{id}` and named IDs are UUID and must resolve within tenant/scope; `{code}` is an allowlisted template code. Unless listed otherwise, query parameters are none. Named list filters are optional and validated; unsupported filters return 400. Event names below refer to full catalog snapshots, not direct broker sends inside HTTP transactions.
