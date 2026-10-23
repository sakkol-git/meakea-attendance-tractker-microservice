# Golden slice — create one department

## Why this slice and current evidence

Choose POST /api/v1/departments, a planned identity-org write in the preserved catalog, not a claim that a controller exists. A name-only department is smaller than employee lifecycle, check-in or leave. Source reinspection finds only identity-org startup class; class/table names below are proposed examples to be confirmed at inspection. Weeks 1–3 progressively establish the same contract/test/security recipe later features repeat.

## Weeks 1–3 ladder

- Week 1: versioned OpenAPI/example, diagnostic DB skeleton, problem shape, red/green test recipe and CI formatting/scanning/coverage baseline.
- Week 2: real login/JWT/refresh/throttle, trusted tenant/resource role, private confirmed-host deploy and first verified external backup/restore.
- Week 3: department migration/entity/repository/typed create, HR authority, shared tenant suite, non-owner RLS, architecture fitness, safe log/counter and redeployed smoke.

The diagnostic route is not a completed secure golden slice. Until Week 3 gate passes, later code cannot claim the full recipe is proven.

## Contract and validation fixture

```json
{"name":"Operations"}
```

201 returns stable id, name and version plus Location. Blank name is 400 VALIDATION_FAILED; anonymous is 401; non-HR visible action is 403; foreign object access is 404. Same retry key returns same resource and changed payload under that key is 409.

```java
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
record DepartmentInput(@NotBlank @Size(max = 100) String name) {}
```

This Java 21 DTO example requires the app's validation dependency, created in the scaffold step. Inspect actual DTO location; do not invent an existing DepartmentInput class or bind persistence entities directly.

## Persistence and non-owner defense

Proposed singular table name follows preserved schema design; confirm once in canonical migration inventory before adding code. Do not let plural names drift between SQL/tests/OpenAPI resource names.

```sql
CREATE TABLE identity_org.department (
    tenant_id uuid NOT NULL,
    id uuid NOT NULL,
    name text NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 100),
    version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
    PRIMARY KEY (tenant_id, id),
    UNIQUE (tenant_id, name)
);
ALTER TABLE identity_org.department ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.department FORCE ROW LEVEL SECURITY;
CREATE POLICY department_tenant ON identity_org.department
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
```

Set context parameterized with local=true inside the same query transaction; parameter comes from verified principal. Runtime cannot own/bypass table policies. Tests prove missing context, tenant A/B query and insert, pooled reuse and rollback; schema owner-only tests are invalid RLS evidence. Tenant-root and pre-auth credential exceptions are separately reviewed under ADR 007.

## Transaction and error behavior

Service obtains verified tenant/actor, checks HR authority, validates invariant, inserts row plus audit/command result/intent as required, commits, then returns. No provider call in transaction. A single MVC advice and Security handlers map errors; use Spring ProblemDetail or equivalent typed problem shape, not a stack trace or arbitrary exception message.

## Minimum test matrix

- Unit: trimmed/blank/oversized name and stable code mapping with fixed IDs/clock.
- PostgreSQL: clean/upgrade migration, same tenant duplicate, different tenants same name, FK/role/RLS, failed transaction leaves zero business/audit/intent rows.
- HTTP: contract/201/Location, validation 400, 401/403/404, scoped list/count, duplicate key and changed-body conflict.
- Architecture: controller only own service; no foreign internal repository; empty import test fails.
- Client smoke: React submits, Flutter mock agrees and real client observes committed response after auth integration.
- Operations: safe structured log/correlation, bounded counter, CI scans, private real-host smoke and valid external restore evidence.

## Repeat the golden slice

Every later feature step set says “repeat the golden slice” and lists only its delta: new domain invariant, new persistence/lock rule, new role/resource permission, new event/side effect or new client state. Reuse shared problem handler/tenant suite/CI rather than paste a second framework.

A feature is not done because the happy write returned 201. Its negative, tenant, concurrency, rollback and release checks determine the gate. Use [review](CODE-REVIEW-CHECKLIST.md), [tests](TEST-STRATEGY.md), [standards](ENGINEERING-STANDARDS.md) and [API guide](API-GUIDELINES.md).
