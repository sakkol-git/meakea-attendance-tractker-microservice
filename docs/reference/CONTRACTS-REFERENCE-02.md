# Contracts reference 02

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## 2. Reusable schemas and endpoint profiles

### 2.1 Schema notation

In schema declarations, `?` means nullable/optional; arrays use `[]`; all other fields are required unless stated as response-only. IDs/timestamps in examples are illustrative. Resource examples are representative minimal wire shapes, not existing DTOs. Resource outputs include ID, version, createdAt, updatedAt for mutable aggregates; immutable facts use ID/createdAt without a fictional update lifecycle. List schemas wrap the named resource in the section 1.2 envelope. Jobs return id/status/statusUrl, with READY downloadUrl only after authorization. Nested references below are within the same tenant; no cross-service foreign key is implied.

### Empty

No request body.

### Login

tenantSlug:string; email:string; password:string (write-only).

```json
{
  "tenantSlug": "demo",
  "email": "employee@example.test",
  "password": "example-only-secret"
}
```


### Refresh

refreshToken:string opaque secret; web transport may instead use protected refresh cookie.

```json
{
  "refreshToken": "example-only-opaque-token"
}
```


### Tokens

accessToken:string; tokenType:Bearer; expiresIn:int seconds; refreshToken:string (mobile only; web HttpOnly cookie).

```json
{
  "accessToken": "example-only-jwt",
  "tokenType": "Bearer",
  "expiresIn": 600,
  "refreshToken": "example-only-rotated-token"
}
```


### PasswordChange

currentPassword:string; newPassword:string, password policy validated; never logged.

```json
{
  "currentPassword": "example-old",
  "newPassword": "example-new-long-password"
}
```


### Profile

userId:uuid; employeeId:uuid?; tenantId:uuid; roles:string[]; locale:string; shift:object?.

```json
{
  "userId": "019a0000-0000-7000-8000-000000000020",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "tenantId": "019a0000-0000-7000-8000-000000000020",
  "roles": [
    "EMPLOYEE"
  ],
  "locale": "en",
  "shift": null
}
```


### JWKS

keys:public JWK[]; never contains private key parameters.

```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "example-key",
      "use": "sig",
      "alg": "RS256",
      "n": "example-modulus",
      "e": "AQAB"
    }
  ]
}
```


### TenantInput

name:string; slug:string unique lower-case; timezone:IANA; country:ISO country; ownerEmail:string for provisioning only.

```json
{
  "name": "Demo Company",
  "slug": "demo",
  "timezone": "Asia/Phnom_Penh",
  "country": "KH",
  "ownerEmail": "owner@example.test"
}
```


### Tenant

id:uuid; name:string; slug:string; timezone:IANA; country:string; status:ACTIVE|SUSPENDED; version:int.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "name": "Demo Company",
  "slug": "demo",
  "timezone": "Asia/Phnom_Penh",
  "country": "KH",
  "status": "ACTIVE",
  "version": 1
}
```


### TenantPatch

name?:string; timezone?:IANA; country?:string; no slug/status/role mutation through this route.

```json
{
  "name": "Demo Company Updated"
}
```


### EmployeeInput

employeeNo:string; fullName:string; email?:string; userId?:uuid; departmentId?:uuid; positionId?:uuid; managerId?:uuid; hireDate:date; employmentType:FULL_TIME|PART_TIME|CONTRACT; no self-supplied tenant.

```json
{
  "employeeNo": "E001",
  "fullName": "Demo Employee",
  "hireDate": "2026-10-01",
  "employmentType": "FULL_TIME"
}
```


### Employee

id:uuid; employeeNo:string; fullName:string; departmentId:uuid?; positionId:uuid?; managerId:uuid?; hireDate:date; status:ACTIVE|ON_LEAVE|TERMINATED; version:int.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "employeeNo": "E001",
  "fullName": "Demo Employee",
  "departmentId": null,
  "positionId": null,
  "managerId": null,
  "hireDate": "2026-10-01",
  "status": "ACTIVE",
  "version": 1
}
```


### EmployeePatch

fullName?:string; departmentId?:uuid; positionId?:uuid; managerId?:uuid; phone?:string; no direct termination/user-role mutation.

```json
{
  "fullName": "Updated Employee"
}
```


### Termination

endDate:date; reason:string; effective termination cannot precede hire date.

```json
{
  "endDate": "2026-10-31",
  "reason": "Contract ended"
}
```


### DeviceBinding

deviceId:string pseudonymous installation ID; platform:ANDROID|IOS; binding reset requires reauthentication.

```json
{
  "deviceId": "installation-example",
  "platform": "ANDROID"
}
```


### NamedInput

name:string; parentId?:uuid for departments; title?:string and level?:string for positions.

```json
{
  "name": "Operations"
}
```


### NamedResource

id:uuid; name:string; parentId:uuid?; version:int; position outputs also expose title/level.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "name": "Operations",
  "parentId": null,
  "version": 1
}
```


### PositionInput

Title:string non-empty ≤255 (JSON field `title`); level?:string; no `name` alias required.

```json
{
  "title": "Operations officer",
  "level": "L1"
}
```


### Position

id:uuid; title:string; level:string?; version:int. List uses standard page envelope.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "title": "Operations officer",
  "level": "L1",
  "version": 1
}
```


### SiteInput

name:string; latitude:number [-90,90]; longitude:number [-180,180]; radiusM:number >0; wifiSsid?:string; wifiBssid?:string; timezone:IANA; permission-gated evidence configuration.

```json
{
  "name": "Office",
  "latitude": 11.56,
  "longitude": 104.92,
  "radiusM": 100,
  "timezone": "Asia/Phnom_Penh"
}
```


### Site

SiteInput plus id:uuid and version:int; Wi-Fi identifiers restricted to authorized clients.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "name": "Office",
  "latitude": 11.56,
  "longitude": 104.92,
  "radiusM": 100,
  "timezone": "Asia/Phnom_Penh",
  "version": 1
}
```


### ShiftInput

name:string; startTime:HH:mm; endTime:HH:mm; graceMinutes:int 0..120; workingDays:int bitmask 1..127; siteId:uuid; timezone:IANA; MVP end > start.

```json
{
  "name": "Day shift",
  "startTime": "08:00",
  "endTime": "17:00",
  "graceMinutes": 10,
  "workingDays": 31,
  "siteId": "019a0000-0000-7000-8000-000000000020",
  "timezone": "Asia/Phnom_Penh"
}
```


### Shift

ShiftInput plus id:uuid, version:int; effective assignments are separate.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "name": "Day shift",
  "startTime": "08:00",
  "endTime": "17:00",
  "graceMinutes": 10,
  "workingDays": 31,
  "siteId": "019a0000-0000-7000-8000-000000000020",
  "timezone": "Asia/Phnom_Penh",
  "version": 1
}
```


### AssignmentInput

shiftId:uuid; validFrom:date; validTo:date? inclusive; no overlapping active assignment.

```json
{
  "shiftId": "019a0000-0000-7000-8000-000000000020",
  "validFrom": "2026-10-22",
  "validTo": null
}
```


### Assignment

id:uuid; employeeId:uuid; shiftId:uuid; validFrom:date; validTo:date?; version:int.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "employeeId": "019a0000-0000-7000-8000-000000000020",
  "shiftId": "019a0000-0000-7000-8000-000000000020",
  "validFrom": "2026-10-22",
  "validTo": null,
  "version": 1
}
```


### HolidayInput

date:date; name:string; kind:PUBLIC|COMPANY; unique date per tenant for MVP.

```json
{
  "date": "2026-11-09",
  "name": "Company holiday",
  "kind": "COMPANY"
}
```


### Holiday

HolidayInput plus id:uuid and version:int; removal produces inactive snapshot.

```json
{
  "id": "019a0000-0000-7000-8000-000000000020",
  "date": "2026-11-09",
  "name": "Company holiday",
  "kind": "COMPANY",
  "version": 1
}
```
