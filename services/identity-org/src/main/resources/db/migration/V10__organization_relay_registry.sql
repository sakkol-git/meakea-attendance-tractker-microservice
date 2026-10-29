-- UUID-only discovery metadata lives outside the business runtime schema grant.
CREATE TABLE identity_jobs.organization_tenant_registry (
    tenant_id UUID PRIMARY KEY REFERENCES identity_org.auth_tenant(id),
    lifecycle VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        CHECK (lifecycle IN ('ACTIVE', 'SUSPENDED')),
    revision BIGINT NOT NULL DEFAULT 1 CHECK (revision > 0)
);

INSERT INTO identity_jobs.organization_tenant_registry(tenant_id)
SELECT id FROM identity_org.auth_tenant;

CREATE FUNCTION identity_org.register_organization_tenant() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = pg_catalog, identity_jobs AS $$
BEGIN
    INSERT INTO identity_jobs.organization_tenant_registry(tenant_id)
    VALUES (NEW.id) ON CONFLICT (tenant_id) DO NOTHING;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION identity_org.register_organization_tenant() FROM PUBLIC;

CREATE TRIGGER organization_tenant_registry_insert
AFTER INSERT ON identity_org.auth_tenant
FOR EACH ROW EXECUTE FUNCTION identity_org.register_organization_tenant();
