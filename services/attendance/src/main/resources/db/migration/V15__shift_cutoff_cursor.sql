CREATE TABLE attendance.cutoff_cursor (
    tenant_id uuid NOT NULL, employee_id uuid NOT NULL, next_date date NOT NULL,
    PRIMARY KEY(tenant_id,employee_id)
);
ALTER TABLE attendance.cutoff_cursor ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.cutoff_cursor FORCE ROW LEVEL SECURITY;
CREATE POLICY cutoff_cursor_tenant ON attendance.cutoff_cursor
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
CREATE TABLE attendance.cutoff_inbox (
    tenant_id uuid NOT NULL, event_id uuid NOT NULL, applied_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,event_id)
);
ALTER TABLE attendance.cutoff_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.cutoff_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY cutoff_inbox_tenant ON attendance.cutoff_inbox
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
ALTER TABLE attendance.attendance_outbox DROP CONSTRAINT typed_attendance_envelope;
ALTER TABLE attendance.attendance_outbox ADD CONSTRAINT typed_attendance_envelope CHECK (
    schema_version=0 OR (schema_version=1
      AND topic IN ('attendance.session.v1','attendance.daily-status.v1','time.shift-cutoff.v1')
      AND event_type IS NOT NULL AND correlation_id IS NOT NULL AND occurred_at IS NOT NULL AND event_key IS NOT NULL)
);
