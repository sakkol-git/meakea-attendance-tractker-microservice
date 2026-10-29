ALTER TABLE attendance.attendance_outbox
    ADD COLUMN schema_version integer NOT NULL DEFAULT 0 CHECK (schema_version IN (0,1)),
    ADD COLUMN event_type varchar(128),
    ADD COLUMN correlation_id uuid,
    ADD COLUMN occurred_at timestamptz,
    ADD COLUMN event_key varchar(80);
ALTER TABLE attendance.attendance_outbox ADD CONSTRAINT session_fact_envelope CHECK (
    schema_version = 0 OR
    (schema_version = 1 AND topic = 'attendance.session.v1' AND event_type IS NOT NULL
        AND correlation_id IS NOT NULL AND occurred_at IS NOT NULL AND event_key IS NOT NULL)
);
CREATE UNIQUE INDEX attendance_outbox_revision
    ON attendance.attendance_outbox(tenant_id,topic,aggregate_id,aggregate_revision);

-- Private UUID-only discovery; no attendance payload is exposed to the job role.
CREATE SCHEMA IF NOT EXISTS attendance_jobs;
CREATE TABLE attendance_jobs.tenant_registry (tenant_id uuid PRIMARY KEY);
INSERT INTO attendance_jobs.tenant_registry SELECT DISTINCT tenant_id FROM attendance.attendance_outbox;
CREATE FUNCTION attendance.register_outbox_tenant() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = pg_catalog, attendance_jobs AS $$
BEGIN
    INSERT INTO attendance_jobs.tenant_registry(tenant_id) VALUES (NEW.tenant_id) ON CONFLICT DO NOTHING;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION attendance.register_outbox_tenant() FROM PUBLIC;
CREATE TRIGGER attendance_outbox_tenant AFTER INSERT ON attendance.attendance_outbox
    FOR EACH ROW EXECUTE FUNCTION attendance.register_outbox_tenant();
