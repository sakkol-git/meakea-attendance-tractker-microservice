-- Historical receipts have no actor/path provenance. Preserve them for reconciliation.
CREATE TABLE leave.command_receipt_v2 (
    tenant_id varchar(64) NOT NULL,
    actor_id varchar(64) NOT NULL,
    method varchar(8) NOT NULL CHECK(method='POST'),
    path varchar(512) NOT NULL,
    command_key uuid NOT NULL,
    request_hash varchar(64) NOT NULL,
    response_body text,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(tenant_id,actor_id,method,path,command_key)
);
ALTER TABLE leave.command_receipt_v2 ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.command_receipt_v2 FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_receipt_v2_tenant ON leave.command_receipt_v2
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),''));
