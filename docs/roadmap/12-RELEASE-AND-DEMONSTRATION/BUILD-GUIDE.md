# Week 12 build guide

> **Navigation:** [Previous: Week 11](../11-HARDENING-AND-PILOT/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Release dashboard](../YOU-ARE-HERE.md)

## Day plan

| Day | Work | New concepts | Time cap per person |
| --- | --- | --- | --- |
| Monday | Stone 1, three small steps | First two linked primers only | 4 h |
| Tuesday | Stone 2, three small steps | Next two if needed | 4 h |
| Wednesday | Stone 3, three small steps | At most two | 4 h |
| Thursday | Stone 4, review and recovery only | None | 1.8 h |
| Friday | Stone 5, gate and redeploy | None | 3 h |
| Weekend | Two rest days | None | 0 h |

Totals stay within 16.8 h per contributor; unused time is buffer, not permission for more scope. Each code example is a proposed minimal pattern/contract; inspect first and adapt names to verified files. Run Maven from repository root; run Flutter tests from the confirmed mobile root. A/B step pairing may use reserved buffer, never exceed day cap.

## Stone 1 — Freeze a reproducible release

**You will have:** Both clients agree on IDs/versions and errors; no mock-only success is counted.

### W12-S01 — Record exact release inputs (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] Week 11 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Freeze approved commit/image digests/config/OpenAPI/migration versions; no dependency upgrade or feature addition; write known limitations.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/selfhost-smoke.sh
```

**Expect:** Anyone can identify exact release and recovery manifest without secrets.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): record exact release inputs`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W12-S02 — Run full acceptance from clean fixtures (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] W12-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create explicit all runner for critical matrix; execute auth/org/attendance/leave/request/inbox/audit/CSV with isolated fixtures and failure cases.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w12.sh all
```

**Expect:** No critical failure is hidden by a conditional pass.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): run full acceptance from clean fixtures`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W12-S03 — B runs final phone and web story (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] W12-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Repeat employee-to-manager story against exact release candidate on real devices and React; preserve failures with precise steps.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Both clients agree on IDs/versions and errors; no mock-only success is counted.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): b runs final phone and web story`.
- [ ] Tagged `w12-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Re-run restoration and rollback

**You will have:** Device confirms recovered business meaning, not merely database container health.

### W12-S04 — Restore release backup cleanly again (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W12-S03 committed with its check passing; tag `w12-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use most recent nightly external copy on clean scratch target, restore DB/private files, verify checksums/counts and suppress live notifications.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/restore-clean.sh
```

**Expect:** Second independent clean restore passes; backup age and media identity recorded.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): restore release backup cleanly again`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W12-S05 — Rehearse final failed-deploy recovery (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W12-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use exact release previous manifest; prove expand-compatible migrations and old image rollback; no editing applied Flyway files.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/rollback-check.sh
```

**Expect:** Safe previous release recovers with new data intact.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): rehearse final failed-deploy recovery`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W12-S06 — B verifies restored release user flow (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W12-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Log into restored isolated instance, check attendance history/leave balance/material request and authorized CSV.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Device confirms recovered business meaning, not merely database container health.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): b verifies restored release user flow`.
- [ ] Tagged `w12-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Tell the truth in the demonstration

**You will have:** Recording/demo narrative matches actual tests; product is not called complete merely because guide exists.

### W12-S07 — Write timed demonstration script (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W12-S06 committed with its check passing; tag `w12-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Record 10-minute sequence: tenant security, check-in retry, stopped worker, leave conflict/cancel, material approval, correction, CSV, restore evidence.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w12.sh happy
```

**Expect:** Each demo action has a verified fixture and expected visible result.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): write timed demonstration script`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W12-S08 — Record limits and support handover (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W12-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Document single-host downtime, power/internet/Cloudflare dependency, Desktop login requirement, backup responsibilities, critical open issues and operator.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/selfhost-smoke.sh
```

**Expect:** Release notes distinguish deployed features, deferred scope and blocked launch.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): record limits and support handover`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W12-S09 — B records and rehearses demo (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W12-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use synthetic names, no secrets or private addresses on recording; rehearse fallback screenshots/evidence if live network fails.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Recording/demo narrative matches actual tests; product is not called complete merely because guide exists.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-12): b records and rehearses demo`.
- [ ] Tagged `w12-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
