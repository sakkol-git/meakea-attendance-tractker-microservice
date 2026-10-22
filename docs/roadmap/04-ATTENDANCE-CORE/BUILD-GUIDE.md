# Week 04 build guide

> **Navigation:** [Previous: Week 03](../03-ORGANIZATION-AND-ADMIN/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 05](../05-OUTBOX-AND-NOTIFICATIONS/README.md)

## Day plan

| Day | Work | New concepts | Time cap per person |
| --- | --- | --- | --- |
| Monday | Stone 1, three small steps | First two linked primers only | 3.4 h |
| Tuesday | Stone 2, three small steps | Next two if needed | 3.4 h |
| Wednesday | Stone 3, three small steps | At most two | 3.4 h |
| Thursday | Stone 4, review and recovery only | None | 1.53 h |
| Friday | Stone 5, gate and redeploy | None | 2.55 h |
| Weekend | Two rest days | None | 0 h |

Totals stay within 14.28 h per contributor; unused time is buffer, not permission for more scope. Each code example is a proposed minimal pattern/contract; inspect first and adapt names to verified files. Run Maven from repository root; run Flutter tests from the confirmed mobile root. A/B step pairing may use reserved buffer, never exceed day cap.

## Stone 1 — A safe check-in interval

**You will have:** Exactly one session exists after client retry and app restart.

### W04-S01 — Create evidence and session integrity (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] Week 03 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add attendance evidence/session/day-guard migrations; unique client observation and one-open-effective-session constraint; immutable raw evidence.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Duplicate open sessions fail at DB boundary; invalid interval check passes.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): create evidence and session integrity`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W04-S02 — Persist idempotent check-in (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] W04-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement /attendance/check-in with token employee, canonical body/key hash, employee lock, evidence/session and durable publication intent in one transaction.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Same key returns same 201/session; changed body 409; other key while open 409.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): persist idempotent check-in`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W04-S03 — B retries a lost response (30–45 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] W04-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Persist command key before send; simulate server commit then dropped response and retry unchanged body; React shows canonical returned state.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Exactly one session exists after client retry and app restart.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): b retries a lost response`.
- [ ] Tagged `w04-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Checkout and classification

**You will have:** No duplicate tap while pending; offline history does not pretend to be current server state.

### W04-S04 — Close an owned session once (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] W04-S03 committed with its check passing; tag `w04-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement check-out with owner scope and If-Match; checkout before check-in 422; two concurrent checkouts cannot corrupt interval.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** 200 closes once; stale version fails; raw TAP evidence remains.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): close an owned session once`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W04-S05 — Calculate flags with a clear method (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] W04-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement a plain status function: late after start+grace, early flag, holiday/leave conflict review; no strategy/chain refactor during critical gate.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Fixed-clock fixtures prove boundary minute, missing checkout and late+early preservation.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): calculate flags with a clear method`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W04-S06 — B renders own day and history (30–45 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] W04-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement today/me views and error cases from sole contract; display work-site timezone, pending retry and review-required state.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** No duplicate tap while pending; offline history does not pretend to be current server state.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): b renders own day and history`.
- [ ] Tagged `w04-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Corrections without lost history

**You will have:** Rejected correction leaves original effective; approved correction is visible without deleting history.

### W04-S07 — Request a correction (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] W04-S06 committed with its check passing; tag `w04-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add correction request plus immutable action migration; self may propose times with reason but cannot alter existing evidence.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Request is PENDING; original session/evidence values stay unchanged.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): request a correction`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W04-S08 — Approve with independent scope (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] W04-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add manager/HR decision lock and If-Match; append replacement manual revision and daily calculation intent; deny requester self approval.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Approval yields higher effective revision; concurrent approval has one winner.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): approve with independent scope`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W04-S09 — B verifies original and effective records (30–45 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] W04-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Show requested correction and decision status, original versus effective times, and non-editable audited explanation.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Rejected correction leaves original effective; approved correction is visible without deleting history.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-04): b verifies original and effective records`.
- [ ] Tagged `w04-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
