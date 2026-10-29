-- Notification Core Schema: inbox, records, preferences, devices, and outbox

CREATE TABLE notification.notification_inbox (
    consumer_name VARCHAR(100) NOT NULL,
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    topic VARCHAR(120) NOT NULL,
    aggregate_id UUID NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_inbox_pk PRIMARY KEY (consumer_name, tenant_id, event_id)
);

CREATE TABLE notification.notification_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    recipient_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    detail TEXT,
    body TEXT,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    record_id VARCHAR(100),
    kind VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notification.notification_preference (
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    email BOOLEAN NOT NULL DEFAULT TRUE,
    in_app BOOLEAN NOT NULL DEFAULT TRUE,
    reminders BOOLEAN NOT NULL DEFAULT TRUE,
    digest_frequency VARCHAR(50) NOT NULL DEFAULT 'DAILY',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_preference_pk PRIMARY KEY (tenant_id, employee_id)
);

CREATE TABLE notification.notification_device (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    device_token VARCHAR(255) NOT NULL,
    platform VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_device_unique UNIQUE (tenant_id, employee_id, device_token)
);

CREATE TABLE notification.notification_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    topic VARCHAR(120) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    aggregate_revision BIGINT NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX notification_inbox_lookup_idx ON notification.notification_inbox (tenant_id, event_id);
CREATE INDEX notification_record_recipient_idx ON notification.notification_record (tenant_id, recipient_id, created_at DESC);
CREATE INDEX notification_device_employee_idx ON notification.notification_device (tenant_id, employee_id);
CREATE INDEX notification_outbox_pending_idx ON notification.notification_outbox (tenant_id, created_at) WHERE published_at IS NULL;

-- Enable and FORCE Row-Level Security
ALTER TABLE notification.notification_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification.notification_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_inbox_tenant ON notification.notification_inbox
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE notification.notification_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification.notification_record FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_record_tenant ON notification.notification_record
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE notification.notification_preference ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification.notification_preference FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_preference_tenant ON notification.notification_preference
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE notification.notification_device ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification.notification_device FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_device_tenant ON notification.notification_device
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE notification.notification_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification.notification_outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY notification_outbox_tenant ON notification.notification_outbox
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
