ALTER TABLE attendance.daily_recalculation
    ADD COLUMN available_at timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN attempts integer NOT NULL DEFAULT 0 CHECK(attempts>=0);
CREATE INDEX daily_recalculation_available ON attendance.daily_recalculation(tenant_id,available_at,requested_at);
-- Employees with no taps still need tenant discovery for cutoff and recalculation jobs.
INSERT INTO attendance_jobs.tenant_registry SELECT DISTINCT tenant_id FROM attendance.employee_projection ON CONFLICT DO NOTHING;
CREATE TRIGGER attendance_employee_tenant AFTER INSERT OR UPDATE ON attendance.employee_projection
    FOR EACH ROW EXECUTE FUNCTION attendance.register_outbox_tenant();
