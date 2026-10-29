CREATE TABLE notification.employee_ref (
    tenant_id uuid NOT NULL, employee_id uuid NOT NULL, account_id uuid,
    manager_id uuid, active boolean NOT NULL, roles jsonb,
    revision bigint NOT NULL CHECK(revision>0), PRIMARY KEY(tenant_id,employee_id)
);
ALTER TABLE notification.employee_ref ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification.employee_ref FORCE ROW LEVEL SECURITY;
CREATE POLICY employee_ref_tenant ON notification.employee_ref
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
