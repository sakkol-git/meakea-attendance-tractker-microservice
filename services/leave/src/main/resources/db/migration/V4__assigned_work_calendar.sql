ALTER TABLE leave.employee_projection ADD COLUMN assignments jsonb;
ALTER TABLE leave.employee_projection ADD CONSTRAINT leave_assignment_shape
    CHECK(assignments IS NULL OR jsonb_typeof(assignments)='array');
CREATE TABLE leave.shift_projection (
    tenant_id varchar(64) NOT NULL, shift_id varchar(64) NOT NULL,
    start_time time NOT NULL, end_time time NOT NULL CHECK(end_time>start_time), timezone varchar(64) NOT NULL,
    working_days integer NOT NULL CHECK(working_days BETWEEN 1 AND 127), deleted boolean NOT NULL,
    revision bigint NOT NULL CHECK(revision>0), PRIMARY KEY(tenant_id,shift_id)
);
CREATE TABLE leave.holiday_projection (
    tenant_id varchar(64) NOT NULL, holiday_id varchar(64) NOT NULL, date date NOT NULL,
    deleted boolean NOT NULL, revision bigint NOT NULL CHECK(revision>0), PRIMARY KEY(tenant_id,holiday_id)
);
ALTER TABLE leave.shift_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.shift_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_shift_tenant ON leave.shift_projection
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
ALTER TABLE leave.holiday_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.holiday_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_holiday_tenant ON leave.holiday_projection
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
ALTER TABLE leave.leave_day ADD COLUMN scheduled_seconds bigint CHECK(scheduled_seconds>0);
