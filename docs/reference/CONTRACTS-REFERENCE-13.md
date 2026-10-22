# Contracts reference 13

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### reporting.leavefact

Owner: `reporting` service schema. Purpose: Full current leave request snapshot.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."leave_fact" (
  tenant_id uuid NOT NULL,
  request_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  department_id uuid,
  leave_type_id uuid NOT NULL,
  start_date date NOT NULL,
  end_date date NOT NULL,
  days numeric(6,1) NOT NULL,
  status text NOT NULL,
  decided_at timestamptz,
  source_version bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,request_id)
);
CREATE INDEX "reporting_leave_fact_q1" ON "reporting"."leave_fact" (tenant_id,start_date,end_date,
  department_id);
```


### reporting.leavebalancefact

Owner: `reporting` service schema. Purpose: Proposed projection required by preserved balance report API.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."leave_balance_fact" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  leave_type_id uuid NOT NULL,
  year integer NOT NULL,
  entitled numeric(6,1) NOT NULL,
  used numeric(6,1) NOT NULL,
  pending numeric(6,1) NOT NULL,
  carried_over numeric(6,1) NOT NULL,
  source_version bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id,leave_type_id,year)
);
```


### reporting.requestfact

Owner: `reporting` service schema. Purpose: Current request state and cycle duration from workflow snapshots.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."request_fact" (
  tenant_id uuid NOT NULL,
  request_id uuid NOT NULL,
  type_id uuid NOT NULL,
  requester_id uuid NOT NULL,
  department_id uuid,
  status text NOT NULL,
  submitted_at timestamptz,
  decided_at timestamptz,
  cycle_hours numeric(12,3),
  source_version bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,request_id)
);
CREATE INDEX "reporting_request_fact_q1" ON "reporting"."request_fact" (tenant_id,submitted_at,
  type_id,status);
```


### reporting.dailysummary

Owner: `reporting` service schema. Purpose: Derived count bucket; null department represented by explicit bucket key, avoiding nullable PK.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."daily_summary" (
  tenant_id uuid NOT NULL,
  work_date date NOT NULL,
  department_key text NOT NULL,
  present integer NOT NULL CHECK (present >= 0),
  late integer NOT NULL CHECK (late >= 0),
  absent integer NOT NULL CHECK (absent >= 0),
  on_leave integer NOT NULL CHECK (on_leave >= 0),
  leave_early integer NOT NULL CHECK (leave_early >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,work_date,department_key)
);
```


### reporting.exportjob

Owner: `reporting` service schema. Purpose: Observable export job and authorized storage artifact.

Retention: DELIVERY; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."export_job" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  requested_by uuid NOT NULL,
  kind text NOT NULL,
  format text NOT NULL CHECK (format IN ('CSV','XLSX')),
  params jsonb NOT NULL,
  scope_snapshot jsonb NOT NULL,
  status text NOT NULL CHECK (status IN ('QUEUED','RUNNING','READY','FAILED','EXPIRED')),
  object_key text,
  error_code text,
  expires_at timestamptz,
  lease_token uuid,
  lease_until timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id)
);
CREATE INDEX "reporting_export_job_q1" ON "reporting"."export_job" (tenant_id,requested_by,
  created_at,id);
CREATE INDEX "reporting_export_job_q2" ON "reporting"."export_job" (created_at) WHERE status =
  'QUEUED';
```


### reporting.projectionwatermark

Owner: `reporting` service schema. Purpose: Per input partition checkpoint and freshness; no fictitious global Kafka ordering.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."projection_watermark" (
  tenant_id uuid NOT NULL,
  consumer_name text NOT NULL,
  topic text NOT NULL,
  partition integer NOT NULL,
  last_offset bigint NOT NULL,
  source_time timestamptz NOT NULL,
  applied_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,consumer_name,topic,partition)
);
```


### 4.3 Shared infrastructure tables

Instantiate the following owner-local tables in every domain schema. Reporting needs inbox/idempotency/audit for projections/export commands; it does not need a business-event outbox unless it later publishes facts. Notification retains an outbox option for durable internal delivery scheduling; no `notification.sent.v1` dependency is introduced. Shared definitions are generated below for all six schemas to make tenant constraints explicit. `aggregate_key` is the Kafka key; aggregate revision ordering is guarded by relay eligibility, not merely SKIP LOCKED. `consumer_name` distinguishes independently applied projections; the same event may legitimately reach two handlers. `command_scope` contains actor + method + canonical route; credential-command fingerprints use keyed HMAC, and stored responses never contain secrets.

### identityorg.auditlog

Owner: `identity_org` service schema. Purpose: Owner-local append-only audit; opaque actor IDs are not cross-service FK.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "identity_org"."audit_log" (
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
CREATE INDEX "identity_org_audit_log_q1" ON "identity_org"."audit_log" (tenant_id,at,id);
```


### identityorg.outbox

Owner: `identity_org` service schema. Purpose: Durable ordered publication intent, lease claimed in short transaction.

Retention: TRANSIT; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "identity_org"."outbox" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  event_id uuid NOT NULL,
  topic text NOT NULL,
  event_type text NOT NULL,
  aggregate_id text NOT NULL,
  aggregate_key text NOT NULL,
  aggregate_version bigint NOT NULL CHECK (aggregate_version > 0),
  schema_version integer NOT NULL CHECK (schema_version > 0),
  payload jsonb NOT NULL,
  occurred_at timestamptz NOT NULL,
  correlation_id uuid NOT NULL,
  causation_id uuid,
  state text NOT NULL CHECK (state IN ('PENDING','CLAIMED','PUBLISHED','QUARANTINED')),
  next_attempt_at timestamptz NOT NULL DEFAULT now(),
  attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  lease_token uuid,
  lease_until timestamptz,
  published_at timestamptz,
  last_error_code text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  UNIQUE (tenant_id,event_id),
  UNIQUE (tenant_id,topic,aggregate_id,aggregate_version,event_type)
);
CREATE INDEX "identity_org_outbox_q1" ON "identity_org"."outbox" (next_attempt_at,created_at) WHERE
  state IN ('PENDING','CLAIMED');
CREATE INDEX "identity_org_outbox_q2" ON "identity_org"."outbox" (tenant_id,topic,aggregate_id,
  aggregate_version) WHERE state <> 'PUBLISHED';
```


### identityorg.inbox

Owner: `identity_org` service schema. Purpose: Deduplication record committed with local effect, before Kafka offset acknowledgment.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "identity_org"."inbox" (
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


### identityorg.idempotencyrecord

Owner: `identity_org` service schema. Purpose: Committed safe HTTP result; do not persist login/refresh token responses.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "identity_org"."idempotency_record" (
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
CREATE INDEX "identity_org_idempotency_record_q1" ON "identity_org"."idempotency_record"
  (expires_at);
```


### attendance.auditlog

Owner: `attendance` service schema. Purpose: Owner-local append-only audit; opaque actor IDs are not cross-service FK.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."audit_log" (
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
CREATE INDEX "attendance_audit_log_q1" ON "attendance"."audit_log" (tenant_id,at,id);
```


### attendance.outbox

Owner: `attendance` service schema. Purpose: Durable ordered publication intent, lease claimed in short transaction.

Retention: TRANSIT; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."outbox" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  event_id uuid NOT NULL,
  topic text NOT NULL,
  event_type text NOT NULL,
  aggregate_id text NOT NULL,
  aggregate_key text NOT NULL,
  aggregate_version bigint NOT NULL CHECK (aggregate_version > 0),
  schema_version integer NOT NULL CHECK (schema_version > 0),
  payload jsonb NOT NULL,
  occurred_at timestamptz NOT NULL,
  correlation_id uuid NOT NULL,
  causation_id uuid,
  state text NOT NULL CHECK (state IN ('PENDING','CLAIMED','PUBLISHED','QUARANTINED')),
  next_attempt_at timestamptz NOT NULL DEFAULT now(),
  attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  lease_token uuid,
  lease_until timestamptz,
  published_at timestamptz,
  last_error_code text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  UNIQUE (tenant_id,event_id),
  UNIQUE (tenant_id,topic,aggregate_id,aggregate_version,event_type)
);
CREATE INDEX "attendance_outbox_q1" ON "attendance"."outbox" (next_attempt_at,created_at) WHERE
  state IN ('PENDING','CLAIMED');
CREATE INDEX "attendance_outbox_q2" ON "attendance"."outbox" (tenant_id,topic,aggregate_id,
  aggregate_version) WHERE state <> 'PUBLISHED';
```


### attendance.inbox

Owner: `attendance` service schema. Purpose: Deduplication record committed with local effect, before Kafka offset acknowledgment.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."inbox" (
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
