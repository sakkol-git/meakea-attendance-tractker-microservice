-- Additive migration: add from_date and to_date range filter columns to export_job
ALTER TABLE reporting.export_job
    ADD COLUMN from_date DATE,
    ADD COLUMN to_date DATE;
