# Week 02 build guide part 6

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 6 — CI and private operational prerequisites

**You will have:** Only reviewed edge binding is allowed; business/DB/broker ports are not public, and unready external access remains disabled.

### W02-S26 — Review secret separation and rotation (30–30 min)

**Why:** A and B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/CI-SCANS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W02-EVIDENCE.md`, `infra/selfhost/compose.yaml`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Separate runtime/migration roles and ignored host environment from Git. Document key identifiers/rotation procedure without values; trial test-key rollover and verify new signature accepted/old key expires as bounded policy. Check the secret scan remains enabled.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
git diff --check
```

**Expect:** Reviewed config contains secret references only; rotation test and scan reports identify accepted/rejected test keys.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Editing an applied Flyway file breaks reproducible upgrade history; add the next unused version.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s26` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S27 — Draft the repeatable deploy procedure (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/DOCKER-COMPOSE.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `ops/deploy.sh`, `docs/validation/W02-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect existing scripts; if absent draft only the proposed checked deploy script: validate config, require backup, record prior image/config IDs, deploy private candidate, wait bounded readiness, return nonzero on failure. No automatic volume deletion or database downgrade.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash -n ops/deploy.sh
```

**Expect:** Shell syntax exits zero; shell review shows fail-fast behavior and explicit target/rollback prerequisites. This does not prove deployment.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Writing into an unmounted external-disk directory can fill local disk and create a false backup.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s27` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S28 — Write the backup with an external-copy check (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/BACKUPS-RESTORES.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `ops/backup.sh`, `docs/validation/W02-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect verified host DB/file/schema inventory. Write proposed backup script using custom pg_dump, protected private-file/config/schema inventory and encrypted external copy with checksum. Add 14-day retention only after successful new copy. Never embed credentials or delete the only backup.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash -n ops/backup.sh
```

**Expect:** Script syntax succeeds; actual backup succeeds only when dump, files, schema/replay metadata and external checksum verification all finish.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Writing into an unmounted external-disk directory can fill local disk and create a false backup.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s28` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S29 — Restore to a uniquely named scratch database (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/BACKUPS-RESTORES.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `ops/restore-scratch.sh`, `docs/validation/W02-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Write/run the proposed scratch restore only after host and backup prerequisites. Refuse production DB/volume names. Restore roles/schema/data/files to an isolated target; compare synthetic tenant counts, login/profile and schema/config inventory. Git rollback does not reverse DB data.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash -n ops/restore-scratch.sh
```

**Expect:** Syntax passes; actual drill records pg_restore success, verified rows/auth smoke and original DB unchanged. Otherwise restore gate stays failed.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** An archive listing is not a restore; verify isolated rows/files and reject production target names.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s29` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S30 — Observe private readiness and backup failure (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/OBSERVABILITY.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W02-EVIDENCE.md`, `infra/selfhost/compose.yaml`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Record health/readiness route and synthetic uptime check from private monitoring only. Trial a missing external backup destination in scratch config and verify nonzero backup/alert. Do not fill real disk or unplug production storage.
1. Include the checksum-verified offline release image archive/source bundle and protected config/model inventory in the existing backup/restore manifest, as defined by the runbook interface; do not assume local images survive a lost host.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
docker compose -f infra/selfhost/compose.yaml config --quiet
```

**Expect:** Readiness is private; synthetic missing-destination failure is visible and cannot produce a successful backup record.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Writing into an unmounted external-disk directory can fill local disk and create a false backup.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s30` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W02-S31 — Verify the public-access prerequisites stay closed (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/REVERSE-PROXY-HTTPS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W02-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect actual listener bindings and ingress plan. Confirm domain/TLS/auth/firewall/secrets/restore proofs before enabling any tunnel. Missing domain/host means private-only and failed external-access prerequisite; it is not an invitation to port-forward a DB.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
docker compose -f infra/selfhost/compose.yaml ps
```

**Expect:** Only reviewed edge binding is allowed; business/DB/broker ports are not public, and unready external access remains disabled.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** An archive listing is not a restore; verify isolated rows/files and reject production target names.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w02): w02-s31` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w02-s31-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-7.md).
