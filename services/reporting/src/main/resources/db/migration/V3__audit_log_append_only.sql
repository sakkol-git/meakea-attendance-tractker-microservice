-- V3__audit_log_append_only.sql
-- Enforce append-only semantics on reporting.audit_log at database level.
-- The application role may insert and tenant-read audit records. UPDATE and DELETE must fail at database level.

-- 1. Trigger function that rejects UPDATE and DELETE operations
CREATE OR REPLACE FUNCTION reporting.enforce_audit_log_append_only()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'reporting.audit_log is append-only: % is not permitted', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_append_only_stmt
    BEFORE UPDATE OR DELETE ON reporting.audit_log
    FOR EACH STATEMENT
    EXECUTE FUNCTION reporting.enforce_audit_log_append_only();

CREATE TRIGGER trg_audit_log_append_only_row
    BEFORE UPDATE OR DELETE ON reporting.audit_log
    FOR EACH ROW
    EXECUTE FUNCTION reporting.enforce_audit_log_append_only();

-- 2. Restructure Row-Level Security policies: permit tenant SELECT and INSERT, deny UPDATE and DELETE
DROP POLICY IF EXISTS audit_log_tenant ON reporting.audit_log;

CREATE POLICY audit_log_select_tenant ON reporting.audit_log
    FOR SELECT USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

CREATE POLICY audit_log_insert_tenant ON reporting.audit_log
    FOR INSERT WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

-- 3. Revoke UPDATE and DELETE privileges from PUBLIC and application role
REVOKE UPDATE, DELETE ON reporting.audit_log FROM PUBLIC;

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'reporting_app') THEN
        REVOKE UPDATE, DELETE ON reporting.audit_log FROM reporting_app;
        GRANT SELECT, INSERT ON reporting.audit_log TO reporting_app;
    END IF;
END $$;
