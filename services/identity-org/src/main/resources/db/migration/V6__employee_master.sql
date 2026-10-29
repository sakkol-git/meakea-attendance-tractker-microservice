CREATE TABLE identity_org.department (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT department_tenant_id_unique UNIQUE (tenant_id, id),
    CONSTRAINT department_tenant_code_unique UNIQUE (tenant_id, code)
);

CREATE TABLE identity_org.position (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT position_tenant_id_unique UNIQUE (tenant_id, id),
    CONSTRAINT position_tenant_code_unique UNIQUE (tenant_id, code)
);

CREATE TABLE identity_org.employee (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES identity_org.auth_tenant(id),
    employee_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    department_id UUID NOT NULL,
    position_id UUID NOT NULL,
    manager_id UUID,
    hire_date DATE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT employee_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'TERMINATED')),
    CONSTRAINT employee_no_self_manager CHECK (manager_id IS NULL OR manager_id <> id),
    CONSTRAINT employee_tenant_id_unique UNIQUE (tenant_id, id),
    CONSTRAINT employee_tenant_code_unique UNIQUE (tenant_id, employee_code),
    CONSTRAINT employee_tenant_email_unique UNIQUE (tenant_id, email),
    CONSTRAINT employee_department_fk FOREIGN KEY (tenant_id, department_id)
        REFERENCES identity_org.department(tenant_id, id),
    CONSTRAINT employee_position_fk FOREIGN KEY (tenant_id, position_id)
        REFERENCES identity_org.position(tenant_id, id),
    CONSTRAINT employee_manager_fk FOREIGN KEY (tenant_id, manager_id)
        REFERENCES identity_org.employee(tenant_id, id)
);

CREATE INDEX employee_tenant_department_idx ON identity_org.employee(tenant_id, department_id);
CREATE INDEX employee_tenant_manager_idx ON identity_org.employee(tenant_id, manager_id);

CREATE FUNCTION identity_org.reject_employee_manager_cycle() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.manager_id IS NULL THEN
        RETURN NEW;
    END IF;
    -- Serialize hierarchy edits within a tenant before checking the manager chain.
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.tenant_id::text, 0));
    IF EXISTS (
        WITH RECURSIVE managers(id, manager_id) AS (
            SELECT id, manager_id FROM identity_org.employee
            WHERE tenant_id = NEW.tenant_id AND id = NEW.manager_id
            UNION ALL
            SELECT e.id, e.manager_id FROM identity_org.employee e
            JOIN managers m ON e.id = m.manager_id
            WHERE e.tenant_id = NEW.tenant_id
        )
        SELECT 1 FROM managers WHERE id = NEW.id
    ) THEN
        RAISE EXCEPTION 'Employee manager hierarchy would cycle' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER employee_manager_cycle_guard
BEFORE INSERT OR UPDATE OF manager_id, tenant_id ON identity_org.employee
FOR EACH ROW EXECUTE FUNCTION identity_org.reject_employee_manager_cycle();

CREATE TABLE identity_org.command_receipt (
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    method VARCHAR(8) NOT NULL,
    path VARCHAR(200) NOT NULL,
    command_key UUID NOT NULL,
    request_hash CHAR(64) NOT NULL,
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, actor_id, method, path, command_key)
);

-- The runtime role is distinct from the migration owner and must never have BYPASSRLS.
-- An unset transaction context resolves to NULL and therefore denies reads and writes.
ALTER TABLE identity_org.department ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.department FORCE ROW LEVEL SECURITY;
CREATE POLICY department_tenant ON identity_org.department
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE identity_org.position ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.position FORCE ROW LEVEL SECURITY;
CREATE POLICY position_tenant ON identity_org.position
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE identity_org.employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.employee FORCE ROW LEVEL SECURITY;
CREATE POLICY employee_tenant ON identity_org.employee
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE identity_org.command_receipt ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_org.command_receipt FORCE ROW LEVEL SECURITY;
CREATE POLICY command_receipt_tenant ON identity_org.command_receipt
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
