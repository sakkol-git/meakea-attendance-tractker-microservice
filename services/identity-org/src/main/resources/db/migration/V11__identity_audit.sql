CREATE TABLE identity_org.audit_log (
    tenant_id uuid NOT NULL,
    id uuid NOT NULL,
    actor_id uuid,
    action text NOT NULL,
    entity text NOT NULL,
    entity_id uuid,
    diff jsonb NOT NULL,
    at timestamptz NOT NULL,
    correlation_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, id)
);
CREATE INDEX identity_audit_page ON identity_org.audit_log (tenant_id, at DESC, id DESC);
ALTER TABLE identity_org.audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.audit_log FORCE ROW LEVEL SECURITY;
CREATE POLICY identity_audit_tenant ON identity_org.audit_log
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

CREATE FUNCTION identity_org.reject_audit_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Identity audit is append-only' USING ERRCODE = '42501';
END;
$$;
CREATE TRIGGER identity_audit_append_only
    BEFORE UPDATE OR DELETE OR TRUNCATE ON identity_org.audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION identity_org.reject_audit_change();
