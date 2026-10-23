# Week 02 build guide part 4

> **Navigation:** [Overview](README.md) · [First guide](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Stone 7 — Thursday recovery only

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W02-S29 — Spend Thursday on the earliest failed gate (30–30 min)

**Why:** A and B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CODE-REVIEW.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S28 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Use the buffer on one recorded auth/host/restore failure. Re-run its original command and record the minimal repair or next owner/action. No extra feature, new tool or silent reduction of the security matrix.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git status --short
```

**Expect:** Earliest failure is resolved with evidence or remains the first carried action.

**Common mistake:** Starting new scope while an earlier security or restore failure remains open.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S29` for evidence-only work, or `feat(w02): w02-s29` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S30 — Replay the stale-session client failure (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/JWT-REFRESH.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S29 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Repeat existing client negative fixtures: expired access, reused refresh and network interruption. Verify one login prompt, preserved unsent form data and no unbounded retry; do not add a screen today.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
npm --prefix web run build
```

**Expect:** Existing client tests/build pass or name an exact failed assertion; no stale credential is accepted.

**Common mistake:** Looping refresh retries instead of prompting for login once.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S30` for evidence-only work, or `feat(w02): w02-s30` for the isolated implementation change.
- [ ] Tagged `w02-s30-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Stone 8 — Friday release and restore

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W02-S31 — Redeploy the reviewed host candidate (30–30 min)

**Why:** A and B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/DOCKER-COMPOSE.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S30 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Run the reviewed deploy only when host/image/secrets/backup gates pass. Verify private readiness, login/profile and denied anonymous/foreign tenant requests; execute existing rollback check in scratch if rollout fails. Do not deploy red work.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
bash ops/deploy.sh
```

**Expect:** Candidate reaches bounded readiness and auth smoke passes on intended host; failed rollout returns nonzero with prior release preserved.

**Common mistake:** Deploying an unreviewed image or deleting volumes to repair a rollout.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S31` for evidence-only work, or `feat(w02): w02-s31` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W02-S32 — Repeat restore and decide the weekly gate (30–30 min)

**Why:** A and B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/BACKUPS-RESTORES.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W02-S31 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W02-EVIDENCE.md` `docs/roadmap/YOU-ARE-HERE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Run the reviewed scratch restore again from the release backup, record reports and all required Q-items, map next position, commit the gate evidence and tag only if green. Stop for weekend rest; failed security/restore remains the first next action.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
bash ops/restore-scratch.sh
```

**Expect:** Restore/auth/tenant reports and actual release IDs justify **PASS**, or gate stays **FAIL** with owner/next action; nothing is prechecked.

**Common mistake:** Calling a release green without repeating the scratch restore from its actual backup.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w02): record W02-S32` for evidence-only work, or `feat(w02): w02-s32` for the isolated implementation change.
- [ ] Tagged `w02-s32-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Continue

[Verify the gate](VERIFY-AND-GATE.md).
