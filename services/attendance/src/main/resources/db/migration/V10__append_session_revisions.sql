-- attendance_session is the current effective view. Retain each interpretation separately and immutably.
CREATE TABLE attendance.session_revision (
    tenant_id uuid NOT NULL,
    session_id uuid NOT NULL,
    revision bigint NOT NULL CHECK (revision>0),
    snapshot jsonb NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id,session_id,revision)
);
ALTER TABLE attendance.session_revision ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.session_revision FORCE ROW LEVEL SECURITY;
CREATE POLICY session_revision_tenant ON attendance.session_revision
    USING (tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);

CREATE FUNCTION attendance.append_session_revision() RETURNS trigger
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
    IF TG_OP='UPDATE' THEN
        INSERT INTO attendance.session_revision(tenant_id,session_id,revision,snapshot)
        VALUES (OLD.tenant_id,OLD.id,OLD.version+1,to_jsonb(OLD)) ON CONFLICT DO NOTHING;
    END IF;
    INSERT INTO attendance.session_revision(tenant_id,session_id,revision,snapshot)
    VALUES (NEW.tenant_id,NEW.id,NEW.version+1,to_jsonb(NEW)) ON CONFLICT DO NOTHING;
    RETURN NEW;
END;
$$;
CREATE TRIGGER append_session_revision AFTER INSERT OR UPDATE ON attendance.attendance_session
    FOR EACH ROW EXECUTE FUNCTION attendance.append_session_revision();

CREATE FUNCTION attendance.reject_session_revision_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Session revisions are append-only';
END;
$$;
CREATE TRIGGER session_revision_no_change BEFORE UPDATE OR DELETE ON attendance.session_revision
    FOR EACH ROW EXECUTE FUNCTION attendance.reject_session_revision_change();
CREATE TRIGGER session_revision_no_truncate BEFORE TRUNCATE ON attendance.session_revision
    FOR EACH STATEMENT EXECUTE FUNCTION attendance.reject_session_revision_change();
