CREATE TABLE identity_org.work_site (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    name VARCHAR(120) NOT NULL,
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    radius_m INTEGER NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT work_site_tenant_id_unique UNIQUE (tenant_id, id),
    CONSTRAINT work_site_latitude_check CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT work_site_longitude_check CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT work_site_radius_check CHECK (radius_m BETWEEN 1 AND 10000)
);

CREATE TABLE identity_org.shift (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    name VARCHAR(120) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    grace_minutes INTEGER NOT NULL DEFAULT 10,
    working_days INTEGER NOT NULL,
    site_id UUID NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    deleted_at TIMESTAMPTZ,
    last_change_reason VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT shift_tenant_id_unique UNIQUE (tenant_id, id),
    CONSTRAINT shift_site_fk FOREIGN KEY (tenant_id, site_id)
        REFERENCES identity_org.work_site(tenant_id, id),
    CONSTRAINT shift_same_day_check CHECK (end_time > start_time),
    CONSTRAINT shift_grace_check CHECK (grace_minutes BETWEEN 0 AND 120),
    CONSTRAINT shift_days_check CHECK (working_days BETWEEN 1 AND 127)
);
CREATE INDEX shift_tenant_site_idx ON identity_org.shift(tenant_id, site_id);

ALTER TABLE identity_org.work_site ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.work_site FORCE ROW LEVEL SECURITY;
CREATE POLICY work_site_tenant ON identity_org.work_site
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE identity_org.shift ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.shift FORCE ROW LEVEL SECURITY;
CREATE POLICY shift_tenant ON identity_org.shift
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
