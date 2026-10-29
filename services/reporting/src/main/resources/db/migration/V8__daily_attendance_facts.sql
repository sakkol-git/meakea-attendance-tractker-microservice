-- Daily decisions are separate from effective session observations.
CREATE TABLE reporting.daily_fact_projection (
    tenant_id uuid NOT NULL, employee_id uuid NOT NULL, work_date date NOT NULL,
    daily_id uuid NOT NULL, revision bigint NOT NULL CHECK(revision>0), payload jsonb NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(), PRIMARY KEY(tenant_id,employee_id,work_date),
    UNIQUE(tenant_id,daily_id)
);
ALTER TABLE reporting.daily_fact_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE reporting.daily_fact_projection FORCE ROW LEVEL SECURITY;
CREATE POLICY daily_fact_tenant ON reporting.daily_fact_projection
    USING(tenant_id=NULLIF(current_setting('app.tenant_id',true),'')::uuid);
