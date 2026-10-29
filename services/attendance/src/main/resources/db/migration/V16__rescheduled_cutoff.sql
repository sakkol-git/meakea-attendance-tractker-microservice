CREATE TABLE attendance.cutoff_recheck (
    tenant_id uuid NOT NULL, employee_id uuid NOT NULL, work_date date NOT NULL,
    available_at timestamptz NOT NULL,
    PRIMARY KEY(tenant_id,employee_id,work_date)
);
CREATE INDEX attendance_cutoff_due ON attendance.cutoff_recheck(tenant_id,employee_id,available_at);
ALTER TABLE attendance.cutoff_recheck ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.cutoff_recheck FORCE ROW LEVEL SECURITY;
CREATE POLICY cutoff_recheck_tenant ON attendance.cutoff_recheck
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
