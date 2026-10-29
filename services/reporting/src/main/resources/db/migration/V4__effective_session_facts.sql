-- An open session has no completed duration. Retain legacy rows without inventing raw timestamps.
ALTER TABLE reporting.attendance_fact_projection
    ALTER COLUMN minutes_worked DROP NOT NULL,
    ALTER COLUMN minutes_worked DROP DEFAULT,
    ADD COLUMN clock_in timestamptz,
    ADD COLUMN clock_out timestamptz,
    ADD COLUMN timezone varchar(80),
    ADD COLUMN scheduled_start timestamptz,
    ADD COLUMN scheduled_end timestamptz,
    ADD COLUMN grace_minutes integer,
    ADD COLUMN source varchar(20);

ALTER TABLE reporting.attendance_fact_projection ADD CONSTRAINT session_fact_basis CHECK (
    (clock_in IS NULL AND clock_out IS NULL AND timezone IS NULL AND scheduled_start IS NULL
        AND scheduled_end IS NULL AND grace_minutes IS NULL AND source IS NULL)
    OR (clock_in IS NOT NULL AND timezone IS NOT NULL AND scheduled_start IS NOT NULL
        AND scheduled_end IS NOT NULL AND scheduled_end > scheduled_start
        AND grace_minutes IS NOT NULL AND grace_minutes BETWEEN 0 AND 120
        AND source IS NOT NULL AND source IN ('TAP','MANUAL')
        AND (clock_out IS NULL OR clock_out >= clock_in))
);
