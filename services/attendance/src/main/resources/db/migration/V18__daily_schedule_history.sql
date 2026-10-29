-- Preserve the actual first observed daily schedule, never infer missing historical calendars from today's shift.
CREATE TABLE attendance.daily_schedule (
    tenant_id uuid NOT NULL, employee_id uuid NOT NULL, work_date date NOT NULL,
    shift_id uuid NOT NULL, shift_revision bigint NOT NULL CHECK(shift_revision>0),
    timezone varchar(80) NOT NULL, scheduled_start timestamptz NOT NULL, scheduled_end timestamptz NOT NULL,
    grace_minutes integer NOT NULL CHECK(grace_minutes>=0), working_days integer NOT NULL CHECK(working_days BETWEEN 0 AND 127),
    PRIMARY KEY(tenant_id,employee_id,work_date), CHECK(scheduled_end>scheduled_start)
);
ALTER TABLE attendance.daily_schedule ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.daily_schedule FORCE ROW LEVEL SECURITY;
CREATE POLICY daily_schedule_tenant ON attendance.daily_schedule
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
