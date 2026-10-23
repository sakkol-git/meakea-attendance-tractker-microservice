# API guidelines

## Authority and examples

contracts/openapi.yaml is the sole future machine wire authority. Examples below are proposed fixtures, not implemented endpoints. Inspect the existing historical catalog and actual owner package first; add only a required slice.

## URLs and methods

Versioned plural kebab-case resources: POST /api/v1/departments creates an organization resource; GET /api/v1/departments lists only authorized tenant records. Action routes such as POST /api/v1/attendance/check-in represent explicit commands. GET never performs a business mutation.

```json
{"name":"Operations"}
```

## Status and errors

201 plus Location for committed create; 200 for completed transition/read; 202 only for a durable job with status URL; 204 for completed empty response. 400 syntax/DTO, 401 missing/invalid identity, 403 visible forbidden action, 404 hidden/foreign object, 409 state/key conflict, 412 stale If-Match, 422 domain rule, 428 missing precondition, 429 throttling, 503 transient unavailable.

```json
{
  "type": "urn:hrflow:problem:validation",
  "title": "Invalid request",
  "status": 400,
  "detail": "Department name is required.",
  "instance": "/api/v1/departments",
  "code": "VALIDATION_FAILED",
  "traceId": "019a0000-0000-7000-8000-000000000040"
}
```

Use application/problem+json for errors, with stable code extension and safe field details. Security filter failures and controller failures share the same contract. Do not echo password, duplicate-user existence, token or raw SQL.

## Pagination and sorting

Example query: GET /api/v1/departments?page=0&size=20&sort=createdAt,desc. Page nonnegative, size 1–100, sort/filter allowlist with stable ID tiebreaker; counts use identical tenant/resource scope.

```json
{"items":[],"page":0,"size":20,"total":0}
```

## Idempotency and versions

Retryable create/decision POST requires a UUID Idempotency-Key; stored scope is tenant + actor + method + canonical path plus body hash. Same command returns original status/resource, changed body returns IDEMPOTENCY_MISMATCH 409. Never cache credential secrets. Login/refresh have explicitly different throttling/rotation semantics.

```text
Idempotency-Key: 019a0000-0000-7000-8000-000000000041
If-Match: "3"
X-Correlation-Id: 019a0000-0000-7000-8000-000000000040
```

Mutable edits and decision commands use ETag/If-Match where a version exists. A response lost after commit is reconciled by the same key, never blindly submitted as a new command. Dates are YYYY-MM-DD, instants UTC ISO-8601, numeric balances exact; work date is explicit shift-local date.

## Tenant and compatibility

No body/path/header selects business tenant; authenticated principal does. Pre-auth login slug only selects credentials through the reviewed narrow path. Preserve additive v1 compatibility and update mock/live examples together; no duplicate client schema source. Every route enters the shared tenant/role/error test registry.

[Golden slice](GOLDEN-SLICE.md) and [canonical persistence](../REST-API-DATABASE-DESIGN.md).
