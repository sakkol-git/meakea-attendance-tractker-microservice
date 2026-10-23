# Week 01 build guide part 4

> **Navigation:** [Overview](README.md) · [First guide](BUILD-GUIDE.md) · [Dashboard](../YOU-ARE-HERE.md)

## Stone 7 — CI and boundary proof

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W01-S26 — Make CI execute the same checks (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CI-SCANS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S25 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `.github/workflows/verify.yml` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect first: create the proposed workflow only if no equivalent exists. Use Java 21, pinned action revisions and existing Maven/Nuxt commands plus contract/docs checks. Push to the authorized implementation work branch only; no deployment credentials.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
python3 docs/roadmap/tools/docs-lint.py docs
```

**Expect:** Workflow names actual commands; a deliberately failing test blocks CI. Local YAML existence alone is not a CI pass.

**Common mistake:** Writing a workflow file without confirming that a runner executes its checks.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S26` for evidence-only work, or `feat(w01): w01-s26` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S27 — Add a secret-scan failure fixture (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CI-SCANS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S26 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `.github/workflows/verify.yml` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Pin Gitleaks in the existing workflow and verify it detects its documented synthetic test-secret fixture in a temporary test repository. Do not add a real secret, disable scanning or print credentials.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git diff -- .github/workflows/verify.yml
```

**Expect:** Every push has secret scanning; the safe synthetic fixture fails and the real docs/code scan has recorded results.

**Common mistake:** Testing a secret scanner with a real credential.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Record the observed failure before making the smallest repair.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S27` for evidence-only work, or `feat(w01): w01-s27` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S28 — Scan dependencies and the built image (45–45 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CI-SCANS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S27 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `.github/workflows/verify.yml` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Pin Trivy filesystem/dependency and image scans. Add the reviewed build step for the first identity image if its Dockerfile is already created; otherwise record the missing-image blocker and do not mark Q12 complete. Pin base image version/digest from registry evidence.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git diff --stat
```

**Expect:** Workflow cannot report an absent image or failed scan database download as success; unresolved findings are explicit release blockers.

**Common mistake:** Reporting scan database download failure as a successful vulnerability check.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S28` for evidence-only work, or `feat(w01): w01-s28` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S29 — Spike one forbidden service dependency (45–45 min)

**Why:** A owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/ARCHITECTURE-TESTS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S28 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Inspect actual package/module dependencies. Time-box an ArchUnit proof against the nonempty identity test classes; a synthetic infrastructure import from domain must fail. Record proposed rules and exact test command for Week 3, not an empty-package green test.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Finding shows a forbidden edge fails and the clean equivalent passes, or records the exact spike blocker.

**Common mistake:** Running architecture rules against an empty compiled package.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S29` for evidence-only work, or `feat(w01): w01-s29` for the isolated implementation change.
- [ ] Tagged `w01-s29-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Stone 8 — Buffer and Friday evidence

**You will have:** The following named outputs with actual evidence; unfinished output keeps this stone untagged.

### W01-S30 — Use Thursday only to repair one blocker (30–30 min)

**Why:** A and B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CODE-REVIEW.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S29 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Choose the earliest failed prerequisite, reproduce it with its original exact command and record one next action. No new feature, tool or concept today. If repair exceeds the slot, carry it forward and keep the gate failed.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git status --short
```

**Expect:** Named earlier failure has a reproduction/result and owner; no missing quality check is silently deleted.

**Common mistake:** Adding a new feature during the Thursday recovery buffer.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S30` for evidence-only work, or `feat(w01): w01-s30` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S31 — Review the client recovery fixture (30–30 min)

**Why:** B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CONTRACT-TESTS.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S30 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `mobile/test/department_contract_test.dart` `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Re-run existing invalid/timeout fixture from the confirmed mobile root; assert retry preserves entered data and never loops. Record observed UI state with synthetic data. No new screen.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
npm --prefix web run build
```

**Expect:** Existing client build and selected Flutter negative fixture pass; retry is bounded and safe.

**Common mistake:** Retrying indefinitely when a client fixture simulates a timeout.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S31` for evidence-only work, or `feat(w01): w01-s31` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S32 — Self-review the first private release (30–30 min)

**Why:** A and B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/CODE-REVIEW.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S31 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Run the 10-minute shared checklist. Record every current image/config/migration/test ID and blocker. Stage explicit reviewed implementation paths only; never git add dot in a dirty workspace. Do not tag red work.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
git diff --check
```

**Expect:** No whitespace errors; review and test reports identify a reproducible candidate or a failed gate.

**Common mistake:** Creating a green tag before inspecting the exact staged diff and reports.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S32` for evidence-only work, or `feat(w01): w01-s32` for the isolated implementation change.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

### W01-S33 — Redeploy locally and record the gate (30–30 min)

**Why:** A and B owns this one result; it closes the measured architecture or quality gap named by the title.

**Read first (10 min):** [One concept](../primers/BACKUPS-RESTORES.md); count reading against README allowance, not a hidden extra slot.

**Before you start:**

- [ ] W01-S32 has its stated result or an explicit carried blocker; no failed security/restore gate is waived.

**Do:**

1. Inspect first: locate current equivalents before creating these targets: `docs/validation/W01-EVIDENCE.md` `docs/roadmap/YOU-ARE-HERE.md`. At most these three files change; reading dependencies is not permission to edit them all.
1. Redeploy only the reviewed private skeleton on the same confirmed local target; verify readiness/denied business routes and scratch dump restore of the disposable diagnostic DB. If scripts/images/host are not ready record the blocker instead. Decide gate, next exact action and stop for two rest days.
1. Run the check below only after required target/tool exists; record exact exit/output in the weekly evidence file without credentials. A missing target is an environment/blocker result, not a passed business check.

```bash
python3 docs/roadmap/tools/docs-lint.py docs
```

**Expect:** Private readiness, denied unauthenticated business access and scratch restore have actual evidence, or gate remains **FAIL**; no production-readiness claim.

**Common mistake:** Calling a dump file a verified backup without restoring it.

**Undo:** After this isolated un-deployed step commit, `git revert HEAD`; if SQL has been applied, stop and use a new forward migration under the [rollback runbook](../runbooks/ROLLBACK.md) instead of pretending Git undoes data.

**Senior habit:** Use one reviewed commit with evidence for one observable outcome.

**Check:**

- [ ] Observable result and required negative assertion have actual evidence, or this step remains blocked.
- [ ] Committed with message `docs(w01): record W01-S33` for evidence-only work, or `feat(w01): w01-s33` for the isolated implementation change.
- [ ] Tagged `w01-s33-green` only after this stone tests pass and its current private app starts; run the [10-minute review](../CODE-REVIEW-CHECKLIST.md) first. Do not move an old tag.

**If it fails:** Save the exact failing assertion/exit in the evidence file; use [week diagnostics](TROUBLESHOOTING.md). At the time cap stop, preserve work, and use the [stuck protocol](../STUCK-PROTOCOL.md); no new production code in a spike beyond its finding.

**Status:** **Not run**; authoring and source inspection are not execution of this step.

Shutdown: commit reviewed work, record next exact step/blocker and energy in the dashboard, then stop at the planned day cap.

## Continue

[Verify the gate](VERIFY-AND-GATE.md).
