# Week 11 build guide

> **Navigation:** [Previous: Week 10](../10-OPERATIONS-AND-RESTORE/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 12](../12-RELEASE-AND-DEMONSTRATION/README.md)

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

## Stone 1 — Adversarial authorization

**You will have:** No token leak or stale identity silently regains scope.

### W11-S01 — Run every two-tenant route case (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] Week 10 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use synthetic A/B fixtures and enumerate implemented OpenAPI routes, count/list/object/file/export/job checks; try forged body/header IDs.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w11.sh cross-tenant
```

**Expect:** No cross-tenant data or mutation; failures have stable safe codes.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): run every two-tenant route case`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W11-S02 — Race approvals and check-in (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] W11-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Submit parallel check-in, checkout, leave reservations and step decisions with same/different keys and versions.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w11.sh concurrency
```

**Expect:** Domain constraints/ledger prove one intended effect; no negative availability.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): race approvals and check-in`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W11-S03 — B tests device credential boundaries (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Testcontainers primer](../primers/TESTCONTAINERS.md)

**Before you start:**

- [ ] W11-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Exercise secure storage/log redaction, refresh reuse, logout, terminated user and app reinstall with stale installation ID.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** No token leak or stale identity silently regains scope.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): b tests device credential boundaries`.
- [ ] Tagged `w11-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Recovery and load evidence

**You will have:** Core manual workflows recover and battery/permission limits are disclosed.

### W11-S04 — Replay duplicate and older local events (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] W11-S03 committed with its check passing; tag `w11-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Kill dispatcher before/after commit, expire lease, replay revision 4 then 3, quarantine malformed JSON and repair safely.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w11.sh recovery
```

**Expect:** No lost intent/repeated logical notification; blocked event remains observable.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): replay duplicate and older local events`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W11-S05 — Measure 100-client attendance load (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] W11-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create bounded load runner against isolated data; warm up, record hardware/count/errors/p50/p95/p99 and commit-to-projection duration; include duplicates.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w11.sh load
```

**Expect:** Results are measured, not assumed; target p95 under 150 ms is pass/fail with evidence.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): measure 100-client attendance load`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W11-S06 — B tests network and device acceptance (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] W11-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Two representative devices exercise poor network and permission denial; log background limitations without promising notification timing.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Core manual workflows recover and battery/permission limits are disclosed.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): b tests network and device acceptance`.
- [ ] Tagged `w11-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Pilot only behind release controls

**You will have:** Feedback is safe, bounded and tied to existing checklist IDs.

### W11-S07 — Review unresolved policy and incidents (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] W11-S06 committed with its check passing; tag `w11-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Confirm tenant HR approval of calendar/conflict/correction/retention rules, external TLS, firewall, secrets and recent restore before consenting pilot.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/selfhost-smoke.sh
```

**Expect:** Any critical unknown blocks real users; synthetic demo remains possible.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): review unresolved policy and incidents`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W11-S08 — Fix the smallest failing reproduction (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] W11-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Pick highest-severity proven defect, add failing regression, apply minimal fix; no new patterns or feature expansions in hardening.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Regression flips green and broader required checks stay green.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): fix the smallest failing reproduction`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W11-S09 — B rehearses limited pilot story (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Idempotency primer](../primers/IDEMPOTENCY.md)

**Before you start:**

- [ ] W11-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use 5 to 10 consenting users only if control gate passed; otherwise synthetic fixtures; record defects/energy and freeze scope.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Feedback is safe, bounded and tied to existing checklist IDs.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-11): b rehearses limited pilot story`.
- [ ] Tagged `w11-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
