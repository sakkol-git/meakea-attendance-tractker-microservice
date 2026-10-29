ALTER TABLE reporting.export_job ADD CONSTRAINT export_job_tenant_id_unique UNIQUE (tenant_id,id);
CREATE TABLE reporting.export_snapshot (
    tenant_id uuid NOT NULL,
    job_id uuid NOT NULL,
    rows jsonb NOT NULL CHECK (jsonb_typeof(rows)='array'),
    captured_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id,job_id),
    FOREIGN KEY (tenant_id,job_id) REFERENCES reporting.export_job(tenant_id,id)
);
ALTER TABLE reporting.export_snapshot ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.export_snapshot FORCE ROW LEVEL SECURITY;
CREATE POLICY export_snapshot_tenant ON reporting.export_snapshot
    USING (tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);

CREATE SCHEMA IF NOT EXISTS reporting_jobs;
CREATE TABLE reporting_jobs.tenant_registry (tenant_id uuid PRIMARY KEY);
INSERT INTO reporting_jobs.tenant_registry SELECT DISTINCT tenant_id FROM reporting.export_job;
CREATE FUNCTION reporting.register_export_tenant() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
    INSERT INTO reporting_jobs.tenant_registry VALUES (NEW.tenant_id) ON CONFLICT DO NOTHING;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION reporting.register_export_tenant() FROM PUBLIC;
CREATE TRIGGER register_export_tenant AFTER INSERT ON reporting.export_job
    FOR EACH ROW EXECUTE FUNCTION reporting.register_export_tenant();
CREATE INDEX export_job_pending ON reporting.export_job (tenant_id,created_at,id) WHERE status='QUEUED';
