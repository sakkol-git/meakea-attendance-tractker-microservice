ALTER TABLE attendance.attendance_outbox
    ADD COLUMN stream_position bigint GENERATED ALWAYS AS IDENTITY;
CREATE INDEX attendance_pending_stream ON attendance.attendance_outbox
    (tenant_id,event_key,stream_position) WHERE sent_at IS NULL AND schema_version = 1;
