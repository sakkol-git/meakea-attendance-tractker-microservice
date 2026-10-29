CREATE TABLE request.employee_projection (
    tenant_id varchar(64) NOT NULL, employee_id varchar(64) NOT NULL,
    manager_id varchar(64), active boolean NOT NULL, revision bigint NOT NULL CHECK(revision>0),
    PRIMARY KEY(tenant_id,employee_id)
);
CREATE TABLE request.organization_inbox (
    tenant_id varchar(64) NOT NULL, event_id uuid NOT NULL,
    applied_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,event_id)
);
CREATE INDEX request_manager_idx ON request.employee_projection(tenant_id,manager_id) WHERE active=true;
ALTER TABLE request.employee_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.employee_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY request_employee_tenant ON request.employee_projection
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
ALTER TABLE request.organization_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.organization_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY request_org_inbox_tenant ON request.organization_inbox
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
