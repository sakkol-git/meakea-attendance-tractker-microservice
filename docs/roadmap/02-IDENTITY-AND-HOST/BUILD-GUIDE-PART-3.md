# Week 02 build guide part 3

> **Navigation:** [Overview](README.md) · [First guide](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Stone 5 — Confirmed private host procedure

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W02-S21 — Record the intended production computer (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DOCKER-COMPOSE.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S20 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Record OS/architecture, available RAM/disk, continuous uptime/power policy, operator access, backup destination and public/private ingress intent. Missing hardware is a blocker, not permission to substitute the development Mac without agreement.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
df -h .
```

**Expect:** Named host/resource evidence exists or deployment remains BLOCKED; no cloud machine is substituted.

**Common mistake:** Calling the development Mac the production host without confirming that choice.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S21` for evidence-only work, or `feat(w02): w02-s21` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S22 — Validate the private service manifest (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DOCKER-COMPOSE.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S21 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `infra/selfhost/compose.yaml` `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect existing dependency Compose and the final workload inventory. Create/review proposed selfhost manifest without secrets or published business/DB/broker ports; pin image versions/digests from actual builds and add bounded logs/health/restart. Record missing workload images explicitly.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
docker compose -f infra/selfhost/compose.yaml config --quiet
```

**Expect:** Manifest syntax succeeds, service port inventory is private, and missing image/runtime proofs remain visible.

**Common mistake:** Publishing database or business-service ports to the public host interface.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S22` for evidence-only work, or `feat(w02): w02-s22` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S23 — Review secret separation and rotation (30–30 min)

**Why:** A and B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CI-SCANS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S22 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md` `infra/selfhost/compose.yaml`. At most these three files change; reading dependencies is not permission to edit them all.
1. Separate runtime/migration roles and ignored host environment from Git. Document key identifiers/rotation procedure without values; trial test-key rollover and verify new signature accepted/old key expires as bounded policy. Check the secret scan remains enabled.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git diff --check
```

**Expect:** Reviewed config contains secret references only; rotation test and scan reports identify accepted/rejected test keys.

**Common mistake:** Writing secret values into the rotation documentation.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S23` for evidence-only work, or `feat(w02): w02-s23` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S24 — Draft the repeatable deploy procedure (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DOCKER-COMPOSE.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S23 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `ops/deploy.sh` `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect existing scripts; if absent draft only the proposed checked deploy script: validate config, require backup, record prior image/config IDs, deploy private candidate, wait bounded readiness, return nonzero on failure. No automatic volume deletion or database downgrade.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
bash -n ops/deploy.sh
```

**Expect:** Shell syntax exits zero; shell review shows fail-fast behavior and explicit target/rollback prerequisites. This does not prove deployment.

**Common mistake:** Calling shell syntax validation a successful deployment.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S24` for evidence-only work, or `feat(w02): w02-s24` for the isolated implementation change.
- [ ] Tagged `w02-s24-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Stone 6 — Backup restore and access prerequisites

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W02-S25 — Write the backup with an external-copy check (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/BACKUPS-RESTORES.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S24 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `ops/backup.sh` `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect verified host DB/file/schema inventory. Write proposed backup script using custom pg_dump, protected private-file/config/schema inventory and encrypted external copy with checksum. Add 14-day retention only after successful new copy. Never embed credentials or delete the only backup.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
bash -n ops/backup.sh
```

**Expect:** Script syntax succeeds; actual backup succeeds only when dump, files, schema/replay metadata and external checksum verification all finish.

**Common mistake:** Deleting the previous backup before a new external copy verifies.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S25` for evidence-only work, or `feat(w02): w02-s25` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S26 — Restore to a uniquely named scratch database (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/BACKUPS-RESTORES.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S25 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `ops/restore-scratch.sh` `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Write/run the proposed scratch restore only after host and backup prerequisites. Refuse production DB/volume names. Restore roles/schema/data/files to an isolated target; compare synthetic tenant counts, login/profile and schema/config inventory. Git rollback does not reverse DB data.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
bash -n ops/restore-scratch.sh
```

**Expect:** Syntax passes; actual drill records pg_restore success, verified rows/auth smoke and original DB unchanged. Otherwise restore gate stays failed.

**Common mistake:** Restoring into the source database or confusing syntax success with a drill.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S26` for evidence-only work, or `feat(w02): w02-s26` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S27 — Observe private readiness and backup failure (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/OBSERVABILITY.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S26 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md` `infra/selfhost/compose.yaml`. At most these three files change; reading dependencies is not permission to edit them all.
1. Record health/readiness route and synthetic uptime check from private monitoring only. Trial a missing external backup destination in scratch config and verify nonzero backup/alert. Do not fill real disk or unplug production storage.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
docker compose -f infra/selfhost/compose.yaml config --quiet
```

**Expect:** Readiness is private; synthetic missing-destination failure is visible and cannot produce a successful backup record.

**Common mistake:** Giving a failed backup an all-clear uptime result.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S27` for evidence-only work, or `feat(w02): w02-s27` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S28 — Verify the public-access prerequisites stay closed (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/REVERSE-PROXY-HTTPS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S27 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect actual listener bindings and ingress plan. Confirm domain/TLS/auth/firewall/secrets/restore proofs before enabling any tunnel. Missing domain/host means private-only and failed external-access prerequisite; it is not an invitation to port-forward a DB.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
docker compose -f infra/selfhost/compose.yaml ps
```

**Expect:** Only reviewed edge binding is allowed; business/DB/broker ports are not public, and unready external access remains disabled.

**Common mistake:** Opening a tunnel before TLS/auth/firewall/secrets/restore proofs exist.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S28` for evidence-only work, or `feat(w02): w02-s28` for the isolated implementation change.
- [ ] Tagged `w02-s28-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Continue

[Next build part](BUILD-GUIDE-PART-4.md).
