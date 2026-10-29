CREATE TABLE identity_org.organization_revision (
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    topic VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    revision BIGINT NOT NULL CHECK (revision > 0),
    PRIMARY KEY (tenant_id, topic, aggregate_id)
);

CREATE TABLE identity_org.organization_outbox (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    topic VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_revision BIGINT NOT NULL CHECK (aggregate_revision > 0),
    event_type VARCHAR(128) NOT NULL,
    schema_version INTEGER NOT NULL DEFAULT 1 CHECK (schema_version = 1),
    correlation_id UUID NOT NULL,
    causation_id UUID,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    delivery_attempts INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT organization_outbox_tenant_id_unique UNIQUE (tenant_id, id),
    CONSTRAINT organization_outbox_revision_unique
        UNIQUE (tenant_id, topic, aggregate_id, aggregate_revision),
    CONSTRAINT organization_outbox_topic_check
        CHECK (topic IN ('org.employee.v1', 'org.shift.v1', 'org.holiday.v1'))
);
CREATE INDEX organization_outbox_pending_idx
    ON identity_org.organization_outbox(tenant_id, topic, aggregate_id, aggregate_revision)
    WHERE published_at IS NULL;

ALTER TABLE identity_org.organization_revision ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.organization_revision FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_revision_tenant ON identity_org.organization_revision
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE identity_org.organization_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.organization_outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY organization_outbox_tenant ON identity_org.organization_outbox
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
