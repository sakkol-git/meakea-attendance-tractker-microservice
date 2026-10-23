# Week 12 build guide part 3

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 3 — Client and integration deltas

**You will have:** Corrupt/wrong-target restore fails safely; valid isolated release restore matches all checks.

### W12-S07 — B runs final phone and web story — state the failing behavior (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/BACKUPS-RESTORES.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `mobile/test/runsfinalphonewebstory_test.dart`, `web/test/runsfinalphonewebstory.test.ts`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first and repeat the golden slice. Locate the existing assertion or add a missing regression for this existing objective: Repeat employee-to-manager story against exact release candidate on real devices and Nuxt; preserve failures with precise steps. Use the current HTTP/JDBC/client fixture, fixed clock and synthetic tenant; make the test fail for behavior, not missing imports. Add this behavior to the week runner on its first use.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run test -- --run
# From the confirmed Flutter root:
flutter test
```

**Expect:** An existing implemented behavior may already pass; missing regression must expose a real defect before a repair is claimed. Required result: Any client happy/negative mismatch blocks release, not hidden in demo script.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Treating a successful production build as proof that client failure fixtures executed hides missing tests.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Write the failing assertion before changing production behavior.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w12): w12-s07` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S08 — B runs final phone and web story — make the narrow behavior pass (45–45 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/BACKUPS-RESTORES.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `mobile/test/runsfinalphonewebstory_test.dart`, `web/test/runsfinalphonewebstory.test.ts`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with this delta only: Repeat employee-to-manager story against exact release candidate on real devices and Nuxt; preserve failures with precise steps. Run the existing assertion; if it fails, apply only its smallest demonstrated repair and make it green; use existing validation/problem/auth/tenant adapters. If more than three files are needed, take the next smallest adapter first and record the remaining delta, keeping the stone untagged until complete.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run test -- --run
# From the confirmed Flutter root:
flutter test
```

**Expect:** Any client happy/negative mismatch blocks release, not hidden in demo script.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Treating a successful production build as proof that client failure fixtures executed hides missing tests.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Make the smallest tested change and reuse the shared contract.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w12): w12-s08` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S09 — B runs final phone and web story — prove the denied or failure path (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/BACKUPS-RESTORES.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `mobile/test/runsfinalphonewebstory_test.dart`, `web/test/runsfinalphonewebstory.test.ts`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with the failure delta: Any client happy/negative mismatch blocks release, not hidden in demo script. Add this one negative/race assertion to the current feature fixture; assert no forbidden row/effect or leaked bytes. Fix only the proven minimal defect and rerun its red/green tests.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
npm --prefix web run test -- --run
# From the confirmed Flutter root:
flutter test
```

**Expect:** Any client happy/negative mismatch blocks release, not hidden in demo script.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Treating a successful production build as proof that client failure fixtures executed hides missing tests.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Test denial and rollback as carefully as the happy path.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w12): w12-s09` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S10 — Restore release backup cleanly again — state the failing behavior (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w12.sh`, `docs/validation/W12-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first and repeat the golden slice. Locate the existing assertion or add a missing regression for this existing objective: Use most recent nightly external copy on clean scratch target, restore DB/private files, verify checksums/counts and suppress live notifications. Use the current HTTP/JDBC/client fixture, fixed clock and synthetic tenant; make the test fail for behavior, not missing imports. Add this behavior to the week runner on its first use.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w12.sh regression
```

**Expect:** An existing implemented behavior may already pass; missing regression must expose a real defect before a repair is claimed. Required result: Corrupt/wrong-target restore fails safely; valid isolated release restore matches all checks.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** An archive listing is not a restore; verify isolated rows/files and reject production target names.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Write the failing assertion before changing production behavior.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w12): w12-s10` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S11 — Restore release backup cleanly again — make the narrow behavior pass (45–45 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w12.sh`, `docs/validation/W12-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with this delta only: Use most recent nightly external copy on clean scratch target, restore DB/private files, verify checksums/counts and suppress live notifications. Run the existing assertion; if it fails, apply only its smallest demonstrated repair and make it green; use existing validation/problem/auth/tenant adapters. If more than three files are needed, take the next smallest adapter first and record the remaining delta, keeping the stone untagged until complete.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w12.sh regression
```

**Expect:** Corrupt/wrong-target restore fails safely; valid isolated release restore matches all checks.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** An archive listing is not a restore; verify isolated rows/files and reject production target names.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Make the smallest tested change and reuse the shared contract.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w12): w12-s11` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W12-S12 — Restore release backup cleanly again — prove the denied or failure path (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/GIT.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w12.sh`, `docs/validation/W12-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with the failure delta: Corrupt/wrong-target restore fails safely; valid isolated release restore matches all checks. Add this one negative/race assertion to the current feature fixture; assert no forbidden row/effect or leaked bytes. Fix only the proven minimal defect and rerun its red/green tests.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w12.sh negative
```

**Expect:** Corrupt/wrong-target restore fails safely; valid isolated release restore matches all checks.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** An archive listing is not a restore; verify isolated rows/files and reject production target names.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Test denial and rollback as carefully as the happy path.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w12): w12-s12` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w12-s12-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-4.md).
