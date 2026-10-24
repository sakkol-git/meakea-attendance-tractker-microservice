# Golden slice — create one department

## Why this slice and current evidence

Choose POST /api/v1/departments, a planned identity-org write in the preserved catalog, not a claim that a controller exists. A name-only department is smaller than employee lifecycle, check-in or leave. Source reinspection finds only identity-org startup class; class/table names below are proposed examples to be confirmed at inspection. Weeks 1–3 progressively establish the same contract/test/security recipe later features repeat.

## Weeks 1–3 ladder

- Week 1: versioned OpenAPI/example, diagnostic DB skeleton, problem shape, red/green test recipe and CI formatting/scanning/coverage baseline.
- Week 2: real login/JWT/refresh/throttle, trusted tenant/resource role, private confirmed-host deploy and first verified external backup/restore.
- Week 3: department migration/JPA repository/typed create, HR authority, shared tenant suite, non-owner RLS, architecture fitness, safe log/counter and redeployed smoke.

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

This Java 21 DTO example requires the app's validation dependency, verified in services/identity-org/pom.xml during the dependency step. Inspect actual DTO location; do not invent an existing DepartmentInput class or bind persistence entities directly.

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

Service obtains verified tenant/actor, checks HR authority, validates invariant, inserts row plus audit/command result/intent as required, commits, then returns. No provider call in transaction. Each service uses the shared problem contract in its MVC advice/Security handlers; the reactive gateway uses its reactive error writer; use Spring ProblemDetail or equivalent typed problem shape, not a stack trace or arbitrary exception message.

## Minimum test matrix

- Unit: trimmed/blank/oversized name and stable code mapping with fixed IDs/clock.
- PostgreSQL: clean/upgrade migration, same tenant duplicate, different tenants same name, FK/role/RLS, failed transaction leaves zero business/audit/intent rows.
- HTTP: contract/201/Location, validation 400, 401/403/404, scoped list/count, duplicate key and changed-body conflict.
- Architecture: controller only own service; no foreign internal repository; empty import test fails.
- Client smoke: Nuxt submits, Flutter mock agrees and real client observes committed response after auth integration.
- Operations: safe structured log/correlation, bounded counter, CI scans, private real-host smoke and valid external restore evidence.

## Repeat the golden slice

Every later feature step set says “repeat the golden slice” and lists only its delta: new domain invariant, new persistence/lock rule, new role/resource permission, new event/side effect or new client state. Reuse shared problem handler/tenant suite/CI rather than paste a second framework.

A feature is not done because the happy write returned 201. Its negative, tenant, concurrency, rollback and release checks determine the gate. Use [review](CODE-REVIEW-CHECKLIST.md), [tests](TEST-STRATEGY.md), [standards](ENGINEERING-STANDARDS.md) and [API guide](API-GUIDELINES.md).

## Ordered reference ladder and reuse

These are real enforcing IDs for first-time construction, not new extra hours. Later feature sets repeat the shared fixture against their own route and the named delta; passing department tests alone does not prove attendance or leave security.

| Rung | Observable practice | Enforcing ID |
| --- | --- | --- |
| 01 Inspect | Actual source/dirty state and current names | W01-S01 |
| 02 Contract | Versioned input/output/error fixture | W01-S10 |
| 03 Client contract | One mock from the sole OpenAPI | W01-S14 |
| 04 Error | Safe MVC problem shape | W01-S16 |
| 05 Reactive edge | Same gateway error shape | W01-S17 |
| 06 Database | Real PostgreSQL and clean migration | W01-S08 |
| 07 CI | Every-push checks and locked versions | W01-S32 |
| 08 Identity | Verified access claims | W02-S05 |
| 09 Tenant | Caller headers cannot select tenant | W02-S06 |
| 10 Method permission | Independent role/resource checks | W02-S14 |
| 11 Browser security | Origin/CSRF before cookie mutation | W02-S20 |
| 12 Migration red | Constraint assertion for department/employee owner | W03-S06 |
| 13 Migration green | Owned keys/constraints and failure rollback | W03-S07 |
| 14 Typed write | Minimum department input, JPA repository and 201 result | W03-S09 |
| 15 Tenant defense | Personal-table RLS | W03-S25 |
| 16 Role/pool negatives | Non-owner role/pool reuse and insert denial | W03-S34 |
| 17 Architecture | Nonempty layer/module fitness | W03-S28 |
| 18 Event intent | Owner transaction and Kafka snapshot publication | W03-S32 |
| 19 Delivery integration | Attendance owner bootstrap without foreign SQL | W03-S35 |
| 20 Release | Reviewed private redeploy | W03-S39 |
| 21 Recover | Verified external backup and scratch restore | W03-S40 |

## Major task calibration

Each major task traverses the shared test/contract/authorization/observe/review rungs above and these ordered feature-specific atoms. Reused steps are not charged twice; each new endpoint still runs its own registered negative/tenant assertions.

| Major task | Ordered atomic delta IDs | Additional behavior beyond reference |
| --- | --- | --- |
| Check-in, checkout and correction | W04-S01, W04-S02, W04-S03, W04-S04, W04-S05, W04-S06, W04-S10, W04-S11, W04-S12, W04-S13, W04-S14, W04-S15, W04-S19, W04-S20, W04-S21, W04-S22, W04-S23, W04-S24 | Lost response, one open interval, append-only manual revision and independent decision |
| Kafka outbox/inbox | W05-S01, W05-S02, W05-S03, W05-S04, W05-S05, W05-S06, W05-S07, W05-S08, W05-S09, W05-S10, W05-S11, W05-S12, W05-S13, W05-S14, W05-S15, W05-S16, W05-S17, W05-S18 | Publisher/DB/offset crash, duplicates, stale revision, lease and retention bootstrap |
| Leave balance and approval | W06-S01, W06-S02, W06-S03, W06-S04, W06-S05, W06-S06, W06-S10, W06-S11, W06-S12, W06-S13, W06-S14, W06-S15, W06-S19, W06-S20, W06-S21, W06-S22, W06-S23, W06-S24 | Exact arithmetic, reservation locks, cancellation conservation and manual-work conflict |
| Request approvals and fulfillment | W07-S01, W07-S02, W07-S03, W07-S04, W07-S05, W07-S06, W07-S10, W07-S11, W07-S12, W07-S13, W07-S14, W07-S15, W07-S16, W07-S17, W07-S18, W07-S19, W07-S20, W07-S21 | Assigned step, independent actor, reject/cancel/fulfill are separate commands |
| Report export | W09-S01, W09-S02, W09-S03, W09-S04, W09-S05, W09-S06, W09-S10, W09-S11, W09-S12, W09-S13, W09-S14, W09-S15, W09-S16, W09-S17, W09-S18, W09-S19, W09-S20, W09-S21 | Versioned projection, freshness, private atomic CSV, escaping and authorization |
| Backup/restore and deployment | W10-S01, W10-S02, W10-S03, W10-S04, W10-S05, W10-S06, W10-S16, W10-S17, W10-S18, W10-S19, W10-S20, W10-S21, W10-S28, W10-S29, W10-S30, W10-S31, W10-S32, W10-S33, W10-S34, W10-S35, W10-S36 | Explicit target, compatible rollback, clean recovery, outages and replay boundary |

The authentication ladder is in Week 2, with separate profile, browser-CSRF and mobile/Nuxt transport atoms. A discovery that needs more than three files or 90 minutes becomes a capped spike and an explicitly mapped smaller step; it is never hidden behind “repeat the golden slice.” Actual scope/time and gate failures can change the completion date.
