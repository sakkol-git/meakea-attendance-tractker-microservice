# Week 12 build guide part 4

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 4 — Remaining domain invariants

**You will have:** Restored device journey verifies exact balances and scoped CSV.

### W12-S13 — Rehearse final failed-deploy recovery — state the failing behavior (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w12.sh`, `docs/validation/W12-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first and repeat the golden slice. Locate the existing assertion or add a missing regression for this existing objective: Use exact release previous manifest; prove expand-compatible migrations and old image rollback; no editing applied Flyway files. Use the current HTTP/JPA/client fixture, fixed clock and synthetic tenant; make the test fail for behavior, not missing imports. Add this behavior to the week runner on its first use.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w12.sh regression
```

**Expect:** An existing implemented behavior may already pass; missing regression must expose a real defect before a repair is claimed. Required result: Failed rollout restores compatible image without editing Flyway history.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Editing an applied Flyway file breaks reproducible upgrade history; add the next unused version.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Write the failing assertion before changing production behavior.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w12): w12-s13` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S14 — Rehearse final failed-deploy recovery — make the narrow behavior pass (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w12.sh`, `docs/validation/W12-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with this delta only: Use exact release previous manifest; prove expand-compatible migrations and old image rollback; no editing applied Flyway files. Run the existing assertion; if it fails, apply only its smallest demonstrated repair and make it green; use existing validation/problem/auth/tenant adapters. If more than three files are needed, take the next smallest adapter first and record the remaining delta, keeping the stone untagged until complete.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w12.sh regression
```

**Expect:** Failed rollout restores compatible image without editing Flyway history.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Editing an applied Flyway file breaks reproducible upgrade history; add the next unused version.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Make the smallest tested change and reuse the shared contract.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w12): w12-s14` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S15 — Rehearse final failed-deploy recovery — prove the denied or failure path (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w12.sh`, `docs/validation/W12-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with the failure delta: Failed rollout restores compatible image without editing Flyway history. Add this one negative/race assertion to the current feature fixture; assert no forbidden row/effect or leaked bytes. Fix only the proven minimal defect and rerun its red/green tests.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w12.sh negative
```

**Expect:** Failed rollout restores compatible image without editing Flyway history.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Editing an applied Flyway file breaks reproducible upgrade history; add the next unused version.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Test denial and rollback as carefully as the happy path.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w12): w12-s15` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S16 — B verifies restored release user flow — state the failing behavior (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `mobile/test/verifiesrestoredreleaseuserflow_test.dart`, `web/test/verifiesrestoredreleaseuserflow.test.ts`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first and repeat the golden slice. Locate the existing assertion or add a missing regression for this existing objective: Log into restored isolated instance, check attendance history/leave balance/material request and authorized CSV. Use the current HTTP/JPA/client fixture, fixed clock and synthetic tenant; make the test fail for behavior, not missing imports. Add this behavior to the week runner on its first use.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run test -- --run
# From the confirmed Flutter root:
flutter test
```

**Expect:** An existing implemented behavior may already pass; missing regression must expose a real defect before a repair is claimed. Required result: Restored device journey verifies exact balances and scoped CSV.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Making a partial export downloadable or trusting an old download grant can leak data.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Write the failing assertion before changing production behavior.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w12): w12-s16` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S17 — B verifies restored release user flow — make the narrow behavior pass (45–45 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `mobile/test/verifiesrestoredreleaseuserflow_test.dart`, `web/test/verifiesrestoredreleaseuserflow.test.ts`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with this delta only: Log into restored isolated instance, check attendance history/leave balance/material request and authorized CSV. Run the existing assertion; if it fails, apply only its smallest demonstrated repair and make it green; use existing validation/problem/auth/tenant adapters. If more than three files are needed, take the next smallest adapter first and record the remaining delta, keeping the stone untagged until complete.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run test -- --run
# From the confirmed Flutter root:
flutter test
```

**Expect:** Restored device journey verifies exact balances and scoped CSV.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Making a partial export downloadable or trusting an old download grant can leak data.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Make the smallest tested change and reuse the shared contract.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w12): w12-s17` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S18 — B verifies restored release user flow — prove the denied or failure path (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `mobile/test/verifiesrestoredreleaseuserflow_test.dart`, `web/test/verifiesrestoredreleaseuserflow.test.ts`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with the failure delta: Restored device journey verifies exact balances and scoped CSV. Add this one negative/race assertion to the current feature fixture; assert no forbidden row/effect or leaked bytes. Fix only the proven minimal defect and rerun its red/green tests.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run test -- --run
# From the confirmed Flutter root:
flutter test
```

**Expect:** Restored device journey verifies exact balances and scoped CSV.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Making a partial export downloadable or trusting an old download grant can leak data.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Test denial and rollback as carefully as the happy path.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w12): w12-s18` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w12-s18-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-5.md).
