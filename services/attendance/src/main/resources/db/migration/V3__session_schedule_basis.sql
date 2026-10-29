-- Existing sessions retain unknown basis; never backfill guessed shifts or time zones.
ALTER TABLE attendance.attendance_session
    ADD COLUMN work_date date,
    ADD COLUMN timezone varchar(64),
    ADD COLUMN shift_id uuid,
    ADD COLUMN shift_revision bigint,
    ADD COLUMN scheduled_start timestamptz,
    ADD COLUMN scheduled_end timestamptz,
    ADD COLUMN grace_minutes integer;
ALTER TABLE attendance.attendance_session ADD CONSTRAINT session_schedule_basis_complete CHECK (
    (work_date IS NULL AND timezone IS NULL AND shift_id IS NULL AND shift_revision IS NULL
        AND scheduled_start IS NULL AND scheduled_end IS NULL AND grace_minutes IS NULL)
    OR
    (work_date IS NOT NULL AND timezone IS NOT NULL AND shift_id IS NOT NULL AND shift_revision > 0
        AND scheduled_start IS NOT NULL AND scheduled_end > scheduled_start AND grace_minutes BETWEEN 0 AND 120)
);
CREATE INDEX attendance_session_work_date ON attendance.attendance_session(tenant_id, employee_id, work_date);
