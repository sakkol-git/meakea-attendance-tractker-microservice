# Week 02 build guide part two

> **Navigation:** [Previous: Week 01](../01-FOUNDATION/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 03](../03-ORGANIZATION-AND-ADMIN/README.md)

## Day plan

| Day | Work | New concepts | Time cap per person |
| --- | --- | --- | --- |
| Monday | Stone 1, five small steps | JWT and refresh only | 5 h |
| Tuesday | Stone 2, three small steps | Next two if needed | 4 h |
| Wednesday | Stone 3, three small steps | At most two | 3 h |
| Thursday | Stone 4, review and recovery only | None | 2.5 h |
| Friday | Stone 5, gate and redeploy | None | 2.3 h |
| Weekend | Two rest days | None | 0 h |

Totals stay within 16.8 h per contributor; unused time is buffer, not permission for more scope. Each code example is a proposed minimal pattern/contract; inspect first and adapt names to verified files. Run Maven from repository root; run Flutter tests from the confirmed mobile root. A/B step pairing may use reserved buffer, never exceed day cap.

## Stone 4 — Buffer evidence and failure recovery

**You will have:** Known failing reproduction passes or gate is honestly **FAIL**; no unsafe workaround.

### W02-S12 — Inspect the smallest uncertain behavior (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W02-S11 committed with its check passing; tag `w02-s11-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Inspect prior feature steps and write proposed scripts/acceptance/w02.sh as a real scenario runner, not echo **PASS**. Its diagnose branch checks target JDK, Docker, migrations, readiness and fixture IDs without secrets. No new feature scope on Thursday.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w02.sh diagnose
```

**Expect:** One precise failed or healthy check is recorded; script exits nonzero for failed assertions.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `test(week-02): inspect the smallest uncertain behavior`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S13 — B exercises negative and recovery paths (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W02-S12 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use the week-specific matrix in VERIFY-AND-GATE; test malformed input, unauthorized and other-tenant attempts, duplicate commands, stale versions and client restart. Save minimal reproduction; do not add a new screen.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Each executed case has command, actual result and safe report; critical failures remain visible.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `test(week-02): b exercises negative and recovery paths`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S14 — Close only the reviewed gap (45–60 min)

**Why:** A and B makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W02-S13 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use buffer for regression fix and review, not scope. Check migrations/check constraints and unchanged historical data; update OpenAPI examples and dashboard if discrepancy is found. If still blocked, carry exact failing step forward.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Known failing reproduction passes or gate is honestly **FAIL**; no unsafe workaround.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `test(week-02): close only the reviewed gap`.
- [ ] Tagged `w02-s14-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 5 — Friday gate deploy and recovery

**You will have:** Gate decision is justified; final tag exists only if tests pass and app starts; laptop closes for two rest days.

### W02-S15 — Redeploy the checked artifact (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W02-S14 committed with its check passing; tag `w02-s14-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create or inspect proposed ops/deploy.sh before invocation: validate Compose/config, record previous image digest, apply only additive tested migrations, update containers, run authenticated smoke, retain last working manifest. On the confirmed real production host redeploy the exact tested artifact; if host is not ready record **FAIL**, do not substitute a dev demo. Never enable public route before security/backup gate.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/deploy.sh
```

**Expect:** Release commit and image IDs match; app starts and authenticated health/business smoke succeeds; failed deploy retains recovery path.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): redeploy the checked artifact`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S16 — B verifies deployed workflow and restore (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W02-S15 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. From an external test network only after exposure gate, or private LAN while blocked, B exercises this week mini-win. Every Friday run scratch restore of latest external backup, count/checksum known synthetic records; week 1 uses disposable local dump before external host exists.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/restore-scratch.sh
```

**Expect:** Phone/web mini-win and restored synthetic records match; no production volume is overwritten.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): b verifies deployed workflow and restore`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S17 — Decide gate and shut down (45–60 min)

**Why:** A and B makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W02-S16 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. A and B review VERIFY-AND-GATE evidence, require all critical items, and write **PASS**/**CONDITIONAL PASS**/**FAIL** with owner/deadline. Update global checklist/dashboard and next first action. Preserve failed critical item as next week first stone; do not borrow weekends.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
git status --short
```

**Expect:** Gate decision is justified; final tag exists only if tests pass and app starts; laptop closes for two rest days.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `docs(progress): record W02-S17`.
- [ ] Tagged `w02-s17-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Verify the gate](VERIFY-AND-GATE.md).
