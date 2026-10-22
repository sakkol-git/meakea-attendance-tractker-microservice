# Week 05 build guide

> **Navigation:** [Previous: Week 04](../04-ATTENDANCE-CORE/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 06](../06-LEAVE-WORKFLOW/README.md)

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

## Stone 1 — Publication intent is atomic

**You will have:** Both rows commit or neither; no provider latency in response test.

### W05-S01 — Inspect the dual-write boundary (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Transactional Outbox primer](../primers/TRANSACTIONAL-OUTBOX.md)

**Before you start:**

- [ ] Week 04 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Trace week-4 check-in transaction; define JSON eventId/type/tenant/aggregate/revision/correlation envelope and local handler receipt names.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Test fails if domain row can commit without intended outbox row.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): inspect the dual-write boundary`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W05-S02 — Add robust outbox migration (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Transactional Outbox primer](../primers/TRANSACTIONAL-OUTBOX.md)

**Before you start:**

- [ ] W05-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create event intent plus handler receipt/inbox tables, due index and unique tenant/event/handler; keep occurrence and retry metadata separate.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Clean PostgreSQL migration passes; duplicate receipt fails.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): add robust outbox migration`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W05-S03 — Save business and outbox together (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Transactional Outbox primer](../primers/TRANSACTIONAL-OUTBOX.md)

**Before you start:**

- [ ] W05-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use one transaction for check-in and intent; throw before commit and assert neither exists; successful request does not invoke provider.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Both rows commit or neither; no provider latency in response test.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): save business and outbox together`.
- [ ] Tagged `w05-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Replay-safe scheduled worker

**You will have:** No lost intent, one logical inbox item, higher revision retained.

### W05-S04 — Claim bounded local work (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] W05-S03 committed with its check passing; tag `w05-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add scheduled dispatcher selecting aggregate heads with FOR UPDATE SKIP LOCKED; short claim lease, no later revision bypass; do not hold DB locks over provider I/O.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Two workers cannot double-apply the same local handler effect.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): claim bounded local work`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W05-S05 — Commit inbox and handler effects (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] W05-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement notification in-app handler and reporting receipt stub; each commits inbox plus effect atomically; mark event complete after required receipts, retry failures with due time.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Crash after effect commit finds inbox on replay; no duplicate notification.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): commit inbox and handler effects`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W05-S06 — Test failure and retry with PostgreSQL (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] W05-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use Testcontainers PostgreSQL for rollback, expired lease, duplicate delivery and revision 4 then 3; Kafka is disabled, so do not simulate nonexistent offsets.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** No lost intent, one logical inbox item, higher revision retained.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): test failure and retry with postgresql`.
- [ ] Tagged `w05-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Users see durable notifications

**You will have:** Check-in succeeds while worker stopped; one notification appears after resume.

### W05-S07 — Expose own inbox and preferences (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] W05-S06 committed with its check passing; tag `w05-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement notification list/read/unread and preference-set version; scope counts and device identifiers; content is generic and safe.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Tenant B cannot enumerate or read tenant A notifications.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): expose own inbox and preferences`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W05-S08 — B reads and reconnects safely (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] W05-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add inbox screen and monotonic read action; refetch on reconnect. React polls freshness now; SSE is stretch.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Disconnected phone misses no durable inbox records after reconnect.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): b reads and reconnects safely`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W05-S09 — Kill and resume the worker (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] W05-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create current-week acceptance script before invoking; disable dispatcher with configuration, check in, restart worker and compare event/receipt/notification IDs.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w05.sh recovery
```

**Expect:** Check-in succeeds while worker stopped; one notification appears after resume.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-05): kill and resume the worker`.
- [ ] Tagged `w05-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
