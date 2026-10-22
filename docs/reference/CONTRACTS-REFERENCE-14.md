# Contracts reference 14

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### attendance.idempotencyrecord

Owner: `attendance` service schema. Purpose: Committed safe HTTP result; do not persist login/refresh token responses.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."idempotency_record" (
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
CREATE INDEX "attendance_idempotency_record_q1" ON "attendance"."idempotency_record" (expires_at);
```


### leave.auditlog

Owner: `leave` service schema. Purpose: Owner-local append-only audit; opaque actor IDs are not cross-service FK.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."audit_log" (
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
CREATE INDEX "leave_audit_log_q1" ON "leave"."audit_log" (tenant_id,at,id);
```


### leave.outbox

Owner: `leave` service schema. Purpose: Durable ordered publication intent, lease claimed in short transaction.

Retention: TRANSIT; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."outbox" (
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
CREATE INDEX "leave_outbox_q1" ON "leave"."outbox" (next_attempt_at,created_at) WHERE state IN
  ('PENDING','CLAIMED');
CREATE INDEX "leave_outbox_q2" ON "leave"."outbox" (tenant_id,topic,aggregate_id,aggregate_version)
  WHERE state <> 'PUBLISHED';
```


### leave.inbox

Owner: `leave` service schema. Purpose: Deduplication record committed with local effect, before Kafka offset acknowledgment.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."inbox" (
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


### leave.idempotencyrecord

Owner: `leave` service schema. Purpose: Committed safe HTTP result; do not persist login/refresh token responses.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."idempotency_record" (
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
CREATE INDEX "leave_idempotency_record_q1" ON "leave"."idempotency_record" (expires_at);
```


### request.auditlog

Owner: `request` service schema. Purpose: Owner-local append-only audit; opaque actor IDs are not cross-service FK.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."audit_log" (
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
CREATE INDEX "request_audit_log_q1" ON "request"."audit_log" (tenant_id,at,id);
```


### request.outbox

Owner: `request` service schema. Purpose: Durable ordered publication intent, lease claimed in short transaction.

Retention: TRANSIT; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."outbox" (
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
CREATE INDEX "request_outbox_q1" ON "request"."outbox" (next_attempt_at,created_at) WHERE state IN
  ('PENDING','CLAIMED');
CREATE INDEX "request_outbox_q2" ON "request"."outbox" (tenant_id,topic,aggregate_id,
  aggregate_version) WHERE state <> 'PUBLISHED';
```


### request.inbox

Owner: `request` service schema. Purpose: Deduplication record committed with local effect, before Kafka offset acknowledgment.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."inbox" (
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


### request.idempotencyrecord

Owner: `request` service schema. Purpose: Committed safe HTTP result; do not persist login/refresh token responses.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."idempotency_record" (
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
CREATE INDEX "request_idempotency_record_q1" ON "request"."idempotency_record" (expires_at);
```


### notification.auditlog

Owner: `notification` service schema. Purpose: Owner-local append-only audit; opaque actor IDs are not cross-service FK.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."audit_log" (
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
CREATE INDEX "notification_audit_log_q1" ON "notification"."audit_log" (tenant_id,at,id);
```


### notification.outbox

Owner: `notification` service schema. Purpose: Durable ordered publication intent, lease claimed in short transaction.

Retention: TRANSIT; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."outbox" (
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
CREATE INDEX "notification_outbox_q1" ON "notification"."outbox" (next_attempt_at,created_at) WHERE
  state IN ('PENDING','CLAIMED');
CREATE INDEX "notification_outbox_q2" ON "notification"."outbox" (tenant_id,topic,aggregate_id,
  aggregate_version) WHERE state <> 'PUBLISHED';
```


### notification.inbox

Owner: `notification` service schema. Purpose: Deduplication record committed with local effect, before Kafka offset acknowledgment.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."inbox" (
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


### notification.idempotencyrecord

Owner: `notification` service schema. Purpose: Committed safe HTTP result; do not persist login/refresh token responses.

Retention: TRANSIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."idempotency_record" (
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
CREATE INDEX "notification_idempotency_record_q1" ON "notification"."idempotency_record"
  (expires_at);
```
