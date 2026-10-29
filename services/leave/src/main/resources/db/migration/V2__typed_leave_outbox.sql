ALTER TABLE leave.leave_outbox
    ADD COLUMN wire_version smallint NOT NULL DEFAULT 0,
    ADD COLUMN topic varchar(120),
    ADD COLUMN event_key varchar(150),
    ADD COLUMN correlation_id uuid,
    ADD COLUMN occurred_at timestamptz,
    ADD COLUMN stream_position bigint GENERATED ALWAYS AS IDENTITY;
ALTER TABLE leave.leave_outbox ADD CONSTRAINT typed_leave_envelope CHECK (
    wire_version=0 OR (wire_version=1 AND topic='leave.request.v1' AND event_key IS NOT NULL
        AND correlation_id IS NOT NULL AND occurred_at IS NOT NULL AND revision>0)
);
CREATE UNIQUE INDEX leave_fact_revision ON leave.leave_outbox(tenant_id,aggregate_id,revision) WHERE wire_version=1;
CREATE INDEX leave_fact_pending ON leave.leave_outbox(tenant_id,event_key,stream_position) WHERE wire_version=1 AND status='PENDING';
CREATE SCHEMA IF NOT EXISTS leave_jobs;
CREATE TABLE leave_jobs.tenant_registry (tenant_id varchar(64) PRIMARY KEY);
CREATE FUNCTION leave.register_outbox_tenant() RETURNS trigger LANGUAGE plpgsql
SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
    INSERT INTO leave_jobs.tenant_registry VALUES (NEW.tenant_id) ON CONFLICT DO NOTHING;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION leave.register_outbox_tenant() FROM PUBLIC;
CREATE TRIGGER register_outbox_tenant AFTER INSERT ON leave.leave_outbox
    FOR EACH ROW EXECUTE FUNCTION leave.register_outbox_tenant();
