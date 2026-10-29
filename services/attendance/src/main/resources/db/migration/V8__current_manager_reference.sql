-- Existing snapshots lack relationship evidence; wait for the next owner snapshot/replay.
ALTER TABLE attendance.employee_projection ADD COLUMN manager_id uuid;
ALTER TABLE attendance.employee_projection ADD COLUMN manager_known boolean NOT NULL DEFAULT false;
CREATE INDEX employee_current_manager ON attendance.employee_projection(tenant_id,manager_id)
    WHERE manager_known AND status='ACTIVE';
