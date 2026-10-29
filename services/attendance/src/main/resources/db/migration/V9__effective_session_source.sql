-- Do not guess source for legacy sessions. New commands persist their effective interpretation explicitly.
ALTER TABLE attendance.attendance_session ADD COLUMN effective_source varchar(20);
ALTER TABLE attendance.attendance_session ADD CONSTRAINT effective_session_source
    CHECK (effective_source IS NULL OR effective_source IN ('TAP','MANUAL'));
