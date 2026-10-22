# Contracts reference 03

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### Audit

id:uuid; actorId:uuid?; action:string; entity:string; entityId:uuid?; diff:bounded safe object; at:instant.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "actorId": "019a0000-0000-7000-8000-000000000020",
  "action": "EMPLOYEE_UPDATED",
  "entity": "employee",
  "entityId": "019a0000-0000-7000-8000-000000000020",
  "diff": {
    "fullName": "Updated Employee"
  },
  "at": "2026-10-22T01:00:00Z"
}
```


### CheckIn

deviceId:string; occurredAt:instant; source:TAP|NUDGE; latitude?:number; longitude?:number; accuracyM?:number >=0; employee comes from token; manual source forbidden.

```json
{
  "deviceId": "installation-example",
  "occurredAt": "2026-10-22T01:00:00Z",
  "source": "TAP"
}
```


### CheckOut

sessionId:uuid; occurredAt:instant; deviceId:string; checkout >= check-in; session owned by caller.

```json
{
  "sessionId": "019a0000-0000-7000-8000-000000000020",
  "occurredAt": "2026-10-22T10:00:00Z",
  "deviceId": "installation-example"
}
```


### Session

id:uuid; employeeId:uuid; workDate:date; checkInAt:instant; checkOutAt:instant?; source:TAP|NUDGE|INFERRED|MANUAL; confidence:number? 0..1; reason:string?; confirmStatus:NONE|PENDING|CONFIRMED|REJECTED; effective:boolean; version:int.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "workDate": "2026-10-22",
  "checkInAt": "2026-10-22T01:00:00Z",
  "checkOutAt": null,
  "source": "TAP",
  "confidence": null,
  "reason": null,
  "confirmStatus": "NONE",
  "effective": true,
  "version": 1
}
```


### EvidenceBatch

items:EvidenceInput[1..100]; all-or-nothing admission; item eventId is stable across batch retries.

```json
{
  "items": [
    {
      "eventId": "019a0000-0000-7000-8000-000000000020",
      "kind": "TAP",
      "occurredAt": "2026-10-22T01:00:00Z",
      "deviceId": "installation-example",
      "payload": {}
    }
  ]
}
```


### EvidenceInput

eventId:uuid; kind:TAP|MANUAL|GEOFENCE_ENTER|GEOFENCE_EXIT|WIFI|BLE; occurredAt:instant; deviceId:string; latitude?:number; longitude?:number; accuracyM?:number; payload:bounded object. MANUAL accepted only via HR/correction server path.

```json
{
  "eventId": "019a0000-0000-7000-8000-000000000020",
  "kind": "TAP",
  "occurredAt": "2026-10-22T01:00:00Z",
  "deviceId": "installation-example",
  "payload": {}
}
```


### EvidenceReceipt

acceptedIds:uuid[]; duplicates:uuid[]; no automatic attendance conclusion implied.

```json
{
  "acceptedIds": [
    "019a0000-0000-7000-8000-000000000020"
  ],
  "duplicates": []
}
```


### Evidence

EvidenceInput plus id:uuid, receivedAt:instant, employeeId:uuid, provenance:string; raw coordinates restricted to HR.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "eventId": "019a0000-0000-7000-8000-000000000020",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "kind": "TAP",
  "occurredAt": "2026-10-22T01:00:00Z",
  "receivedAt": "2026-10-22T01:00:00Z",
  "deviceId": "installation-example",
  "payload": {},
  "provenance": "AUTHENTICATED_DEVICE"
}
```


### Today

workDate:date; shift:Shift?; sessions:Session[]; dailyStatus:DailyStatus?; nextAction:CHECK_IN|CHECK_OUT|REVIEW|NONE.

```json
{
  "workDate": "2026-10-22",
  "shift": null,
  "sessions": [],
  "dailyStatus": null,
  "nextAction": "REVIEW"
}
```


### DailyStatus

employeeId:uuid; workDate:date; status:PRESENT|LATE|ABSENT|LEAVE_EARLY|ON_LEAVE|HOLIDAY|DAY_OFF; dayType:string; attendanceState:string; late:boolean; earlyLeave:boolean; workedMinutes:int?; reviewRequired:boolean; version:int.

```json
{
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "workDate": "2026-10-22",
  "status": "PRESENT",
  "dayType": "WORKING",
  "attendanceState": "OPEN",
  "late": false,
  "earlyLeave": false,
  "workedMinutes": null,
  "reviewRequired": false,
  "version": 1
}
```


### Reason

reason:string non-empty ≤2000; approval may use optional note instead as specified.

```json
{
  "reason": "Incorrect recorded time"
}
```


### Decision

note?:string ≤2000; reject requires non-empty note; no state supplied by client.

```json
{
  "note": "Reviewed supporting details"
}
```


### CorrectionInput

sessionId?:uuid; workDate:date; newCheckIn:instant; newCheckOut:instant?; reason:string; target employee is self.

```json
{
  "sessionId": "019a0000-0000-7000-8000-000000000020",
  "workDate": "2026-10-22",
  "newCheckIn": "2026-10-22T01:00:00Z",
  "newCheckOut": "2026-10-22T10:00:00Z",
  "reason": "Recorded checkout was missing"
}
```


### Correction

id:uuid; sessionId:uuid?; employeeId:uuid; workDate:date; newCheckIn:instant; newCheckOut:instant?; reason:string; status:PENDING|APPROVED|REJECTED; version:int; decisions:object[].

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "sessionId": "019a0000-0000-7000-8000-000000000020",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "workDate": "2026-10-22",
  "newCheckIn": "2026-10-22T01:00:00Z",
  "newCheckOut": null,
  "reason": "Recorded time was incorrect",
  "status": "PENDING",
  "version": 1,
  "decisions": []
}
```


### ManualSession

employeeId:uuid; workDate:date; checkInAt:instant; checkOutAt:instant?; reason:string; conflict with leave flags review.

```json
{
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "workDate": "2026-10-22",
  "checkInAt": "2026-10-22T01:00:00Z",
  "checkOutAt": "2026-10-22T10:00:00Z",
  "reason": "Approved missing record"
}
```


### LeaveTypeInput

code:ANNUAL|SICK|SPECIAL|UNPAID; name:string; paid:boolean; requiresAttachment:boolean; maxDaysPerRequest:number >0; allowHalfDay:boolean.

```json
{
  "code": "ANNUAL",
  "name": "Annual leave",
  "paid": true,
  "requiresAttachment": false,
  "maxDaysPerRequest": 20,
  "allowHalfDay": true
}
```


### LeaveType

LeaveTypeInput plus id:uuid and version:int.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "code": "ANNUAL",
  "name": "Annual leave",
  "paid": true,
  "requiresAttachment": false,
  "maxDaysPerRequest": 20,
  "allowHalfDay": true,
  "version": 1
}
```


### Balance

employeeId:uuid; leaveTypeId:uuid; year:int; entitled:decimal; carriedOver:decimal; used:decimal; pending:decimal; available:decimal; version:int.

```json
{
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "leaveTypeId": "019a0000-0000-7000-8000-000000000020",
  "year": 2026,
  "entitled": 18,
  "carriedOver": 0,
  "used": 2,
  "pending": 1,
  "available": 15,
  "version": 1
}
```


### LeaveInput

leaveTypeId:uuid; startDate:date; endDate:date; halfDay:NONE|AM|PM; reason:string; attachmentId?:uuid (verified private upload); days calculated server-side.

```json
{
  "leaveTypeId": "019a0000-0000-7000-8000-000000000020",
  "startDate": "2026-10-26",
  "endDate": "2026-10-26",
  "halfDay": "NONE",
  "reason": "Personal leave"
}
```


### LeaveRequest

id:uuid; employeeId:uuid; leaveTypeId:uuid; startDate:date; endDate:date; days:decimal; status:PENDING|APPROVED|REJECTED|CANCELLED|NEEDS_REVIEW; syncState:PENDING|APPLIED|CONFLICT|UNKNOWN; version:int; dayPortions:object[].

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "leaveTypeId": "019a0000-0000-7000-8000-000000000020",
  "startDate": "2026-10-26",
  "endDate": "2026-10-26",
  "days": 1,
  "status": "PENDING",
  "syncState": "PENDING",
  "version": 1,
  "dayPortions": [
    {
      "date": "2026-10-26",
      "portion": "FULL",
      "units": 1
    }
  ]
}
```


### LeavePolicy

leaveTypeId:uuid; annualDays:decimal >=0; accrual:UPFRONT|MONTHLY; carryOverMax:decimal >=0; version:int.

```json
{
  "leaveTypeId": "019a0000-0000-7000-8000-000000000020",
  "annualDays": 18,
  "accrual": "UPFRONT",
  "carryOverMax": 5,
  "version": 1
}
```


### BalanceAdjustment

leaveTypeId:uuid; year:int; delta:decimal nonzero; reason:string; no direct used/pending overwrite.

```json
{
  "leaveTypeId": "019a0000-0000-7000-8000-000000000020",
  "year": 2026,
  "delta": 1,
  "reason": "Approved entitlement adjustment"
}
```


### YearEndInput

fromYear:int; toYear:int = fromYear+1; policyRevision:int; per employee/type ledger idempotency required.

```json
{
  "fromYear": 2026,
  "toYear": 2027,
  "policyRevision": 1
}
```


### Job

id:uuid; status:QUEUED|RUNNING|READY|FAILED|EXPIRED; statusUrl:string; downloadUrl?:string; errorCode?:string.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "status": "QUEUED",
  "statusUrl": "/api/v1/reports/exports/019a0000-0000-7000-8000-000000000020"
}
```
