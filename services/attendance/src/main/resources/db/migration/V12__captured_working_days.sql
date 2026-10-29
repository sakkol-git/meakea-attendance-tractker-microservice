-- Unknown historical calendars remain unknown; never assign a made-up weekday mask to old sessions.
ALTER TABLE attendance.attendance_session ADD COLUMN working_days integer;
ALTER TABLE attendance.attendance_session ADD CONSTRAINT captured_working_days
    CHECK(working_days IS NULL OR working_days BETWEEN 1 AND 127);
