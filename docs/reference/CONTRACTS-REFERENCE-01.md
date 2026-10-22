# Contracts reference 01

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

- Revision: 2026-10-23; status: planned implementation contract.
- All business endpoints and tables below are **planned**, not implemented controllers or migrations.
- New contract details and explicitly marked extensions are **proposed** pending design review.
- The only implemented HTTP API is the standalone confidence prototype, described separately.
- Related: [architecture](../Architecture.MD), [delivery plan](../PLAN.md), [decision records](../Architecture.MD).
- Existing endpoint/entity coverage has been retained; malformed tables and ambiguous semantics are replaced here.

## 1. Shared contract conventions

### 1.1 Addressing and identity

Client prefix is `/api/v1`; local target origin is `http://localhost:8080`. No production domain is established. JWKS alone is at root `/.well-known/jwks.json`. Target ports: identity-org 8101, attendance 8102, leave 8103, request 8104, notification 8105, reporting 8106. Attendance currently configures 8082; target routing is not implemented. Identity schema is `identity_org`, matching bootstrap SQL rather than the old draft's `identity` spelling. All prefixes route to the owning service; gateway preserves the public contract while private path rewriting is an implementation choice. Authentication uses bearer JWT validated at gateway and service, except explicit auth/public/control-plane endpoints. Trusted tenant is token `tenantId`, never a client-selected header/body. Roles are EMPLOYEE/MANAGER/HR_ADMIN/OWNER with no automatic role inheritance. `self` requires the token-linked employee; `team` means current direct reports; `participant` means requester or assigned approver. All object queries include tenant and resource scope; cross-tenant/hidden objects return 404. Platform provisioning uses a separate control-plane credential, not a tenant OWNER token.

### 1.2 Data and validation

IDs are UUID strings; UUIDv7 generation is proposed, but validation accepts valid UUID identifiers without assuming sort semantics. JSON is camelCase; SQL is snake_case. Unknown command fields are rejected. Instants are ISO-8601 UTC (`2026-10-22T01:00:00Z`), local dates `YYYY-MM-DD`, local times `HH:mm`, zones IANA strings. Server-generated fields (tenantId, actor, status transitions, balances, revision) are never mass assigned. Strings default to trimmed non-empty ≤255 characters; reason/comment ≤2000; email ≤254 and normalized for uniqueness. Pagination: `page` ≥0, `size` 1–100 (default 20), allowlisted `sort` (default createdAt desc, ID tiebreaker). Date ranges default to current work date/month as endpoint indicates, maximum 366 days proposed; exports may use separately approved limits. List response: `{items, page, size, total}`; projected responses additionally contain `asOf`, `projectionLagMs`, `isStale`. GET is side-effect free, except access audit logging; lists never bypass scope for total counts.

### 1.3 Headers and concurrency

All protected routes: `Authorization: Bearer <access-token>`. Optional validated UUID `X-Correlation-Id` is echoed/generated; client identity headers are stripped. POST commands require `Idempotency-Key: <uuid>` unless explicitly exempt below. Scope key by tenant, actor, method, canonical path; store canonical body hash and committed response in the same transaction. Same key/body returns original status/body/Location; different body returns 409 IDEMPOTENCY_MISMATCH. Concurrent duplicates serialize at a unique constraint; an in-flight operation may return 409 IDEMPOTENCY_IN_PROGRESS with Retry-After if lock wait exceeds budget. Only committed success is cached; validation/transient failures can retry safely. Proposed retention is ≥30 days; mobile automatic retry horizon ≤7 days, after which user reconciliation is required. Mutable resources expose `version` and ETag `"<version>"`; PATCH/PUT/DELETE and decision commands require `If-Match` unless explicitly exempt. Missing If-Match is 428; mismatched version is 412. State conflicts are 409, business policy violations 422. Create commands use row locks/uniques to handle concurrent creation even without an existing version.

### 1.4 HTTP and errors

201 = created and durably committed, with Location; 200 = read or completed transition; 204 = completed deletion/revocation without body. 202 = durable background job accepted with status URL; not every event-producing write is asynchronous HTTP. 400 = invalid syntax/field shape; 401 = authentication failure; 403 = visible but unauthorized operation; 404 = absent/hidden object. 409 = state/idempotency/concurrency conflict; 412 = stale precondition; 413 = body too large; 422 = business rule; 428 = missing precondition. 429 includes Retry-After; 503 indicates transient unavailability/reference freshness failure when retry is appropriate. No stack traces, tokens, private location, or object keys appear in error messages.

```json
{
  "code": "LEAVE_BALANCE_INSUFFICIENT",
  "message": "Insufficient available leave.",
  "details": [
    {
      "field": "endDate",
      "issue": "Requested units exceed availability."
    }
  ],
  "traceId": "019a0000-0000-7000-8000-000000000040"
}
```


For every protected endpoint, common errors are 400/401/403/404/429/503; mutating operations add 409/422 and precondition errors where applicable. Read retries are safe; command retries preserve key/body/version. A timeout after commit must be reconciled by key, not a new submission. Normal transaction boundary is one owner-service DB transaction including audit, idempotency result, and outbox for listed business events. No cross-service transaction is implied. GET observes owner state or a freshness-labeled local projection.
