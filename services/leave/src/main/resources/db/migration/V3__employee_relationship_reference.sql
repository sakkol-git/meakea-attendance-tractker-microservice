CREATE TABLE leave.employee_projection (
    tenant_id varchar(64) NOT NULL, employee_id varchar(64) NOT NULL,
    manager_id varchar(64), active boolean NOT NULL, revision bigint NOT NULL CHECK(revision>0),
    PRIMARY KEY(tenant_id,employee_id)
);
CREATE TABLE leave.organization_inbox (
    tenant_id varchar(64) NOT NULL, event_id uuid NOT NULL,
    applied_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,event_id)
);
CREATE INDEX leave_manager_idx ON leave.employee_projection(tenant_id,manager_id) WHERE active=true;
ALTER TABLE leave.employee_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.employee_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_employee_tenant ON leave.employee_projection
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
ALTER TABLE leave.organization_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.organization_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_org_inbox_tenant ON leave.organization_inbox
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
