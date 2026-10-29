CREATE TABLE attendance.daily_status (
    tenant_id uuid NOT NULL, employee_id uuid NOT NULL, work_date date NOT NULL,
    id uuid NOT NULL, revision bigint NOT NULL CHECK(revision>0), payload jsonb NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,employee_id,work_date),
    UNIQUE(tenant_id,id)
);
CREATE TABLE attendance.daily_recalculation (
    tenant_id uuid NOT NULL, employee_id uuid NOT NULL, work_date date NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,employee_id,work_date)
);
ALTER TABLE attendance.daily_status ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.daily_status FORCE ROW LEVEL SECURITY;
CREATE POLICY daily_status_tenant ON attendance.daily_status
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
ALTER TABLE attendance.daily_recalculation ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.daily_recalculation FORCE ROW LEVEL SECURITY;
CREATE POLICY daily_recalculation_tenant ON attendance.daily_recalculation
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
ALTER TABLE attendance.attendance_outbox DROP CONSTRAINT session_fact_envelope;
ALTER TABLE attendance.attendance_outbox ADD CONSTRAINT typed_attendance_envelope CHECK (
    schema_version=0 OR (schema_version=1
      AND topic IN ('attendance.session.v1','attendance.daily-status.v1')
      AND event_type IS NOT NULL AND correlation_id IS NOT NULL AND occurred_at IS NOT NULL AND event_key IS NOT NULL)
);
