# Contracts reference 11

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### attendance.reminder

Owner: `attendance` service schema. Purpose: Deduplication for +5/+15 minute reminders.

Retention: DELIVERY; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."reminder" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  work_date date NOT NULL,
  kind text NOT NULL CHECK (kind IN ('CHECK_IN','CHECK_OUT')),
  level integer NOT NULL CHECK (level IN (5,15)),
  sent_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "attendance"."employee_ref" (tenant_id,
  employee_id),
  UNIQUE (tenant_id,employee_id,work_date,kind,level)
);
```


### attendance.cutoffjob

Owner: `attendance` service schema. Purpose: Durable cutoff scheduling and dedupe; proposed support for existing cutoff requirement.

Retention: TRANSIT; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."cutoff_job" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  work_date date NOT NULL,
  shift_revision bigint NOT NULL,
  due_at timestamptz NOT NULL,
  status text NOT NULL CHECK (status IN ('PENDING','EMITTED','APPLIED','FAILED')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  UNIQUE (tenant_id,employee_id,work_date,shift_revision)
);
CREATE INDEX "attendance_cutoff_job_q1" ON "attendance"."cutoff_job" (due_at) WHERE status =
  'PENDING';
```


### leave.leavetype

Owner: `leave` service schema. Purpose: Configured leave categories and validation flags.

Retention: MASTER; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."leave_type" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  code text NOT NULL CHECK (code IN ('ANNUAL','SICK','SPECIAL','UNPAID')),
  name text NOT NULL,
  paid boolean NOT NULL,
  requires_attachment boolean NOT NULL,
  max_days_per_request numeric(6,1) NOT NULL CHECK (max_days_per_request > 0),
  allow_half_day boolean NOT NULL,
  deleted_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  UNIQUE (tenant_id,code)
);
```


### leave.leavepolicy

Owner: `leave` service schema. Purpose: Current versioned allocation policy; request stores snapshot.

Retention: MASTER; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."leave_policy" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  leave_type_id uuid NOT NULL,
  annual_days numeric(6,1) NOT NULL CHECK (annual_days >= 0),
  accrual text NOT NULL CHECK (accrual IN ('UPFRONT','MONTHLY')),
  carry_over_max numeric(6,1) NOT NULL CHECK (carry_over_max >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,leave_type_id) REFERENCES "leave"."leave_type" (tenant_id,id),
  UNIQUE (tenant_id,leave_type_id)
);
```


### leave.leavebalance

Owner: `leave` service schema. Purpose: Lockable materialized balance; ledger is reconciliation source.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."leave_balance" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  leave_type_id uuid NOT NULL,
  year integer NOT NULL CHECK (year BETWEEN 2000 AND 2200),
  entitled numeric(6,1) NOT NULL CHECK (entitled >= 0),
  carried_over numeric(6,1) NOT NULL CHECK (carried_over >= 0),
  used numeric(6,1) NOT NULL CHECK (used >= 0),
  pending numeric(6,1) NOT NULL CHECK (pending >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id,leave_type_id,year),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "leave"."employee_ref" (tenant_id,employee_id),
  FOREIGN KEY (tenant_id,leave_type_id) REFERENCES "leave"."leave_type" (tenant_id,id),
  CHECK (entitled + carried_over - used - pending >= 0)
);
```


### leave.leaveattachment

Owner: `leave` service schema. Purpose: Private supporting evidence upload; proposed table closing attachment workflow gap.

Retention: SENSITIVE; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."leave_attachment" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
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
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "leave"."employee_ref" (tenant_id,employee_id),
  UNIQUE (tenant_id,object_key)
);
```


### leave.leaverequest

Owner: `leave` service schema. Purpose: Authoritative request and approval state, not attendance acceptance.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."leave_request" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  leave_type_id uuid NOT NULL,
  start_date date NOT NULL,
  end_date date NOT NULL,
  half_day text NOT NULL CHECK (half_day IN ('NONE','AM','PM')),
  days numeric(6,1) NOT NULL CHECK (days > 0),
  reason text NOT NULL,
  attachment_id uuid,
  policy_snapshot jsonb NOT NULL,
  status text NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED',
  'NEEDS_REVIEW')),
  approver_id uuid,
  decided_at timestamptz,
  decision_note text,
  attendance_applied_version bigint,
  conflict_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "leave"."employee_ref" (tenant_id,employee_id),
  FOREIGN KEY (tenant_id,leave_type_id) REFERENCES "leave"."leave_type" (tenant_id,id),
  FOREIGN KEY (tenant_id,attachment_id) REFERENCES "leave"."leave_attachment" (tenant_id,id),
  CHECK (end_date >= start_date),
  CHECK (half_day = 'NONE' OR start_date = end_date),
  CHECK (approver_id IS NULL OR approver_id <> employee_id)
);
CREATE INDEX "leave_leave_request_q1" ON "leave"."leave_request" (tenant_id,employee_id,start_date,
  end_date);
CREATE INDEX "leave_leave_request_q2" ON "leave"."leave_request" (tenant_id,approver_id,status,
  created_at,id);
```


### leave.leaverequestday

Owner: `leave` service schema. Purpose: Exact chargeable day/portion expanded at submission; historical snapshot.

Retention: BUSINESS; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."leave_request_day" (
  tenant_id uuid NOT NULL,
  request_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  date date NOT NULL,
  portion text NOT NULL CHECK (portion IN ('FULL','AM','PM')),
  units numeric(2,1) NOT NULL CHECK (units IN (0.5,1.0)),
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,request_id,date,portion),
  FOREIGN KEY (tenant_id,request_id) REFERENCES "leave"."leave_request" (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "leave"."employee_ref" (tenant_id,employee_id)
);
CREATE INDEX "leave_leave_request_day_q1" ON "leave"."leave_request_day" (tenant_id,employee_id,
  date);
```


### leave.balanceledger

Owner: `leave` service schema. Purpose: Append-only deltas by component; one transaction per command/period key.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."balance_ledger" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  leave_type_id uuid NOT NULL,
  year integer NOT NULL,
  request_id uuid,
  operation_key text NOT NULL,
  component text NOT NULL CHECK (component IN ('ENTITLED','CARRIED_OVER','USED','PENDING')),
  delta numeric(6,1) NOT NULL CHECK (delta <> 0),
  reason text NOT NULL,
  actor_id uuid,
  at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "leave"."employee_ref" (tenant_id,employee_id),
  FOREIGN KEY (tenant_id,leave_type_id) REFERENCES "leave"."leave_type" (tenant_id,id),
  FOREIGN KEY (tenant_id,request_id) REFERENCES "leave"."leave_request" (tenant_id,id),
  UNIQUE (tenant_id,operation_key,component)
);
CREATE INDEX "leave_balance_ledger_q1" ON "leave"."balance_ledger" (tenant_id,employee_id,
  leave_type_id,year,at,id);
```


### leave.yearendjob

Owner: `leave` service schema. Purpose: Observable tenant batch operation with checkpoint and safe repeat semantics.

Retention: DELIVERY; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."year_end_job" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  requested_by uuid NOT NULL,
  from_year integer NOT NULL,
  to_year integer NOT NULL,
  policy_revision bigint NOT NULL,
  status text NOT NULL CHECK (status IN ('QUEUED','RUNNING','READY','FAILED')),
  processed integer NOT NULL DEFAULT 0,
  cursor jsonb,
  error_code text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  CHECK (to_year = from_year + 1),
  UNIQUE (tenant_id,from_year,to_year,policy_revision)
);
```


### request.employeeref

Owner: `request` service schema. Purpose: Local full employee snapshot for authorization and eligibility; no cross-schema lookup.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."employee_ref" (
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
CREATE INDEX "request_employee_ref_q1" ON "request"."employee_ref" (tenant_id,manager_id,
  employee_id);
```


### request.requesttype

Owner: `request` service schema. Purpose: Bounded form schema and chain template, snapshotted when submitted.

Retention: MASTER; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."request_type" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  code text NOT NULL CHECK (code IN ('MATERIAL','CERTIFICATE','OVERTIME','OTHER')),
  name text NOT NULL,
  form_schema jsonb NOT NULL,
  approval_chain jsonb NOT NULL,
  deleted_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  UNIQUE (tenant_id,code)
);
```


### request.materialitem

Owner: `request` service schema. Purpose: Advisory stock catalogue, no authoritative inventory reservation.

Retention: MASTER; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "request"."material_item" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  sku text NOT NULL,
  name text NOT NULL,
  unit text NOT NULL,
  stock_qty numeric(12,3) NOT NULL CHECK (stock_qty >= 0),
  active boolean NOT NULL DEFAULT true,
  deleted_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  UNIQUE (tenant_id,sku)
);
```
