CREATE TABLE attendance.organization_inbox (
    consumer_name VARCHAR(64) NOT NULL,
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    topic VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_revision BIGINT NOT NULL CHECK (aggregate_revision > 0),
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer_name, tenant_id, event_id)
);

CREATE TABLE attendance.organization_revision (
    tenant_id UUID NOT NULL,
    topic VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    revision BIGINT NOT NULL CHECK (revision > 0),
    PRIMARY KEY (tenant_id, topic, aggregate_id)
);

CREATE TABLE attendance.employee_projection (
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    employee_no VARCHAR(64) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    status VARCHAR(16) NOT NULL,
    assignments JSONB NOT NULL,
    revision BIGINT NOT NULL CHECK (revision > 0),
    PRIMARY KEY (tenant_id, employee_id)
);

CREATE TABLE attendance.shift_projection (
    tenant_id UUID NOT NULL,
    shift_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    grace_minutes INTEGER NOT NULL,
    working_days INTEGER NOT NULL,
    site_id UUID NOT NULL,
    site_name VARCHAR(120) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius_m DOUBLE PRECISION NOT NULL,
    deleted_at TIMESTAMPTZ,
    revision BIGINT NOT NULL CHECK (revision > 0),
    PRIMARY KEY (tenant_id, shift_id),
    CONSTRAINT shift_projection_same_day CHECK (end_time > start_time),
    CONSTRAINT shift_projection_radius CHECK (radius_m > 0)
);

CREATE TABLE attendance.holiday_projection (
    tenant_id UUID NOT NULL,
    holiday_id UUID NOT NULL,
    date DATE NOT NULL,
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('PUBLIC', 'COMPANY')),
    deleted_at TIMESTAMPTZ,
    revision BIGINT NOT NULL CHECK (revision > 0),
    PRIMARY KEY (tenant_id, holiday_id)
);

CREATE INDEX employee_projection_status_idx ON attendance.employee_projection(tenant_id, status);
CREATE INDEX holiday_projection_date_idx ON attendance.holiday_projection(tenant_id, date);

ALTER TABLE attendance.organization_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.organization_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_inbox_tenant ON attendance.organization_inbox
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE attendance.organization_revision ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.organization_revision FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_revision_tenant ON attendance.organization_revision
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE attendance.employee_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.employee_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY employee_projection_tenant ON attendance.employee_projection
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE attendance.shift_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.shift_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY shift_projection_tenant ON attendance.shift_projection
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE attendance.holiday_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.holiday_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY holiday_projection_tenant ON attendance.holiday_projection
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
