CREATE TABLE identity_org.auth_tenant (
    id UUID PRIMARY KEY,
    slug VARCHAR(63) NOT NULL UNIQUE
);

INSERT INTO identity_org.auth_tenant (id, slug)
SELECT DISTINCT ON (tenant_slug) tenant_id, tenant_slug
FROM identity_org.auth_user
ORDER BY tenant_slug, id;

UPDATE identity_org.auth_user AS account
SET tenant_id = tenant.id
FROM identity_org.auth_tenant AS tenant
WHERE account.tenant_slug = tenant.slug;

ALTER TABLE identity_org.auth_user
ADD CONSTRAINT auth_user_tenant_fk FOREIGN KEY (tenant_id)
REFERENCES identity_org.auth_tenant(id);

CREATE UNIQUE INDEX auth_user_tenant_email_idx
ON identity_org.auth_user(tenant_id, email);
