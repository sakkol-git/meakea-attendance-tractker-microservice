# Contracts reference 12

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### request.request

Owner: `request` service schema. Purpose: General request aggregate and current approval position.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."request" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  requester_id uuid NOT NULL,
  type_id uuid NOT NULL,
  title text NOT NULL,
  description text,
  data jsonb NOT NULL,
  type_snapshot jsonb NOT NULL,
  status text NOT NULL CHECK (status IN ('DRAFT','PENDING','APPROVED','REJECTED','CANCELLED',
  'FULFILLED')),
  current_step integer CHECK (current_step > 0),
  submitted_at timestamptz,
  decided_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,requester_id) REFERENCES "request"."employee_ref" (tenant_id,employee_id),
  FOREIGN KEY (tenant_id,type_id) REFERENCES "request"."request_type" (tenant_id,id)
);
CREATE INDEX "request_request_q1" ON "request"."request" (tenant_id,requester_id,created_at,id);
CREATE INDEX "request_request_q2" ON "request"."request" (tenant_id,status,submitted_at,id);
```


### request.requestline

Owner: `request` service schema. Purpose: Material quantities; editing only on draft.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."request_line" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  request_id uuid NOT NULL,
  item_id uuid NOT NULL,
  qty numeric(12,3) NOT NULL CHECK (qty > 0),
  note text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,request_id) REFERENCES "request"."request" (tenant_id,id),
  FOREIGN KEY (tenant_id,item_id) REFERENCES "request"."material_item" (tenant_id,id),
  UNIQUE (tenant_id,request_id,item_id)
);
```


### request.approvalstep

Owner: `request` service schema. Purpose: Atomic current step decision; role is a requirement, approver_id is resolved authority.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."approval_step" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  request_id uuid NOT NULL,
  step_no integer NOT NULL CHECK (step_no > 0),
  approver_role text NOT NULL CHECK (approver_role IN ('MANAGER','HR_ADMIN')),
  approver_id uuid NOT NULL,
  status text NOT NULL CHECK (status IN ('WAITING','APPROVED','REJECTED','SKIPPED')),
  decided_at timestamptz,
  note text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,request_id) REFERENCES "request"."request" (tenant_id,id),
  FOREIGN KEY (tenant_id,approver_id) REFERENCES "request"."employee_ref" (tenant_id,employee_id),
  UNIQUE (tenant_id,request_id,step_no)
);
CREATE INDEX "request_approval_step_q1" ON "request"."approval_step" (tenant_id,approver_id,status,
  request_id);
```


### request.requestattachment

Owner: `request` service schema. Purpose: Private upload metadata and scan state; no client-selected key.

Retention: SENSITIVE; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."request_attachment" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  request_id uuid NOT NULL,
  object_key text NOT NULL,
  file_name text NOT NULL,
  content_type text NOT NULL,
  size bigint NOT NULL CHECK (size BETWEEN 1 AND 10485760),
  sha256 char(64) NOT NULL,
  status text NOT NULL CHECK (status IN ('PENDING','QUARANTINED','READY','REJECTED')),
  expires_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,request_id) REFERENCES "request"."request" (tenant_id,id),
  UNIQUE (tenant_id,object_key)
);
```


### request.requestcomment

Owner: `request` service schema. Purpose: Immutable participant conversation.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."request_comment" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  request_id uuid NOT NULL,
  author_id uuid NOT NULL,
  body text NOT NULL CHECK (length(body) BETWEEN 1 AND 2000),
  at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,request_id) REFERENCES "request"."request" (tenant_id,id)
);
CREATE INDEX "request_request_comment_q1" ON "request"."request_comment" (tenant_id,request_id,at,
  id);
```


### notification.employeeref

Owner: `notification` service schema. Purpose: Local full employee snapshot for authorization and eligibility; no cross-schema lookup.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."employee_ref" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  manager_id uuid,
  department_id uuid,
  full_name text NOT NULL,
  status text NOT NULL CHECK (status IN ('ACTIVE','ON_LEAVE','TERMINATED')),
  hire_date date NOT NULL,
  terminated_on date,
  device_id text,
  source_version bigint NOT NULL CHECK (source_version > 0),
  source_event_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id)
);
CREATE INDEX "notification_employee_ref_q1" ON "notification"."employee_ref" (tenant_id,manager_id,
  employee_id);
```


### notification.devicetoken

Owner: `notification` service schema. Purpose: Encrypted provider token with tenant-owned installation identity.

Retention: SENSITIVE; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."device_token" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  installation_id text NOT NULL,
  platform text NOT NULL CHECK (platform IN ('ANDROID','IOS','WEB')),
  token_ciphertext bytea NOT NULL,
  token_hash text NOT NULL,
  last_seen_at timestamptz NOT NULL,
  active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "notification"."employee_ref" (tenant_id,
  employee_id),
  UNIQUE (tenant_id,employee_id,installation_id),
  UNIQUE (tenant_id,token_hash)
);
```


### notification.notificationtemplate

Owner: `notification` service schema. Purpose: Tenant/channel/locale template revision.

Retention: MASTER; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."notification_template" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  code text NOT NULL,
  channel text NOT NULL CHECK (channel IN ('PUSH','EMAIL','IN_APP')),
  locale text NOT NULL CHECK (locale IN ('en','km')),
  subject text,
  body text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  UNIQUE (tenant_id,code,channel,locale)
);
```


### notification.notificationpref

Owner: `notification` service schema. Purpose: Employee category/channel preference; locale stored per preference set consistently.

Retention: MASTER; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."notification_pref" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  category text NOT NULL,
  push boolean NOT NULL,
  email boolean NOT NULL,
  in_app boolean NOT NULL,
  locale text NOT NULL CHECK (locale IN ('en','km')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id,category),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "notification"."employee_ref" (tenant_id,
  employee_id)
);
```


### notification.notification

Owner: `notification` service schema. Purpose: Durable own inbox deduped independently of provider attempts.

Retention: DELIVERY; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."notification" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  source_event_id uuid NOT NULL,
  category text NOT NULL,
  title text NOT NULL,
  body text NOT NULL,
  data jsonb NOT NULL,
  read_at timestamptz,
  sequence bigint GENERATED ALWAYS AS IDENTITY,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "notification"."employee_ref" (tenant_id,
  employee_id),
  UNIQUE (tenant_id,source_event_id,employee_id,category),
  UNIQUE (tenant_id,employee_id,sequence)
);
CREATE INDEX "notification_notification_q1" ON "notification"."notification" (tenant_id,employee_id,
  sequence);
CREATE INDEX "notification_notification_q2" ON "notification"."notification" (tenant_id,employee_id,
  created_at,id) WHERE read_at IS NULL;
```


### notification.deliveryattempt

Owner: `notification` service schema. Purpose: Durable attempt lease/state; provider accepted is not device delivered.

Retention: DELIVERY; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "notification"."delivery_attempt" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  notification_id uuid NOT NULL,
  channel text NOT NULL CHECK (channel IN ('PUSH','EMAIL','IN_APP')),
  target_id uuid,
  attempt_no integer NOT NULL CHECK (attempt_no > 0),
  status text NOT NULL CHECK (status IN ('PENDING','PROVIDER_ACCEPTED','FAILED','UNKNOWN')),
  provider_ref text,
  error_code text,
  next_attempt_at timestamptz,
  lease_token uuid,
  lease_until timestamptz,
  at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,notification_id) REFERENCES "notification"."notification" (tenant_id,id),
  UNIQUE (tenant_id,notification_id,channel,target_id,attempt_no)
);
CREATE INDEX "notification_delivery_attempt_q1" ON "notification"."delivery_attempt"
  (next_attempt_at) WHERE status IN ('PENDING','UNKNOWN');
```


### reporting.employeedim

Owner: `reporting` service schema. Purpose: Authorized reporting dimension from employee snapshots; historical effective context preserved in fact snapshot.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."employee_dim" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  name text NOT NULL,
  department_id uuid,
  department_name text,
  position text,
  manager_id uuid,
  status text NOT NULL,
  source_version bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id)
);
CREATE INDEX "reporting_employee_dim_q1" ON "reporting"."employee_dim" (tenant_id,manager_id,
  employee_id);
```


### reporting.attendancefact

Owner: `reporting` service schema. Purpose: One current attendance day revision; no transactional attendance joins.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "reporting"."attendance_fact" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  work_date date NOT NULL,
  department_id uuid,
  status text NOT NULL,
  first_in timestamptz,
  last_out timestamptz,
  late_minutes integer NOT NULL CHECK (late_minutes >= 0),
  worked_minutes integer CHECK (worked_minutes >= 0),
  source text NOT NULL,
  source_version bigint NOT NULL,
  source_event_id uuid NOT NULL,
  source_committed_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id,work_date)
);
CREATE INDEX "reporting_attendance_fact_q1" ON "reporting"."attendance_fact" (tenant_id,work_date,
  department_id,status);
```
