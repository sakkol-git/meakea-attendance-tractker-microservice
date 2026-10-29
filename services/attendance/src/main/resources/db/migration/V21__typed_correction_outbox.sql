-- Legacy schema_version=0 correction payloads remain preserved for explicit reconciliation, never relabelled as accepted wire.
ALTER TABLE attendance.attendance_outbox DROP CONSTRAINT typed_attendance_envelope;
ALTER TABLE attendance.attendance_outbox ADD CONSTRAINT typed_attendance_envelope CHECK (
    schema_version=0 OR (schema_version=1
      AND topic IN ('attendance.session.v1','attendance.daily-status.v1','time.shift-cutoff.v1','attendance.correction.v1')
      AND event_type IS NOT NULL AND correlation_id IS NOT NULL AND occurred_at IS NOT NULL AND event_key IS NOT NULL)
);
