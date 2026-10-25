CREATE TABLE identity_org.auth_user (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    tenant_slug VARCHAR(63) NOT NULL,
    employee_id UUID NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    roles VARCHAR(120) NOT NULL,
    time_zone VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (tenant_slug, email)
);

CREATE TABLE identity_org.refresh_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity_org.auth_user(id),
    family_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX refresh_session_family_idx ON identity_org.refresh_session(family_id);
