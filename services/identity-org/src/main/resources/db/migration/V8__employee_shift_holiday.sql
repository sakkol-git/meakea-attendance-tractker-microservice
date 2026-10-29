CREATE TABLE identity_org.employee_shift (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    employee_id UUID NOT NULL,
    shift_id UUID NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    shift_snapshot JSONB NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT employee_shift_tenant_id_unique UNIQUE (tenant_id, id),
    CONSTRAINT employee_shift_employee_fk FOREIGN KEY (tenant_id, employee_id)
        REFERENCES identity_org.employee(tenant_id, id),
    CONSTRAINT employee_shift_shift_fk FOREIGN KEY (tenant_id, shift_id)
        REFERENCES identity_org.shift(tenant_id, id),
    CONSTRAINT employee_shift_period_check CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT employee_shift_start_unique UNIQUE (tenant_id, employee_id, valid_from)
);
CREATE INDEX employee_shift_lookup_idx
    ON identity_org.employee_shift(tenant_id, employee_id, valid_from, valid_to);

CREATE FUNCTION identity_org.reject_overlapping_employee_shift() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    -- Use the same employee row lock as the owner service to serialize direct SQL and API writes.
    PERFORM 1 FROM identity_org.employee
    WHERE tenant_id = NEW.tenant_id AND id = NEW.employee_id FOR UPDATE;
    IF EXISTS (
        SELECT 1 FROM identity_org.employee_shift existing
        WHERE existing.tenant_id = NEW.tenant_id
          AND existing.employee_id = NEW.employee_id
          AND existing.id <> NEW.id
          AND existing.valid_from <= coalesce(NEW.valid_to, 'infinity'::date)
          AND NEW.valid_from <= coalesce(existing.valid_to, 'infinity'::date)
    ) THEN
        RAISE EXCEPTION 'Employee shift effective periods overlap' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER employee_shift_overlap_guard
BEFORE INSERT OR UPDATE OF tenant_id, employee_id, valid_from, valid_to
ON identity_org.employee_shift
FOR EACH ROW EXECUTE FUNCTION identity_org.reject_overlapping_employee_shift();

CREATE TABLE identity_org.holiday (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    date DATE NOT NULL,
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT holiday_tenant_id_unique UNIQUE (tenant_id, id),
    CONSTRAINT holiday_kind_check CHECK (kind IN ('PUBLIC', 'COMPANY')),
    CONSTRAINT holiday_tenant_date_kind_unique UNIQUE (tenant_id, date, kind)
);
CREATE INDEX holiday_tenant_date_idx ON identity_org.holiday(tenant_id, date);

ALTER TABLE identity_org.employee_shift ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.employee_shift FORCE ROW LEVEL SECURITY;
CREATE POLICY employee_shift_tenant ON identity_org.employee_shift
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE identity_org.holiday ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.holiday FORCE ROW LEVEL SECURITY;
CREATE POLICY holiday_tenant ON identity_org.holiday
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
