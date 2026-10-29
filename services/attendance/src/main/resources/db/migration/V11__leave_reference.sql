CREATE TABLE attendance.leave_inbox (
    tenant_id uuid NOT NULL, event_id uuid NOT NULL,
    applied_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,event_id)
);
CREATE TABLE attendance.leave_projection (
    tenant_id uuid NOT NULL, leave_id uuid NOT NULL, employee_id uuid NOT NULL,
    portion varchar(20) NOT NULL CHECK(portion IN ('FULL_DAY','MORNING','AFTERNOON')),
    state varchar(20) NOT NULL CHECK(state IN ('PENDING','APPROVED','REJECTED','CANCELLED','NEEDS_REVIEW')),
    working_dates jsonb NOT NULL CHECK(jsonb_typeof(working_dates)='array'),
    charged_days numeric(12,2) NOT NULL CHECK(charged_days>0),
    revision bigint NOT NULL CHECK(revision>0), PRIMARY KEY(tenant_id,leave_id)
);
CREATE INDEX leave_employee_idx ON attendance.leave_projection(tenant_id,employee_id);
ALTER TABLE attendance.leave_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.leave_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_inbox_tenant ON attendance.leave_inbox
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
ALTER TABLE attendance.leave_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.leave_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_projection_tenant ON attendance.leave_projection
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
