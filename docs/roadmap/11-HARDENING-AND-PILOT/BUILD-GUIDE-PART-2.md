# Week 11 build guide part 2

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 2 — Feature scope and negative paths

**You will have:** Racing approvals, check-in and balances preserve exactly the DB invariants.

### W11-S04 — Race approvals and check-in — state the failing behavior (30–30 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/TESTCONTAINERS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w11.sh`, `docs/validation/W11-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect first and repeat the golden slice. Locate the existing assertion or add a missing regression for this existing objective: Submit parallel check-in, checkout, leave reservations and step decisions with same/different keys and versions. Use the current HTTP/JDBC/client fixture, fixed clock and synthetic tenant; make the test fail for behavior, not missing imports. Add this behavior to the week runner on its first use.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w11.sh regression
```

**Expect:** An existing implemented behavior may already pass; missing regression must expose a real defect before a repair is claimed. Required result: Racing approvals, check-in and balances preserve exactly the DB invariants.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Treating a missing environment/class as the intended failing behavior.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Write the failing assertion before changing production behavior.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w11): w11-s04` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W11-S05 — Race approvals and check-in — make the narrow behavior pass (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/TESTCONTAINERS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w11.sh`, `docs/validation/W11-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with this delta only: Submit parallel check-in, checkout, leave reservations and step decisions with same/different keys and versions. Run the existing assertion; if it fails, apply only its smallest demonstrated repair and make it green; use existing validation/problem/auth/tenant adapters. If more than three files are needed, take the next smallest adapter first and record the remaining delta, keeping the stone untagged until complete.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w11.sh regression
```

**Expect:** Racing approvals, check-in and balances preserve exactly the DB invariants.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Tagging or deploying before this behavior and its negative check pass.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Make the smallest tested change and reuse the shared contract.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w11): w11-s05` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W11-S06 — Race approvals and check-in — prove the denied or failure path (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/TESTCONTAINERS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/w11.sh`, `docs/validation/W11-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Repeat the golden slice with the failure delta: Racing approvals, check-in and balances preserve exactly the DB invariants. Add this one negative/race assertion to the current feature fixture; assert no forbidden row/effect or leaked bytes. Fix only the proven minimal defect and rerun its red/green tests.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/w11.sh negative
```

**Expect:** Racing approvals, check-in and balances preserve exactly the DB invariants.

```text
Red: expected behavior assertion fails; build/setup errors are diagnosed separately.
Green: Failures: 0, Errors: 0; negative assertion prevents the forbidden effect.
```

**Common mistake:** Asserting an error without checking that no forbidden row/effect escaped.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Test denial and rollback as carefully as the happy path.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `test(w11): w11-s06` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w11-s06-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-3.md).
