ALTER TABLE attendance.attendance_correction ADD COLUMN previous_sessions JSONB;
COMMENT ON COLUMN attendance.attendance_correction.previous_sessions IS
    'Submission-time effective session facts. NULL means unknown legacy basis; [] means verified no matching sessions.';
CREATE FUNCTION attendance.preserve_correction_original_sessions() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.previous_sessions IS DISTINCT FROM OLD.previous_sessions THEN
        RAISE EXCEPTION 'Correction original session snapshot is immutable';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER correction_original_sessions_immutable
    BEFORE UPDATE OF previous_sessions ON attendance.attendance_correction
    FOR EACH ROW EXECUTE FUNCTION attendance.preserve_correction_original_sessions();
