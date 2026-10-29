ALTER TABLE request.approval_step ALTER COLUMN approver_id DROP NOT NULL;
ALTER TABLE request.approval_step DROP CONSTRAINT chk_step_status;
ALTER TABLE request.approval_step ADD CONSTRAINT chk_step_status
    CHECK(status IN ('PENDING','APPROVED','REJECTED','SKIPPED'));
ALTER TABLE request.approval_step ADD COLUMN requester_revision bigint CHECK(requester_revision>0);
ALTER TABLE request.approval_step ADD COLUMN approver_revision bigint CHECK(approver_revision>0);
ALTER TABLE request.approval_step ADD CONSTRAINT request_role_or_assigned_step
    CHECK(approver_id IS NOT NULL OR approver_role='HR_ADMIN' OR (approver_role='MANAGER' AND status='SKIPPED'));
