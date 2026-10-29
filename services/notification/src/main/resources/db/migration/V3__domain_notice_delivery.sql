CREATE TABLE notification.workflow_revision (
    tenant_id uuid NOT NULL, topic varchar(120) NOT NULL, aggregate_id uuid NOT NULL,
    employee_id uuid NOT NULL, revision bigint NOT NULL CHECK(revision>0), PRIMARY KEY(tenant_id,topic,aggregate_id)
);
ALTER TABLE notification.workflow_revision ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification.workflow_revision FORCE ROW LEVEL SECURITY;
CREATE POLICY workflow_revision_tenant ON notification.workflow_revision
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
ALTER TABLE notification.notification_record ADD COLUMN origin_event_id uuid;
ALTER TABLE notification.notification_record ADD COLUMN channel varchar(16) NOT NULL DEFAULT 'IN_APP';
CREATE UNIQUE INDEX notification_event_recipient_channel_unique
    ON notification.notification_record(tenant_id,origin_event_id,recipient_id,channel) WHERE origin_event_id IS NOT NULL;
