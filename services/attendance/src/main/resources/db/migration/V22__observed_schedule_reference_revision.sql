-- Optional provenance for catch-up across an interval observed before it became historical.
-- Existing dated schedules remain valid for their own date; null cannot authorize extrapolation.
ALTER TABLE attendance.daily_schedule ADD COLUMN employee_reference_revision bigint CHECK(employee_reference_revision>0);
