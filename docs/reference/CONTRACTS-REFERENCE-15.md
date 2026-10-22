# Contracts reference 15

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### reporting.auditlog

Owner: `reporting` service schema. Purpose: Owner-local append-only audit; opaque actor IDs are not cross-service FK.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."audit_log" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  actor_id uuid,
  action text NOT NULL,
  entity text NOT NULL,
  entity_id uuid,
  diff jsonb NOT NULL,
  at timestamptz NOT NULL,
  correlation_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,id)
);
CREATE INDEX "reporting_audit_log_q1" ON "reporting"."audit_log" (tenant_id,at,id);
```


### reporting.inbox

Owner: `reporting` service schema. Purpose: Deduplication record committed with local effect, before Kafka offset acknowledgment.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."inbox" (
  tenant_id uuid NOT NULL,
  consumer_name text NOT NULL,
  event_id uuid NOT NULL,
  topic text NOT NULL,
  partition integer NOT NULL,
  source_offset bigint NOT NULL,
  aggregate_id text NOT NULL,
  aggregate_version bigint NOT NULL,
  processed_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,consumer_name,event_id)
);
```


### reporting.idempotencyrecord

Owner: `reporting` service schema. Purpose: Committed safe HTTP result; do not persist login/refresh token responses.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."idempotency_record" (
  tenant_id uuid NOT NULL,
  command_scope text NOT NULL,
  idempotency_key uuid NOT NULL,
  request_hash char(64) NOT NULL,
  response_status integer NOT NULL CHECK (response_status BETWEEN 200 AND 299),
  response_body jsonb,
  resource_id uuid,
  location text,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,command_scope,idempotency_key)
);
CREATE INDEX "reporting_idempotency_record_q1" ON "reporting"."idempotency_record" (expires_at);
```


### 4.4 Integrity, concurrency, and migration review

The DDL enforces tenant-scoped uniqueness, same-service composite FKs, open-session uniqueness, nonnegative balances, and inbox IDs. It intentionally does not claim every domain invariant is expressible with a row check:

- Employee_shift date-range overlap needs employee-row serialization or a reviewed exclusion constraint migration; the unique start date is insufficient alone.
- Closed session overlap needs employee/day guard plus interval validation under lock.
- Leave FULL versus AM/PM overlap needs employee guard and active-request/day lookup; historical day rows remain after cancellation.
- Request approver != requester spans two rows and must be checked inside the request lock transaction.
- Tenant/employee consistency of leave_request_day is enforced by the additional composite parent constraint in section 4.5.
- Refresh family rotation and token reuse detection need a family/user lock and retained revocation history.
- Device token reassignment must atomically deactivate prior ownership; do not let nullable uniqueness or provider duplicates leak notifications.
- Delivery target_id is made non-null in section 4.5; email/in-app uses stable logical recipient IDs.
- Notification preference-set ETag uses preference_set (section 4.5); lock it while replacing category rows.
- Batch read-all captures one cutoff in the idempotency transaction.

All services use bounded row locks, deterministic lock ordering, and retry-safe commands. No balance mutation is a read-modify-write without a lock/version predicate. Current session checkout may update the session projection, but evidence/correction_action/audit/ledger remain append-only. Apply initial schemas/tables through owner Flyway migrations; this Markdown DDL is not already installed. Before launch, test migration order, runtime grants, query plans, upgrade compatibility, rollback, retention cleanup, and tenant-predicate enforcement. Do not partition evidence until volume warrants it and uniqueness across partitions is deliberately redesigned. Projection rebuilds run in isolated tables/consumer groups, with external side effects disabled.

### 4.5 Supporting aggregate guards and integrity extensions

These proposed tables/constraints complete the aggregate behaviors that cannot rely on individual row versions alone.

### notification.preferenceset

Owner: notification. Purpose: one lock/version for the entire own preference resource; tenant-scoped employee FK. Retention MASTER; audited mutable state. GET/PUT ETag uses this version, not per-category row versions. No extra index beyond PK is needed.

```sql
CREATE TABLE notification.preference_set (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  locale text NOT NULL CHECK (locale IN ('en','km')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES notification.employee_ref(tenant_id,employee_id)
);
ALTER TABLE notification.notification_pref DROP COLUMN locale;
ALTER TABLE notification.delivery_attempt ALTER COLUMN target_id SET NOT NULL;
ALTER TABLE leave.leave_request ADD CONSTRAINT leave_request_employee_identity UNIQUE (tenant_id,id,
  employee_id);
ALTER TABLE leave.leave_request_day ADD CONSTRAINT leave_day_parent_employee_fk
  FOREIGN KEY (tenant_id,request_id,employee_id) REFERENCES leave.leave_request(tenant_id,id,
  employee_id);
ALTER TABLE attendance.cutoff_job ADD COLUMN kind text NOT NULL CHECK (kind IN ('CHECK_IN',
  'CHECK_OUT'));
ALTER TABLE attendance.cutoff_job DROP CONSTRAINT
  cutoff_job_tenant_id_employee_id_work_date_shift_revision_key;
ALTER TABLE attendance.cutoff_job ADD CONSTRAINT cutoff_business_key UNIQUE (tenant_id,employee_id,
  work_date,shift_revision,kind);
```


Cutoff kind distinguishes initial absence assessment from missing-checkout finalization on the same shift revision. Delivery target_id is a stable non-null logical recipient/device target, even for email or in-app channel; this makes attempt uniqueness effective. Leave day employee identity is now enforced by a composite parent FK as well as owner transaction checks. Identity tables without a transitive tenant reference also reference the root directly:

```sql
ALTER TABLE identity_org.department ADD FOREIGN KEY (tenant_id) REFERENCES identity_org.tenant(id);
ALTER TABLE identity_org.position ADD FOREIGN KEY (tenant_id) REFERENCES identity_org.tenant(id);
ALTER TABLE identity_org.work_site ADD FOREIGN KEY (tenant_id) REFERENCES identity_org.tenant(id);
ALTER TABLE identity_org.holiday ADD FOREIGN KEY (tenant_id) REFERENCES identity_org.tenant(id);
```


Runtime grants must restrict inserts into all owner schemas to authorized tenant context. Root FKs are integrity, not authorization. Each supporting table follows the common migration/version/retention rules; these are documentation examples, not applied repository migrations.
