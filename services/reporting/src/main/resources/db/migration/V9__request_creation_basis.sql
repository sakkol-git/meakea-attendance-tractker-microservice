-- Original creation time is unavailable in old fixture-only records; no updated_at backfill.
ALTER TABLE reporting.request_fact_projection ADD COLUMN created_at timestamptz;
CREATE INDEX request_created_range ON reporting.request_fact_projection(tenant_id,created_at,request_id);
