-- Existing bootstrap identities remain unlinked. Never guess or rewrite employee associations.
-- Pre-existing duplicate enabled links require explicit operator reconciliation before this upgrade.
CREATE UNIQUE INDEX auth_user_active_employee_unique
    ON identity_org.auth_user(tenant_id,employee_id) WHERE enabled;
