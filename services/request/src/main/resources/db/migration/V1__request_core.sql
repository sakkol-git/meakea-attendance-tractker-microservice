CREATE TABLE request.request_type (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    form_schema JSONB,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_request_type_code UNIQUE (tenant_id, code)
);

CREATE TABLE request.material_item (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    advisory_stock INT NOT NULL DEFAULT 0,
    unit VARCHAR(32) NOT NULL DEFAULT 'UNIT',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_material_stock_nonneg CHECK (advisory_stock >= 0),
    CONSTRAINT uq_material_code UNIQUE (tenant_id, code)
);

CREATE TABLE request.general_request (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    employee_id VARCHAR(64) NOT NULL,
    employee_name VARCHAR(128),
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    quantity INT NOT NULL DEFAULT 1,
    form_data JSONB,
    handover_notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_request_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED', 'FULFILLED')),
    CONSTRAINT chk_request_quantity CHECK (quantity > 0)
);

CREATE TABLE request.request_line (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL REFERENCES request.general_request(id) ON DELETE CASCADE,
    material_item_id VARCHAR(64) NOT NULL REFERENCES request.material_item(id),
    quantity INT NOT NULL DEFAULT 1,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_line_quantity CHECK (quantity > 0)
);

CREATE TABLE request.approval_step (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL REFERENCES request.general_request(id) ON DELETE CASCADE,
    step_order INT NOT NULL,
    approver_id VARCHAR(64) NOT NULL,
    approver_name VARCHAR(128),
    approver_role VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    decision_reason TEXT,
    decided_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_step_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT uq_request_step UNIQUE (tenant_id, request_id, step_order)
);

CREATE TABLE request.request_comment (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL REFERENCES request.general_request(id) ON DELETE CASCADE,
    author_id VARCHAR(64) NOT NULL,
    author_name VARCHAR(128) NOT NULL,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE request.request_attachment (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL REFERENCES request.general_request(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_attachment_status CHECK (status IN ('PENDING', 'FINALIZED', 'ORPHAN')),
    CONSTRAINT chk_attachment_size CHECK (file_size > 0 AND file_size <= 10485760)
);

CREATE TABLE request.request_outbox (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    revision BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE request.request_inbox (
    consumer_name VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (consumer_name, tenant_id, event_id)
);

CREATE TABLE request.command_receipt (
    tenant_id VARCHAR(64) NOT NULL,
    command_key VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    settled_resource_id VARCHAR(64) NOT NULL,
    response_status INT NOT NULL,
    response_body JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, command_key)
);

ALTER TABLE request.request_type ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.request_type FORCE ROW LEVEL SECURITY;
CREATE POLICY request_type_tenant_isolation ON request.request_type
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.material_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.material_item FORCE ROW LEVEL SECURITY;
CREATE POLICY material_item_tenant_isolation ON request.material_item
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.general_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.general_request FORCE ROW LEVEL SECURITY;
CREATE POLICY general_request_tenant_isolation ON request.general_request
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.request_line ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.request_line FORCE ROW LEVEL SECURITY;
CREATE POLICY request_line_tenant_isolation ON request.request_line
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.approval_step ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.approval_step FORCE ROW LEVEL SECURITY;
CREATE POLICY approval_step_tenant_isolation ON request.approval_step
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.request_comment ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.request_comment FORCE ROW LEVEL SECURITY;
CREATE POLICY request_comment_tenant_isolation ON request.request_comment
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.request_attachment ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.request_attachment FORCE ROW LEVEL SECURITY;
CREATE POLICY request_attachment_tenant_isolation ON request.request_attachment
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.request_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.request_outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY request_outbox_tenant_isolation ON request.request_outbox
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.request_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.request_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY request_inbox_tenant_isolation ON request.request_inbox
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE request.command_receipt ENABLE ROW LEVEL SECURITY;
ALTER TABLE request.command_receipt FORCE ROW LEVEL SECURITY;
CREATE POLICY command_receipt_tenant_isolation ON request.command_receipt
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));
