# Week 02 build guide part 8

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 8 — Friday deploy restore and gate

**You will have:** Restore/auth/tenant reports and actual release IDs justify **PASS**, or gate stays **FAIL** with owner/next action; nothing is prechecked.

### W02-S34 — Redeploy the reviewed host candidate (30–30 min)

**Why:** A and B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/DOCKER-COMPOSE.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W02-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Run the reviewed deploy only when host/image/secrets/backup gates pass. Verify private readiness, login/profile and denied anonymous/foreign tenant requests; execute existing rollback check in scratch if rollout fails. Do not deploy red work.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash ops/deploy.sh
```

**Expect:** Candidate reaches bounded readiness and auth smoke passes on intended host; failed rollout returns nonzero with prior release preserved.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Writing into an unmounted external-disk directory can fill local disk and create a false backup.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `docs(w02): w02-s34` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S35 — Repeat restore and decide the weekly gate (30–30 min)

**Why:** A and B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/BACKUPS-RESTORES.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W02-EVIDENCE.md`, `docs/roadmap/YOU-ARE-HERE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Run the reviewed scratch restore again from the release backup, record reports and all required Q-items, map next position, commit the gate evidence and tag only if green. Stop for weekend rest; failed security/restore remains the first next action.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash ops/restore-scratch.sh
```

**Expect:** Restore/auth/tenant reports and actual release IDs justify **PASS**, or gate stays **FAIL** with owner/next action; nothing is prechecked.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** An archive listing is not a restore; verify isolated rows/files and reject production target names.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `docs(w02): w02-s35` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w02-s35-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Verify and gate](VERIFY-AND-GATE.md).
