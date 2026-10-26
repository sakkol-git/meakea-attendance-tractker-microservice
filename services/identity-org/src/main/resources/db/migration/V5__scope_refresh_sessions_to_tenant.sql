ALTER TABLE identity_org.refresh_session
ADD COLUMN tenant_id UUID;

UPDATE identity_org.refresh_session AS session
SET tenant_id = account.tenant_id
FROM identity_org.auth_user AS account
WHERE session.user_id = account.id;

ALTER TABLE identity_org.refresh_session
ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE identity_org.auth_user
ADD CONSTRAINT auth_user_id_tenant_unique UNIQUE (id, tenant_id);

ALTER TABLE identity_org.refresh_session
DROP CONSTRAINT refresh_session_user_id_fkey;

ALTER TABLE identity_org.refresh_session
ADD CONSTRAINT refresh_session_user_tenant_fk
FOREIGN KEY (user_id, tenant_id)
REFERENCES identity_org.auth_user(id, tenant_id);

DROP INDEX identity_org.refresh_session_family_idx;

CREATE INDEX refresh_session_tenant_family_idx
ON identity_org.refresh_session(tenant_id, family_id);
