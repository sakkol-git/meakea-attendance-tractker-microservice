CREATE TABLE leave.leave_type (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    days_allowed NUMERIC(5,2) NOT NULL DEFAULT 0,
    paid BOOLEAN NOT NULL DEFAULT true,
    allow_half_day BOOLEAN NOT NULL DEFAULT true,
    evidence_required BOOLEAN NOT NULL DEFAULT false,
    minimum_notice_days INT NOT NULL DEFAULT 0,
    carry_over_max NUMERIC(5,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_leave_type_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE leave.leave_policy (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    leave_type_code VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    min_notice_days INT NOT NULL DEFAULT 0,
    allow_half_day BOOLEAN NOT NULL DEFAULT true,
    evidence_required BOOLEAN NOT NULL DEFAULT false,
    carry_over_max NUMERIC(5,2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_leave_policy UNIQUE (tenant_id, leave_type_code)
);

CREATE TABLE leave.leave_balance (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    employee_id VARCHAR(64) NOT NULL,
    leave_type_code VARCHAR(64) NOT NULL,
    year INT NOT NULL,
    allocated NUMERIC(5,2) NOT NULL DEFAULT 0,
    used NUMERIC(5,2) NOT NULL DEFAULT 0,
    pending NUMERIC(5,2) NOT NULL DEFAULT 0,
    available NUMERIC(5,2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_leave_balance UNIQUE (tenant_id, employee_id, leave_type_code, year),
    CONSTRAINT chk_leave_balance_allocated_nonneg CHECK (allocated >= 0),
    CONSTRAINT chk_leave_balance_used_nonneg CHECK (used >= 0),
    CONSTRAINT chk_leave_balance_pending_nonneg CHECK (pending >= 0),
    CONSTRAINT chk_leave_balance_available_nonneg CHECK (available >= 0)
);

CREATE TABLE leave.leave_ledger (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    employee_id VARCHAR(64) NOT NULL,
    leave_type_code VARCHAR(64) NOT NULL,
    year INT NOT NULL,
    entry_type VARCHAR(32) NOT NULL,
    amount NUMERIC(5,2) NOT NULL,
    reference_id VARCHAR(64),
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave.leave_request (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    employee_id VARCHAR(64) NOT NULL,
    employee_name VARCHAR(128),
    approver_id VARCHAR(64),
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    portion VARCHAR(32) NOT NULL DEFAULT 'FULL_DAY',
    days NUMERIC(5,2) NOT NULL,
    reason TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 1,
    decision_reason TEXT,
    decision_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_leave_request_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_leave_request_days_pos CHECK (days > 0),
    CONSTRAINT chk_leave_request_portion CHECK (portion IN ('FULL_DAY', 'MORNING', 'AFTERNOON'))
);

CREATE TABLE leave.leave_day (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL REFERENCES leave.leave_request(id) ON DELETE CASCADE,
    employee_id VARCHAR(64) NOT NULL,
    date DATE NOT NULL,
    portion VARCHAR(32) NOT NULL DEFAULT 'FULL_DAY',
    hours NUMERIC(4,2) NOT NULL DEFAULT 8.0,
    CONSTRAINT uq_leave_day UNIQUE (tenant_id, employee_id, date)
);

CREATE TABLE leave.leave_outbox (
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

CREATE TABLE leave.leave_inbox (
    consumer_name VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (consumer_name, tenant_id, event_id)
);

CREATE TABLE leave.command_receipt (
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

ALTER TABLE leave.leave_type ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_type FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_type_tenant_isolation ON leave.leave_type
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE leave.leave_policy ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_policy FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_policy_tenant_isolation ON leave.leave_policy
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE leave.leave_balance ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_balance FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_balance_tenant_isolation ON leave.leave_balance
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE leave.leave_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_ledger FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_ledger_tenant_isolation ON leave.leave_ledger
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE leave.leave_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_request FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_request_tenant_isolation ON leave.leave_request
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE leave.leave_day ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_day FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_day_tenant_isolation ON leave.leave_day
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE leave.leave_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_outbox_tenant_isolation ON leave.leave_outbox
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE leave.leave_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.leave_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_inbox_tenant_isolation ON leave.leave_inbox
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));

ALTER TABLE leave.command_receipt ENABLE ROW LEVEL SECURITY;
ALTER TABLE leave.command_receipt FORCE ROW LEVEL SECURITY;
CREATE POLICY command_receipt_tenant_isolation ON leave.command_receipt
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''))
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), ''));
