CREATE TABLE attendance.schedule_daily_change (
    tenant_id uuid NOT NULL, event_id uuid NOT NULL,
    source_kind varchar(16) NOT NULL CHECK(source_kind IN ('EMPLOYEE','SHIFT')),
    source_id uuid NOT NULL,
    observed_at timestamptz NOT NULL,
    employee_cursor uuid, date_cursor date,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,event_id),
    CHECK((employee_cursor IS NULL)=(date_cursor IS NULL))
);
ALTER TABLE attendance.schedule_daily_change ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.schedule_daily_change FORCE ROW LEVEL SECURITY;
CREATE POLICY schedule_daily_change_tenant ON attendance.schedule_daily_change
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
