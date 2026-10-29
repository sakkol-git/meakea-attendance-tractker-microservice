-- Attendance owns overlap evidence; leave owns the review transition. No balance adjustment is automatic.
CREATE TABLE leave.attendance_inbox (
    tenant_id varchar(64) NOT NULL,
    event_id uuid NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,event_id)
);
CREATE TABLE leave.attendance_day_revision (
    tenant_id varchar(64) NOT NULL,
    employee_id varchar(64) NOT NULL,
    work_date date NOT NULL,
    aggregate_id uuid NOT NULL,
    revision bigint NOT NULL CHECK(revision>0),
    PRIMARY KEY(tenant_id,employee_id,work_date)
);
CREATE TABLE leave.attendance_review (
    tenant_id varchar(64) NOT NULL,
    event_id uuid NOT NULL,
    leave_id varchar(64) NOT NULL,
    work_date date NOT NULL,
    attendance_id uuid NOT NULL,
    attendance_revision bigint NOT NULL,
    approval_revision bigint NOT NULL,
    portion varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,event_id,leave_id)
);
ALTER TABLE leave.attendance_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.attendance_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY attendance_inbox_tenant ON leave.attendance_inbox USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
ALTER TABLE leave.attendance_day_revision ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.attendance_day_revision FORCE ROW LEVEL SECURITY;
CREATE POLICY attendance_day_tenant ON leave.attendance_day_revision USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
ALTER TABLE leave.attendance_review ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.attendance_review FORCE ROW LEVEL SECURITY;
CREATE POLICY attendance_review_tenant ON leave.attendance_review USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
