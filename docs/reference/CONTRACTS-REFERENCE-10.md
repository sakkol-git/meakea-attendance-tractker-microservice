# Contracts reference 10

> **Warning:** Superseded historical design; use the current parent specification and roadmap. No feature is implemented by this reference.

## Continued reference

### attendance.shiftref

Owner: `attendance` service schema. Purpose: Full shift snapshot including site evidence context.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."shift_ref" (
  tenant_id uuid NOT NULL,
  shift_id uuid NOT NULL,
  start_time time NOT NULL,
  end_time time NOT NULL,
  timezone text NOT NULL,
  grace_minutes integer NOT NULL CHECK (grace_minutes >= 0),
  working_days smallint NOT NULL,
  site jsonb NOT NULL,
  source_version bigint NOT NULL,
  source_event_id uuid NOT NULL,
  deleted_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,shift_id)
);
```


### attendance.employeeshiftref

Owner: `attendance` service schema. Purpose: Assignment snapshot from org.employee; source version follows employee aggregate.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."employee_shift_ref" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  assignment_id uuid NOT NULL,
  shift_id uuid NOT NULL,
  valid_from date NOT NULL,
  valid_to date,
  source_version bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,assignment_id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "attendance"."employee_ref" (tenant_id,
  employee_id),
  CHECK (valid_to IS NULL OR valid_to >= valid_from)
);
CREATE INDEX "attendance_employee_shift_ref_q1" ON "attendance"."employee_shift_ref" (tenant_id,
  employee_id,valid_from,valid_to);
```


### attendance.holidayref

Owner: `attendance` service schema. Purpose: Holiday projection with stable owner ID and revision.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."holiday_ref" (
  tenant_id uuid NOT NULL,
  holiday_id uuid NOT NULL,
  date date NOT NULL,
  name text NOT NULL,
  source_version bigint NOT NULL,
  deleted_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,holiday_id),
  UNIQUE (tenant_id,date)
);
```


### leave.employeeref

Owner: `leave` service schema. Purpose: Local full employee snapshot for authorization and eligibility; no cross-schema lookup.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."employee_ref" (
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
CREATE INDEX "leave_employee_ref_q1" ON "leave"."employee_ref" (tenant_id,manager_id,employee_id);
```


### leave.holidayref

Owner: `leave` service schema. Purpose: Holiday projection with stable owner ID and revision.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "leave"."holiday_ref" (
  tenant_id uuid NOT NULL,
  holiday_id uuid NOT NULL,
  date date NOT NULL,
  name text NOT NULL,
  source_version bigint NOT NULL,
  deleted_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,holiday_id),
  UNIQUE (tenant_id,date)
);
```


### attendance.leaveref

Owner: `attendance` service schema. Purpose: Approved/needs-review per-request day overlays; cancellation updates only this request.

Retention: PROJECTION; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."leave_ref" (
  tenant_id uuid NOT NULL,
  request_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  work_date date NOT NULL,
  portion text NOT NULL CHECK (portion IN ('FULL','AM','PM')),
  leave_type_id uuid NOT NULL,
  status text NOT NULL CHECK (status IN ('APPROVED','NEEDS_REVIEW','CANCELLED')),
  source_version bigint NOT NULL,
  conflict boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,request_id,work_date,portion),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "attendance"."employee_ref" (tenant_id,employee_id)
);
CREATE INDEX "attendance_leave_ref_q1" ON "attendance"."leave_ref" (tenant_id,employee_id,
  work_date);
```


### attendance.employeedayguard

Owner: `attendance` service schema. Purpose: Lockable employee/day boundary for check-in, cutoff, and correction races.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."employee_day_guard" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  work_date date NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id,work_date),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "attendance"."employee_ref" (tenant_id,employee_id)
);
```


### attendance.evidence

Owner: `attendance` service schema. Purpose: Append-only observations; occurrence and reception are distinct.

Retention: SENSITIVE; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."evidence" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  client_event_id uuid NOT NULL,
  kind text NOT NULL CHECK (kind IN ('GEOFENCE_ENTER','GEOFENCE_EXIT','WIFI','BLE','TAP','MANUAL')),
  occurred_at timestamptz NOT NULL,
  received_at timestamptz NOT NULL DEFAULT now(),
  latitude numeric(9,6) CHECK (latitude BETWEEN -90 AND 90),
  longitude numeric(9,6) CHECK (longitude BETWEEN -180 AND 180),
  accuracy_m numeric CHECK (accuracy_m >= 0),
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  device_id text,
  provenance text NOT NULL,
  consent_revision text,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "attendance"."employee_ref" (tenant_id,
  employee_id),
  UNIQUE (tenant_id,employee_id,client_event_id)
);
CREATE INDEX "attendance_evidence_q1" ON "attendance"."evidence" (tenant_id,employee_id,occurred_at,
  id);
```


### attendance.attendancesession

Owner: `attendance` service schema. Purpose: Current interval revision; replacements append new row and supersede old row, never erase evidence.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."attendance_session" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  work_date date NOT NULL,
  check_in_at timestamptz NOT NULL,
  check_out_at timestamptz,
  source text NOT NULL CHECK (source IN ('TAP','NUDGE','INFERRED','MANUAL')),
  confidence numeric(5,4) CHECK (confidence BETWEEN 0 AND 1),
  reason text,
  model_version text,
  confirm_status text NOT NULL CHECK (confirm_status IN ('NONE','PENDING','CONFIRMED','REJECTED')),
  effective boolean NOT NULL DEFAULT true,
  supersedes_id uuid,
  shift_snapshot jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "attendance"."employee_ref" (tenant_id,
  employee_id),
  FOREIGN KEY (tenant_id,supersedes_id) REFERENCES "attendance"."attendance_session" (tenant_id,id),
  CHECK (check_out_at IS NULL OR check_out_at >= check_in_at)
);
CREATE INDEX "attendance_attendance_session_q1" ON "attendance"."attendance_session" (tenant_id,
  employee_id,work_date,check_in_at);
```


```sql
CREATE UNIQUE INDEX attendance_one_open_session ON attendance.attendance_session (tenant_id,
  employee_id) WHERE effective AND check_out_at IS NULL AND confirm_status <> 'REJECTED';
```


### attendance.attendancecorrection

Owner: `attendance` service schema. Purpose: Immutable requested values with mutable review summary; actions preserve decisions.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."attendance_correction" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  employee_id uuid NOT NULL,
  session_id uuid,
  work_date date NOT NULL,
  requested_by uuid NOT NULL,
  new_check_in timestamptz NOT NULL,
  new_check_out timestamptz,
  reason text NOT NULL,
  status text NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED')),
  decided_by uuid,
  decided_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "attendance"."employee_ref" (tenant_id,
  employee_id),
  FOREIGN KEY (tenant_id,session_id) REFERENCES "attendance"."attendance_session" (tenant_id,id),
  CHECK (new_check_out IS NULL OR new_check_out >= new_check_in),
  CHECK (decided_by IS NULL OR decided_by <> requested_by)
);
CREATE INDEX "attendance_attendance_correction_q1" ON "attendance"."attendance_correction"
  (tenant_id,status,created_at,id);
```


### attendance.correctionaction

Owner: `attendance` service schema. Purpose: Append-only correction decision; proposed supporting table for original correction workflow.

Retention: AUDIT; immutable fact/action; insert-only normal role.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."correction_action" (
  tenant_id uuid NOT NULL,
  id uuid NOT NULL,
  correction_id uuid NOT NULL,
  actor_id uuid NOT NULL,
  action text NOT NULL CHECK (action IN ('SUBMITTED','APPROVED','REJECTED')),
  reason text NOT NULL,
  at timestamptz NOT NULL,
  result_session_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (tenant_id,id),
  FOREIGN KEY (tenant_id,correction_id) REFERENCES "attendance"."attendance_correction" (tenant_id,
  id),
  FOREIGN KEY (tenant_id,result_session_id) REFERENCES "attendance"."attendance_session" (tenant_id,
  id),
  UNIQUE (tenant_id,correction_id,action)
);
```


### attendance.dailystatus

Owner: `attendance` service schema. Purpose: Recalculable current daily classification; orthogonal dimensions preserve exceptions.

Retention: BUSINESS; mutable current state; owner audit and version preconditions required.

PK and all FK/unique/check constraints are declared below; listed indexes serve tenant-scoped lookups/queues. If no extra index is listed, the primary/unique index serves the expected access path.

```sql
CREATE TABLE "attendance"."daily_status" (
  tenant_id uuid NOT NULL,
  employee_id uuid NOT NULL,
  work_date date NOT NULL,
  status text NOT NULL CHECK (status IN ('PRESENT','LATE','ABSENT','LEAVE_EARLY','ON_LEAVE',
  'HOLIDAY','DAY_OFF')),
  day_type text NOT NULL,
  attendance_state text NOT NULL,
  late boolean NOT NULL DEFAULT false,
  early_leave boolean NOT NULL DEFAULT false,
  first_in timestamptz,
  last_out timestamptz,
  late_minutes integer NOT NULL DEFAULT 0 CHECK (late_minutes >= 0),
  worked_minutes integer CHECK (worked_minutes >= 0),
  review_required boolean NOT NULL DEFAULT false,
  finalized_at timestamptz,
  decided_by uuid,
  shift_snapshot jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 1 CHECK (version > 0),
  PRIMARY KEY (tenant_id,employee_id,work_date),
  FOREIGN KEY (tenant_id,employee_id) REFERENCES "attendance"."employee_ref" (tenant_id,employee_id)
);
CREATE INDEX "attendance_daily_status_q1" ON "attendance"."daily_status" (tenant_id,work_date,
  status,employee_id);
```
