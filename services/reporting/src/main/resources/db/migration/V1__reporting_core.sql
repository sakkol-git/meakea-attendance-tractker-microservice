-- Reporting Core Schema: projections, inbox, audit_logs, and checkpoint_progress

CREATE TABLE reporting.reporting_inbox (
    consumer_name VARCHAR(100) NOT NULL,
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    topic VARCHAR(120) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_revision BIGINT NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT reporting_inbox_pk PRIMARY KEY (consumer_name, tenant_id, event_id)
);

CREATE TABLE reporting.attendance_fact_projection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    session_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    work_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    minutes_late INT NOT NULL DEFAULT 0,
    minutes_worked INT NOT NULL DEFAULT 0,
    aggregate_revision BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT attendance_fact_unique UNIQUE (tenant_id, session_id)
);

CREATE TABLE reporting.leave_fact_projection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    leave_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    leave_type VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    portion_days NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    status VARCHAR(50) NOT NULL,
    aggregate_revision BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT leave_fact_unique UNIQUE (tenant_id, leave_id)
);

CREATE TABLE reporting.request_fact_projection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    request_id UUID NOT NULL,
    requester_id UUID NOT NULL,
    request_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    aggregate_revision BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT request_fact_unique UNIQUE (tenant_id, request_id)
);

CREATE TABLE reporting.audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    actor_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    details JSONB,
    ip_address VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE reporting.checkpoint_progress (
    tenant_id UUID NOT NULL,
    topic VARCHAR(120) NOT NULL,
    partition_num INT NOT NULL,
    last_offset BIGINT NOT NULL,
    last_processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    pending_intents INT NOT NULL DEFAULT 0,
    CONSTRAINT checkpoint_progress_pk PRIMARY KEY (tenant_id, topic, partition_num)
);

CREATE TABLE reporting.export_job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    requester_id UUID NOT NULL,
    export_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    file_name VARCHAR(255),
    download_url VARCHAR(500),
    row_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX reporting_inbox_lookup_idx ON reporting.reporting_inbox (tenant_id, event_id);
CREATE INDEX attendance_fact_tenant_date_idx ON reporting.attendance_fact_projection (tenant_id, work_date);
CREATE INDEX leave_fact_tenant_status_idx ON reporting.leave_fact_projection (tenant_id, status);
CREATE INDEX request_fact_tenant_status_idx ON reporting.request_fact_projection (tenant_id, status);
CREATE INDEX audit_log_tenant_created_idx ON reporting.audit_log (tenant_id, created_at DESC);
CREATE INDEX audit_log_tenant_resource_idx ON reporting.audit_log (tenant_id, resource_type, created_at DESC);

-- Enable and FORCE Row-Level Security
ALTER TABLE reporting.reporting_inbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.reporting_inbox FORCE ROW LEVEL SECURITY;
CREATE POLICY reporting_inbox_tenant ON reporting.reporting_inbox
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE reporting.attendance_fact_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.attendance_fact_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY attendance_fact_tenant ON reporting.attendance_fact_projection
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE reporting.leave_fact_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.leave_fact_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY leave_fact_tenant ON reporting.leave_fact_projection
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE reporting.request_fact_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.request_fact_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY request_fact_tenant ON reporting.request_fact_projection
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE reporting.audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.audit_log FORCE ROW LEVEL SECURITY;
CREATE POLICY audit_log_tenant ON reporting.audit_log
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE reporting.checkpoint_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.checkpoint_progress FORCE ROW LEVEL SECURITY;
CREATE POLICY checkpoint_progress_tenant ON reporting.checkpoint_progress
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE reporting.export_job ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.export_job FORCE ROW LEVEL SECURITY;
CREATE POLICY export_job_tenant ON reporting.export_job
    FOR ALL USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
