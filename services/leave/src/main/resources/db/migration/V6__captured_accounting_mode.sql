-- Historical requests remain unknown until their actual reservation/ledger basis is reviewed.
ALTER TABLE leave.leave_request ADD COLUMN paid_charge boolean;
ALTER TABLE leave.leave_day ADD CONSTRAINT leave_day_portion CHECK(portion IN ('FULL_DAY','MORNING','AFTERNOON'));
