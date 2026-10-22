# Week 09 build guide

> **Navigation:** [Previous: Week 08](../08-WEB-AND-MOBILE-INTEGRATION/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 10](../10-OPERATIONS-AND-RESTORE/README.md)

## Day plan

| Day | Work | New concepts | Time cap per person |
| --- | --- | --- | --- |
| Monday | Stone 1, three small steps | First two linked primers only | 4 h |
| Tuesday | Stone 2, three small steps | Next two if needed | 4 h |
| Wednesday | Stone 3, three small steps | At most two | 4 h |
| Thursday | Stone 4, review and recovery only | None | 2.5 h |
| Friday | Stone 5, gate and redeploy | None | 2.3 h |
| Weekend | Two rest days | None | 0 h |

Totals stay within 16.8 h per contributor; unused time is buffer, not permission for more scope. Each code example is a proposed minimal pattern/contract; inspect first and adapt names to verified files. Run Maven from repository root; run Flutter tests from the confirmed mobile root. A/B step pairing may use reserved buffer, never exceed day cap.

## Stone 1 — Rebuildable facts and protected audit

**You will have:** No stale report claims instantaneous truth; department/team scope enforced server-side.

### W09-S01 — Project full revised facts (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Specification primer](../primers/SPECIFICATION.md)

**Before you start:**

- [ ] Week 08 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Local outbox handler upserts attendance/leave/balance/request facts only for higher revision; same tenant dimensions, no cross-package SQL join.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Duplicate/older snapshots leave fact unchanged; summary subtracts old contribution.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): project full revised facts`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W09-S02 — Expose freshness and audit safely (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Specification primer](../primers/SPECIFICATION.md)

**Before you start:**

- [ ] W09-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add scoped report endpoints plus owner audit filters; freshness uses pending intent/checkpoint progress, not age of idle business event.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Wrong tenant/count query leaks nothing; quiet queue is not falsely stale.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): expose freshness and audit safely`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W09-S03 — B renders summary and empty states (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Specification primer](../primers/SPECIFICATION.md)

**Before you start:**

- [ ] W09-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Show bounded summaries with asOf/review status; A implements basic React table instead of new chart scope.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** No stale report claims instantaneous truth; department/team scope enforced server-side.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): b renders summary and empty states`.
- [ ] Tagged `w09-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — CSV and storage lifecycle

**You will have:** Requester sees pending/ready/failed/expired without leaked paths.

### W09-S04 — Create observable CSV job (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Observability primer](../primers/OBSERVABILITY.md)

**Before you start:**

- [ ] W09-S03 committed with its check passing; tag `w09-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Persist authorized export job/scope; worker writes private storage through adapter and marks READY only after completed file.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** 202 has status route; failed storage produces FAILED without download URL.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): create observable csv job`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W09-S05 — Authorize downloads and prevent formulas (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Observability primer](../primers/OBSERVABILITY.md)

**Before you start:**

- [ ] W09-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Recheck current HR/requester scope before short-lived download token; escape formula-leading cells and enforce row/date limits.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Revoked HR cannot download an old export; formula fixture safely encoded.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): authorize downloads and prevent formulas`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W09-S06 — B verifies export status (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Observability primer](../primers/OBSERVABILITY.md)

**Before you start:**

- [ ] W09-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Poll job with bounded backoff; React downloads safe file; failed/expired states offer retry as a new explicit command.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Requester sees pending/ready/failed/expired without leaked paths.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): b verifies export status`.
- [ ] Tagged `w09-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Filter specification only after repetition

**You will have:** Before/after result sets and tenant negatives match exactly.

### W09-S07 — Write plain report filters (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Observability primer](../primers/OBSERVABILITY.md)

**Before you start:**

- [ ] W09-S06 committed with its check passing; tag `w09-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Start with simple allowlisted tenant/date/department predicates in two actual report queries; pin tenant predicate outside user filters.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Report tests pass for different scope/date combinations.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): write plain report filters`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W09-S08 — Show duplicated predicate pain (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Observability primer](../primers/OBSERVABILITY.md)

**Before you start:**

- [ ] W09-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Record repetition across attendance and leave summaries; assess admission; a clear method/switch is acceptable if no genuine composition need.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** At least two real combinations and unchanged assertion suite justify or reject abstraction.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): show duplicated predicate pain`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W09-S09 — Refactor filters only if admitted (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Observability primer](../primers/OBSERVABILITY.md)

**Before you start:**

- [ ] W09-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Compose small repository specifications without permitting caller to remove tenant scope; keep old result/authorization tests identical.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Before/after result sets and tenant negatives match exactly.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-09): refactor filters only if admitted`.
- [ ] Tagged `w09-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
