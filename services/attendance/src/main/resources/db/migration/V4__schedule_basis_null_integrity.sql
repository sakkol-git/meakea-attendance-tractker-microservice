-- SQL CHECK accepts UNKNOWN, so nullable comparison operands need explicit completeness guards.
ALTER TABLE attendance.attendance_session ADD CONSTRAINT session_schedule_basis_not_partial CHECK (
    work_date IS NULL OR
    (shift_revision IS NOT NULL AND scheduled_end IS NOT NULL AND grace_minutes IS NOT NULL)
);
