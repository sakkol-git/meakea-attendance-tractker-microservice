# REST API and database schema design

> **Decision:** ADR 008 supersedes ADR 006: seven services, JDBC, Kafka/Avro and Nuxt are the canonical design; ADR 007 tenant/error safeguards carry forward. REST endpoints route through `/api/v1`, persistence maps to six isolated PostgreSQL 16 schemas, and event delivery uses an owner-local transactional outbox to Kafka and consumer inbox receipts.

## Wire conventions and HTTP standards

The sole machine-readable wire authority is `contracts/openapi.yaml`, constructed incrementally alongside implemented features. All HTTP endpoints adhere to uniform conventions:

- Base path: All domain API endpoints are mounted under `/api/v1/{domain-prefix}`.
- Resource naming: URLs use lowercase, hyphen-separated plural nouns (e.g., `/api/v1/attendance/sessions`, `/api/v1/leave/requests`).
- Content negotiation: Request and response payloads use `application/json`; errors use `application/problem+json`. File uploads use `multipart/form-data`.
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
| 403 Forbidden | Visible operation forbidden | Valid principal but insufficient role; hidden cross-tenant resources use 404. |
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

The database instance hosts six logical schemas, enforcing service ownership:

| Schema name | Owner service | Core tables | Isolation constraints |
| --- | --- | --- | --- |
| `identity_org` | `identityorg` | `tenants`, `users`, `refresh_tokens`, `departments`, `positions`, `employees` | Composite unique on `(tenant_id, email)` and `(tenant_id, employee_code)`. |
| `attendance` | `attendance` | `raw_attendance_events`, `attendance_sessions`, `shift_rules`, `attendance_corrections` | One open effective session per tenant/employee; multiple closed sessions per work date allowed. |
| `leave` | `leave` | `leave_types`, `leave_balances`, `leave_requests`, `leave_request_days` | Composite unique on `(tenant_id, employee_id, leave_type_id, year)`. |
| `request` | `request` | `general_requests`, `request_approvals`, `request_attachments` | Composite foreign keys to request header; private attachment UUID. |
| `notification` | `notification` | Owner-local outbox tables; notification inbox and delivery state | Receipt uniqueness includes `(tenant_id, event_id, handler_name)`. |
| `reporting` | `reporting` | Owner-local audit logs; derived attendance/leave summaries | Owner audit is written with mutation; reporting receives derived facts only. |

## Transactional outbox and consumer inbox tables

Spring JDBC is the target; existing JPA dependencies are scaffold drift, not permission to adopt Hibernate. The following are proposed shapes, not installed migrations. Instantiate outbox_events in each publishing owner schema; the attendance example is shown. Each handler owns its inbox_receipts with its effect. This deliberate infrastructure-write exception does not permit cross-domain business-table access:

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
- No cross-schema joins: Repositories in `com.hrflow.leave` must never query tables in `identity_org` or `attendance`. Cross-service data is read from event-built local projections, never Java calls into another service or cross-service SQL.

## Historical references

Detailed endpoint lists, full entity schemas, and event definitions from the earlier seven-service analysis are preserved in [Historical reference index](reference/README.md). These serve as domain references; the approved service topology is defined by [Architecture](Architecture.md) and [Decisions](roadmap/DECISION-REGISTER.md).

## RLS and role requirements

Apply ENABLE and FORCE ROW LEVEL SECURITY to personal business tables with USING and WITH CHECK against transaction-local app.tenant_id. Missing/empty context denies access; set it with a parameterized set_config call inside the same transaction as queries. Tests use a non-owner runtime role with neither superuser nor BYPASSRLS and verify tenant A/B pool reuse, background jobs, rollback and insert denial.

Pre-auth credential selection is not business tenant authorization. A narrow, audited fixed-query credential lookup path may select by tenant slug/email before password verification; it cannot expose arbitrary business rows or execute caller-provided SQL. Review this exception before real users. Runtime/worker requests still establish trusted tenant only from verified principal or stored job/event. Backups use a separate controlled role and prove all-tenant restoration.

[ADR 007](adr/007-QUALITY-AND-TENANT-DEFENSE.md) specifies scope, role/grant boundaries and migration. Proposed DDL above is adapted and tested during implementation; it is not permission to change applied migrations. Owner-local append-only audit and exact numeric balance fields remain mandatory.

## Broker and identity boundary

Producer acknowledgment precedes marking an outbox row sent. Consumer effect and inbox receipt commit before Kafka offset acknowledgment. Crash/replay tests use real Kafka and PostgreSQL; Schema Registry compatibility is checked on every event schema change. Retain the actual contract root libs/hrflow-contracts/events until an inspect-first step approves a move. A JSONB outbox payload is internal storage, not a replacement for Avro wire contracts.

Gateway JWT verification is the source of trusted identity. Strip caller identity headers, emit only verified principal fields, reject direct service ingress, and test spoofing. Service method/resource authorization and scoped JDBC queries remain required. Header provenance is a conditional security gate until the network restriction is proved; ADR 008 does not claim that header naming alone satisfies Q04.

Physical identity schema is identity_org from infra/docker/postgres-init.sql; identity in the final architecture is the logical owner. Names in the table above remain proposed catalog names, not discovered tables. Inspect migrations before choosing singular/plural names. No cross-service foreign keys.
