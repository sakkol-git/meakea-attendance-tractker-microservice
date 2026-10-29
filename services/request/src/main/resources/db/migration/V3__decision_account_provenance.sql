-- Historical decisions have unknown account provenance; never infer it from employee IDs.
ALTER TABLE request.approval_step ADD COLUMN decided_by_account varchar(64);
ALTER TABLE request.approval_step ADD CONSTRAINT request_decision_actor_nonblank
    CHECK(decided_by_account IS NULL OR length(trim(decided_by_account))>0);
