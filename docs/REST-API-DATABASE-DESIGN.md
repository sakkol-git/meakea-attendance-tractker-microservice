# REST API and database schema design

> **Decision:** ADRs 006 and 007 establish the canonical API and database contracts for the modular monolith. REST endpoints route through `/api/v1`, persistence maps to six isolated PostgreSQL 16 schemas, and event delivery uses an in-database Transactional Outbox.

## Wire conventions and HTTP standards

The sole machine-readable wire authority is `contracts/openapi.yaml`, constructed incrementally alongside implemented features. All HTTP endpoints adhere to uniform conventions:

- Base path: All domain API endpoints are mounted under `/api/v1/{domain-prefix}`.
- Resource naming: URLs use lowercase, hyphen-separated plural nouns (e.g., `/api/v1/attendance/sessions`, `/api/v1/leave/requests`).
- Content negotiation: Request and response payloads use `application/json; errors use application/problem+json`. File uploads use `multipart/form-data`.
- Authentication header: Clients pass JWT access tokens via `Authorization: Bearer <token>`.
- Idempotency control: Retryable mutating POST commands require an `Idempotency-Key` header containing a client-generated UUID v4.

## Standard HTTP status codes and error responses

The API uses standard HTTP status codes to communicate outcome and error conditions:

| Status code | Meaning | Typical scenario |
| --- | --- | --- |
| 200 OK | Success with response body | Resource retrieved, entity updated synchronously. |
| 201 Created | Resource created | Entity created; returns created entity and `Location` header. |
| 202 Accepted | Async job accepted | Background export or report generation queued. |
| 400 Bad Request | Syntactic validation error | Malformed JSON, invalid query parameter format. |
| 401 Unauthorized | Authentication missing or invalid | Missing token, expired JWT, invalid signature. |
| 403 Forbidden | Authorization or tenant boundary denied | Valid token but wrong role or cross-tenant resource attempt. |
| 404 Not Found | Resource not found | ID does not exist within the caller's tenant scope. |
| 409 Conflict | State conflict | Idempotency key reused with different payload; stale If-Match uses 412. |
| 422 Unprocessable | Business invariant violated | Insufficient leave balance, shift overlap detected. |
| 503 Service Unavailable | Transient dependency unavailable | Database unavailable before commit; retry with same key. |

Standard problem response; clients branch on code, not detail. Authentication filter/authorization failures use the same shape. Never return stack traces or credential-existence information.

```json
{
  "type": "urn:hrflow:problem:leave-balance-insufficient",
  "title": "Leave request rejected",
  "status": 422,
  "detail": "Insufficient available leave.",
  "instance": "/api/v1/leave/requests",
  "code": "LEAVE_BALANCE_INSUFFICIENT",
  "traceId": "019a0000-0000-7000-8000-000000000040"
}
```

PUT/PATCH/decision commands require If-Match where a mutable version exists: 428 missing, 412 stale. Hidden/cross-tenant objects return 404; 403 is reserved for visible forbidden operations. Idempotency scope includes tenant, actor, method and canonical path plus body hash. No cached response stores refresh/login secrets.

## PostgreSQL domain schemas and table ownership

The database instance hosts six logical schemas, preventing monolithic table bloat and ensuring clean package isolation:

| Schema name | Owner package | Core tables | Isolation constraints |
| --- | --- | --- | --- |
| `identity_org` | `identityorg` | `tenants`, `users`, `refresh_tokens`, `departments`, `positions`, `employees` | Composite unique on `(tenant_id, email)` and `(tenant_id, employee_code)`. |
| `attendance` | `attendance` | `raw_attendance_events`, `attendance_sessions`, `shift_rules`, `attendance_corrections` | One open effective session per tenant/employee; multiple closed sessions per work date allowed. |
| `leave` | `leave` | `leave_types`, `leave_balances`, `leave_requests`, `leave_request_days` | Composite unique on `(tenant_id, employee_id, leave_type_id, year)`. |
| `request` | `request` | `general_requests`, `request_approvals`, `request_attachments` | Composite foreign keys to request header; private attachment UUID. |
| `notification` | `notification` | Owner-local outbox tables; notification inbox and delivery state | Receipt uniqueness includes `(tenant_id, event_id, handler_name)`. |
| `reporting` | `reporting` | Owner-local audit logs; derived attendance/leave summaries | Owner audit is written with mutation; reporting receives derived facts only. |

## Transactional outbox and consumer inbox tables

The following are proposed shapes, not installed migrations. Instantiate outbox_events in each publishing owner schema; the attendance example is shown. Each handler owns its inbox_receipts with its effect. This deliberate infrastructure-write exception does not permit cross-domain business-table access:

```sql
CREATE TABLE attendance.outbox_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_pending
ON attendance.outbox_events (status, created_at)
WHERE status = 'PENDING';
```

```sql
CREATE TABLE notification.inbox_receipts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    handler_name VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inbox_handler UNIQUE (tenant_id, event_id, handler_name)
);
```

## Multi-tenant persistence rules

Every table created across the six schemas must adhere to mandatory multi-tenancy rules:

- Tenant column: Every table must define `tenant_id UUID NOT NULL`.
- Foreign key scoping: Child tables must include `tenant_id` in foreign keys to parent tables, preventing cross-tenant child references:

```sql
CONSTRAINT fk_leave_request_employee
FOREIGN KEY (tenant_id, employee_id)
REFERENCES leave.employee_ref (tenant_id, employee_id)
```

- Query isolation: All application repository queries must include `WHERE tenant_id = :tenantId`.
- No cross-schema joins: Repositories in `com.hrflow.app.leave` must never query tables in `identity_org` or `attendance`. Cross-domain data is accessed via Java service interfaces or local projection tables.

## Historical references

Detailed endpoint lists, full entity schemas, and event definitions from the earlier seven-service analysis are preserved in [Historical reference index](reference/README.md). These serve as domain references; seven-service deployment and Kafka infrastructure are superseded by [Architecture](Architecture.md) and [Decisions](roadmap/DECISION-REGISTER.md).

## RLS and role requirements

Apply ENABLE and FORCE ROW LEVEL SECURITY to personal business tables with USING and WITH CHECK against transaction-local app.tenant_id. Missing/empty context denies access; set it with a parameterized set_config call inside the same transaction as queries. Tests use a non-owner runtime role with neither superuser nor BYPASSRLS and verify tenant A/B pool reuse, background jobs, rollback and insert denial.

Pre-auth credential selection is not business tenant authorization. A narrow, audited fixed-query credential lookup path may select by tenant slug/email before password verification; it cannot expose arbitrary business rows or execute caller-provided SQL. Review this exception before real users. Runtime/worker requests still establish trusted tenant only from verified principal or stored job/event. Backups use a separate controlled role and prove all-tenant restoration.

[ADR 007](adr/007-QUALITY-AND-TENANT-DEFENSE.md) specifies scope, role/grant boundaries and migration. Proposed DDL above is adapted and tested during implementation; it is not permission to change applied migrations. Owner-local append-only audit and exact numeric balance fields remain mandatory.
