# Week 06 build guide

> **Navigation:** [Previous: Week 05](../05-OUTBOX-AND-NOTIFICATIONS/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 07](../07-REQUESTS-AND-APPROVALS/README.md)

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

## Stone 1 — Entitlement and eligible days

**You will have:** Mobile shows server-computed units and cannot submit as another employee.

### W06-S01 — Create type policy balance and ledger (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] Week 05 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. New leave migrations define types/policies/balance/ledger/day expansion; tenant-scoped constraints, positive portions and nonnegative availability.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Clean migration and ledger-to-balance reconciliation test pass.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): create type policy balance and ledger`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W06-S02 — Calculate eligible leave days (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] W06-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use local shift/holiday projections and stored policy version; ignore client days; half-day only one date; reject unsupported cases.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Weekend/holiday/half-day fixtures produce exact expected units.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): calculate eligible leave days`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W06-S03 — B submits from validated mock forms (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Database Locks primer](../primers/DATABASE-LOCKS.md)

**Before you start:**

- [ ] W06-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Display own balance/type, date range and specific validation errors; attachments remain disabled unless secure path is complete.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Mobile shows server-computed units and cannot submit as another employee.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): b submits from validated mock forms`.
- [ ] Tagged `w06-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Reservation and approval

**You will have:** Manager sees exact state and conflict message; no automatic double tap approval.

### W06-S04 — Reserve under deterministic locks (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [State Machines primer](../primers/STATE-MACHINES.md)

**Before you start:**

- [ ] W06-S03 committed with its check passing; tag `w06-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Lock employee then balance rows in fixed order; insert pending request/days/ledger/reservation/outbox/key together.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Two concurrent leave requests cannot overdraw or overlap FULL/AM/PM portions.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): reserve under deterministic locks`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W06-S05 — Approve or reject once (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [State Machines primer](../primers/STATE-MACHINES.md)

**Before you start:**

- [ ] W06-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement assigned direct-report/HR approval with version and no self approval; move pending to used once; rejection releases reservation.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Duplicate approval has one ledger effect; stale or unauthorized decision fails.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): approve or reject once`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W06-S06 — B handles stale manager actions (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [State Machines primer](../primers/STATE-MACHINES.md)

**Before you start:**

- [ ] W06-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add approval inbox and cancellation forms using If-Match and stable keys; on 412 refetch instead of silently retrying different decisions.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Manager sees exact state and conflict message; no automatic double tap approval.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): b handles stale manager actions`.
- [ ] Tagged `w06-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Cancellation and attendance conflict

**You will have:** Phone and React show authoritative local leave state and freshness-labeled attendance.

### W06-S07 — Cancel with compensating ledger (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [State Machines primer](../primers/STATE-MACHINES.md)

**Before you start:**

- [ ] W06-S06 committed with its check passing; tag `w06-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement own pending/future-approved cancellation; write inverse component deltas once and a newer full snapshot.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Available balance recovers once; repeated key does not refund twice.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): cancel with compensating ledger`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W06-S08 — Apply leave overlays and review conflicts (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [State Machines primer](../primers/STATE-MACHINES.md)

**Before you start:**

- [ ] W06-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Local event handler stores request/day/revision overlay; manual-work overlap creates review state and leaves debit pending explicit HR resolution.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Newer cancellation is not reversed by delayed approval/conflict; evidence remains.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): apply leave overlays and review conflicts`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W06-S09 — B verifies full leave flow (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [State Machines primer](../primers/STATE-MACHINES.md)

**Before you start:**

- [ ] W06-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Submit, approve, view day calendar, cancel, and see balance update; record unresolved HR policy as launch blocker.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Phone and React show authoritative local leave state and freshness-labeled attendance.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-06): b verifies full leave flow`.
- [ ] Tagged `w06-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
