-- Legacy projection rows keep an unknown owner basis until a full owner snapshot arrives.
ALTER TABLE reporting.leave_fact_projection ADD COLUMN portion varchar(20);
ALTER TABLE reporting.leave_fact_projection ADD COLUMN working_dates jsonb;
ALTER TABLE reporting.leave_fact_projection ADD CONSTRAINT verified_leave_basis CHECK (
    (portion IS NULL AND working_dates IS NULL) OR
    (portion IN ('FULL_DAY','MORNING','AFTERNOON') AND working_dates IS NOT NULL
     AND jsonb_typeof(working_dates)='array'));
