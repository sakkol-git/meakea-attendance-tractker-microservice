CREATE TABLE reporting.employee_projection (
    tenant_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    manager_id uuid,
    active boolean NOT NULL,
    revision bigint NOT NULL CHECK (revision>0),
    PRIMARY KEY (tenant_id,employee_id)
);
ALTER TABLE reporting.employee_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.employee_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY reporting_employee_tenant ON reporting.employee_projection
    USING (tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
CREATE INDEX reporting_employee_manager ON reporting.employee_projection(tenant_id,manager_id) WHERE active;
