ALTER TABLE request.request_outbox
    ADD COLUMN wire_version smallint NOT NULL DEFAULT 0,
    ADD COLUMN topic varchar(120), ADD COLUMN event_key varchar(150),
    ADD COLUMN correlation_id uuid, ADD COLUMN occurred_at timestamptz,
    ADD COLUMN actor_account_id varchar(64),
    ADD COLUMN stream_position bigint GENERATED ALWAYS AS IDENTITY;
ALTER TABLE request.request_outbox ADD CONSTRAINT typed_request_envelope CHECK (
    wire_version=0 OR (wire_version=1 AND topic='request.workflow.v1' AND event_key IS NOT NULL
        AND correlation_id IS NOT NULL AND occurred_at IS NOT NULL AND actor_account_id IS NOT NULL AND revision>0)
);
CREATE UNIQUE INDEX request_fact_revision ON request.request_outbox(tenant_id,aggregate_id,revision) WHERE wire_version=1;
CREATE INDEX request_fact_pending ON request.request_outbox(tenant_id,event_key,stream_position) WHERE wire_version=1 AND status='PENDING';
CREATE SCHEMA IF NOT EXISTS request_jobs;
CREATE TABLE request_jobs.tenant_registry (tenant_id varchar(64) PRIMARY KEY);
CREATE FUNCTION request.register_outbox_tenant() RETURNS trigger LANGUAGE plpgsql
SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
    INSERT INTO request_jobs.tenant_registry VALUES (NEW.tenant_id) ON CONFLICT DO NOTHING;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION request.register_outbox_tenant() FROM PUBLIC;
CREATE TRIGGER register_outbox_tenant AFTER INSERT ON request.request_outbox
    FOR EACH ROW EXECUTE FUNCTION request.register_outbox_tenant();
CREATE TABLE request.workflow_history (
    tenant_id varchar(64) NOT NULL, event_id uuid NOT NULL, request_id varchar(64) NOT NULL,
    revision bigint NOT NULL CHECK(revision>0), actor_account_id varchar(64) NOT NULL,
    action varchar(128) NOT NULL, state varchar(32) NOT NULL, occurred_at timestamptz NOT NULL,
    PRIMARY KEY(tenant_id,event_id), UNIQUE(tenant_id,request_id,revision)
);
ALTER TABLE request.workflow_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.workflow_history FORCE ROW LEVEL SECURITY;
CREATE POLICY request_history_tenant ON request.workflow_history
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
CREATE FUNCTION request.prevent_history_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Workflow history is append-only' USING ERRCODE='42501'; END;
$$;
CREATE TRIGGER immutable_workflow_history BEFORE UPDATE OR DELETE ON request.workflow_history
    FOR EACH ROW EXECUTE FUNCTION request.prevent_history_change();
