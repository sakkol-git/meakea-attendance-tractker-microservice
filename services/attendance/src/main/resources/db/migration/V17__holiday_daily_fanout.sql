CREATE TABLE attendance.holiday_daily_change (
    tenant_id uuid NOT NULL, event_id uuid NOT NULL,
    old_date date, new_date date NOT NULL,
    employee_cursor uuid, date_cursor date,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,event_id),
    CHECK((employee_cursor IS NULL)=(date_cursor IS NULL))
);
ALTER TABLE attendance.holiday_daily_change ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.holiday_daily_change FORCE ROW LEVEL SECURITY;
CREATE POLICY holiday_daily_change_tenant ON attendance.holiday_daily_change
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
