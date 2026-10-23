# Ten-minute stone self-review

## Before every green stone tag

Use the closing step's last ten minutes. Capture reviewed commit, test report and any exception. Never tag a red-test-only commit or transfer another contributor's partial work into your commit.

- [ ] Names describe business intent; no invented generic framework obscures a simple method.
- [ ] One intent per change, no unused imports/branches/dead code; Spotless and PMD checks pass.
- [ ] Test fails for the intended behavior before change and passes afterward; required negative/race/recovery cases are real assertions.
- [ ] Tenant filter and tenant-leading index/key exist; non-owner RLS and cross-tenant suite pass for each changed endpoint.
- [ ] Role/resource authority and self-approval checks are server-side; DTOs cannot mass-assign tenant/status/roles.
- [ ] Problem-details errors and OpenAPI examples agree; no stack trace or personal data in responses/logs.
- [ ] No secret, token, dump or private employee fixture is staged; secret scan passes.
- [ ] New migration only; applied history unchanged, constraints and exact balance types tested.
- [ ] Outbox/idempotency/audit share required transaction; receipt/offset claims match the local architecture.
- [ ] Deployment smoke, backup/restore gate, docs and ID map are current; app starts before green tag.

## Evidence and undo

Record safe report path, reviewer and commit in the dashboard. When review finds a flaw, keep the branch and fix the failing assertion; never bypass CI or retag an old checkpoint. Use git revert HEAD only after confirming that HEAD is exactly the isolated, un-deployed step commit; database state is not reversed by Git.
