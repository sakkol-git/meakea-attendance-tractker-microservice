CREATE TABLE attendance.command_receipt (
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    method VARCHAR(8) NOT NULL,
    path VARCHAR(120) NOT NULL,
    command_key UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, actor_id, method, path, command_key)
);

CREATE TABLE attendance.attendance_evidence (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    client_observation_id UUID,
    recorded_at TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    accuracy_meters DOUBLE PRECISION,
    device_model VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT attendance_evidence_obs_unique UNIQUE (tenant_id, client_observation_id)
);

CREATE TABLE attendance.attendance_session (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    clock_in TIMESTAMPTZ NOT NULL,
    clock_out TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    version BIGINT NOT NULL DEFAULT 0,
    in_evidence_id UUID REFERENCES attendance.attendance_evidence(id),
    out_evidence_id UUID REFERENCES attendance.attendance_evidence(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT attendance_session_clock_order CHECK (clock_out IS NULL OR clock_out >= clock_in)
);

CREATE UNIQUE INDEX attendance_session_one_open_idx
    ON attendance.attendance_session (tenant_id, employee_id)
    WHERE (clock_out IS NULL);

CREATE TABLE attendance.attendance_outbox (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    topic VARCHAR(64) NOT NULL,
    event_id UUID NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_revision BIGINT NOT NULL CHECK (aggregate_revision > 0),
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ
);

CREATE TABLE attendance.attendance_correction (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    session_id UUID REFERENCES attendance.attendance_session(id),
    date DATE NOT NULL,
    proposed_clock_in TIMESTAMPTZ,
    proposed_clock_out TIMESTAMPTZ,
    reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE attendance.command_receipt ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.command_receipt FORCE ROW LEVEL SECURITY;
CREATE POLICY command_receipt_tenant ON attendance.command_receipt
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE attendance.attendance_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.attendance_evidence FORCE ROW LEVEL SECURITY;
CREATE POLICY attendance_evidence_tenant ON attendance.attendance_evidence
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE attendance.attendance_session ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.attendance_session FORCE ROW LEVEL SECURITY;
CREATE POLICY attendance_session_tenant ON attendance.attendance_session
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE attendance.attendance_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.attendance_outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY attendance_outbox_tenant ON attendance.attendance_outbox
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE attendance.attendance_correction ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance.attendance_correction FORCE ROW LEVEL SECURITY;
CREATE POLICY attendance_correction_tenant ON attendance.attendance_correction
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
