# Week 03 build guide

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Working rules

All paths for new code/tests/scripts below are proposed targets, not discovered classes. Inspect current equivalents first and record exact selected migration, controller/DTO/repository and fixture names in the commit body before editing. Existing Application roots and service poms are source evidence. If an action needs an API adapter or migration, use its inspected actual path instead of blindly creating the proposed Service/Repository pair; at most three files change. A larger change becomes a 45-minute finding and the smallest next atomic step, never a hidden multi-hour task.

Read first is inside the stated duration, once per unfamiliar concept; later repetitions reuse the primer. A/B already need basic Java/SQL/Git or Flutter skills: check them in Week 1. Limit first-time learning to two concepts per day. If the skill check reveals more learning, carry the named step forward and flag the schedule; do not borrow Thursday, Friday or weekends for new concepts. A reviews B's Java fixture/operations work in the scheduled pairing allowance, not unpaid extra time.

A red step writes one behavior assertion against the existing golden fixture. Compile/setup failures are not its intended red. A green step changes the smallest real adapter; the next negative step covers the stated denied/failure case. Repeat the [golden slice](../GOLDEN-SLICE.md): shared contract/problem/tenant/CI/observability are reused; the scope and expected case below are the delta. Reuse existing tests rather than build a parallel framework.

Every step ends with a focused commit. Stage only its exact reviewed paths. Put command/exit/report path and a safe output line in the commit body; aggregate into the weekly evidence file during buffer/Friday, avoiding a fourth source file in each commit. Red commits are work-branch checkpoints, never deployable tags. Every stone ends in the ten-minute review and tests/startup before its tag. For a read-only finding before app creation, startup is the current baseline scaffold or an explicit failed prerequisite; never falsely claim a business app starts.

## Stone 1 — First feature red and green

**You will have:** Employee stale If-Match is 412; absent employee is 404; foreign tenant list/count is empty.

### W03-S01 — Add employee master migrations — state the failing behavior (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/REPOSITORY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/test/java/com/hrflow/identityorg/AddEmployeeMasterMigrationsTest.java`, `scripts/acceptance/w03.sh`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first and repeat the golden slice. Write one failing assertion for this existing objective: Create department, position, employee tables and same-tenant composite FKs in new migration; enforce no self manager and test cyclic hierarchy validation. Use the current HTTP/JDBC/client fixture, fixed clock and synthetic tenant; make the test fail for behavior, not missing imports. Add this behavior to the week runner on its first use. The proposed week runner must dispatch real Maven/client assertions and return their exit codes; include diagnose/regression/negative cases, no echo-only pass.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -pl services/identity-org -am test
```

**Expect:** The behavior assertion fails on the documented missing behavior. Target after green: Reject a cross-tenant department/position FK and manager hierarchy cycle.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Treating a missing environment/class as the intended failing behavior.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Write the failing assertion before changing production behavior.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w03): w03-s01` for implementation or `docs(w03): w03-s01` for findings/evidence, with exact reviewed paths only.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W03-S02 — Add employee master migrations — make the narrow behavior pass (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/REPOSITORY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/main/java/com/hrflow/identityorg/application/AddEmployeeMasterMigrationsService.java`, `services/identity-org/src/main/java/com/hrflow/identityorg/infrastructure/AddEmployeeMasterMigrationsRepository.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with this delta only: Create department, position, employee tables and same-tenant composite FKs in new migration; enforce no self manager and test cyclic hierarchy validation. Make the preceding behavior assertion green; use existing validation/problem/auth/tenant adapters. If more than three files are needed, take the next smallest adapter first and record the remaining delta, keeping the stone untagged until complete.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -pl services/identity-org -am test
```

**Expect:** Reject a cross-tenant department/position FK and manager hierarchy cycle.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Tagging or deploying before this behavior and its negative check pass.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Make the smallest tested change and reuse the shared contract.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w03): w03-s02` for implementation or `docs(w03): w03-s02` for findings/evidence, with exact reviewed paths only.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W03-S03 — Add employee master migrations — prove the denied or failure path (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/REPOSITORY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/test/java/com/hrflow/identityorg/AddEmployeeMasterMigrationsTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with the failure delta: Reject a cross-tenant department/position FK and manager hierarchy cycle. Add this one negative/race assertion to the current feature fixture; assert no forbidden row/effect or leaked bytes. Fix only the proven minimal defect and rerun its red/green tests.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -pl services/identity-org -am test
```

**Expect:** Reject a cross-tenant department/position FK and manager hierarchy cycle.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Asserting an error without checking that no forbidden row/effect escaped.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Test denial and rollback as carefully as the happy path.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w03): w03-s03` for implementation or `docs(w03): w03-s03` for findings/evidence, with exact reviewed paths only.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W03-S04 — Create the department golden write (60–60 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/SPRING-JDBC.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/main/java/com/hrflow/identityorg/application/GoldenDepartmentService.java`, `services/identity-org/src/main/java/com/hrflow/identityorg/infrastructure/GoldenDepartmentRepository.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice; required quality delta: Repeat the golden slice: add the typed department create using confirmed department migration, tenant-scoped JDBC, method HR check, idempotency result/audit/intent transaction and problem adapter. Keep this name-only endpoint smaller than employee lifecycle.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -pl services/identity-org -am test
```

**Expect:** 201/Location and duplicate-key behavior match the sole contract; blank input and foreign tenant fail.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Calling a command invocation proof without inspecting the recorded assertion/output.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w03): w03-s04` for implementation or `docs(w03): w03-s04` for findings/evidence, with exact reviewed paths only.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W03-S05 — Add owner-scoped employee APIs — state the failing behavior (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/REPOSITORY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/src/test/java/com/hrflow/identityorg/AddOwnerscopedEmployeeApisTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first and repeat the golden slice. Write one failing assertion for this existing objective: Implement OpenAPI employees list/create/detail/update with HR scope and If-Match; never bind persistence objects directly. Use the current HTTP/JDBC/client fixture, fixed clock and synthetic tenant; make the test fail for behavior, not missing imports. Add this behavior to the week runner on its first use.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -pl services/identity-org -am test
```

**Expect:** The behavior assertion fails on the documented missing behavior. Target after green: Employee stale If-Match is 412; absent employee is 404; foreign tenant list/count is empty.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Treating a missing environment/class as the intended failing behavior.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Write the failing assertion before changing production behavior.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w03): w03-s05` for implementation or `docs(w03): w03-s05` for findings/evidence, with exact reviewed paths only.
- [ ] Tagged `w03-s05-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-2.md).
