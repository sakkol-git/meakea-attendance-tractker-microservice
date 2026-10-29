-- Preserve known original context before any human-approved effective schedule replacement.
-- Legacy rows without captured context stay unknown.
ALTER TABLE attendance.attendance_session ADD COLUMN original_schedule jsonb;
UPDATE attendance.attendance_session SET original_schedule=jsonb_build_object(
    'workDate',work_date::text,'timezone',timezone,'shiftId',shift_id::text,'shiftRevision',shift_revision,
    'scheduledStart',scheduled_start::text,'scheduledEnd',scheduled_end::text,'graceMinutes',grace_minutes)
WHERE work_date IS NOT NULL;
CREATE FUNCTION attendance.preserve_original_schedule() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.original_schedule IS NOT NULL AND NEW.original_schedule IS DISTINCT FROM OLD.original_schedule THEN
        RAISE EXCEPTION 'Original schedule context is immutable';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER preserve_original_schedule BEFORE UPDATE ON attendance.attendance_session
    FOR EACH ROW EXECUTE FUNCTION attendance.preserve_original_schedule();
