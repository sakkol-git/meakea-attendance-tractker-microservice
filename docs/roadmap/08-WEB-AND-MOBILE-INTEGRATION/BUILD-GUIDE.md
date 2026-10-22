# Week 08 build guide

> **Navigation:** [Previous: Week 07](../07-REQUESTS-AND-APPROVALS/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 09](../09-REPORTING-AUDIT-OBSERVABILITY/README.md)

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

## Stone 1 — One wire contract for both clients

**You will have:** Live phone and React show same resources and recover after API errors.

### W08-S01 — Compare OpenAPI and live responses (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Openapi primer](../primers/OPENAPI.md)

**Before you start:**

- [ ] Week 07 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Validate all implemented routes against sole OpenAPI; remove stale mock shapes; retain stable errors/ETags/date and body schemas.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Recorded contract examples match live auth/attendance/leave/request responses.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): compare openapi and live responses`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W08-S02 — Connect React feature navigation (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Openapi primer](../primers/OPENAPI.md)

**Before you start:**

- [ ] W08-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use feature folders/API client, role navigation, bounded pagination and explicit loading/empty/stale/permission states; backend still enforces scope.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
npm --prefix web run build
```

**Expect:** React tests/build pass and no Nuxt welcome dependency remains in target build.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): connect react feature navigation`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W08-S03 — Connect Flutter to staged API (30–45 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Openapi primer](../primers/OPENAPI.md)

**Before you start:**

- [ ] W08-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use deployment base URL outside source secrets; retain local mock for error tests; run core flows on emulator/device.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Live phone and React show same resources and recover after API errors.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): connect flutter to staged api`.
- [ ] Tagged `w08-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Offline and authentication edges

**You will have:** Same persisted resource appears once; stale dashboards are labeled.

### W08-S04 — Test refresh concurrency and revocation (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Offline Queue primer](../primers/OFFLINE-QUEUE.md)

**Before you start:**

- [ ] W08-S03 committed with its check passing; tag `w08-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Execute parallel refresh/old-token tests plus logout and employee termination window; document short access expiry rather than promise immediate revocation.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Family reuse revoked, no credentials logged, inactive account cannot renew.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): test refresh concurrency and revocation`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W08-S05 — B tests queued command durability (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Offline Queue primer](../primers/OFFLINE-QUEUE.md)

**Before you start:**

- [ ] W08-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Restart app offline, regain connection, send stable key once; 409/412 goes to review and 429 respects Retry-After; never overwrite edited body under old key.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** No duplicated attendance or approvals after interrupted client restart.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): b tests queued command durability`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W08-S06 — React handles timeout and stale state (30–45 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Offline Queue primer](../primers/OFFLINE-QUEUE.md)

**Before you start:**

- [ ] W08-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Test response lost after commit, refetch canonical state and allow explicit retry with same key; do not optimistic-update balances as final.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
npm --prefix web run build
```

**Expect:** Same persisted resource appears once; stale dashboards are labeled.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): react handles timeout and stale state`.
- [ ] Tagged `w08-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Usability without new scope

**You will have:** Both clients observe same committed versions; no contract forks.

### W08-S07 — Review keyboard and form errors (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Offline Queue primer](../primers/OFFLINE-QUEUE.md)

**Before you start:**

- [ ] W08-S06 committed with its check passing; tag `w08-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. A checks tab order/focus/error labels and date timezone display for each core form; fix only verified access barriers.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
npm --prefix web run build
```

**Expect:** Keyboard-only employee can complete check-in and submit request.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): review keyboard and form errors`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W08-S08 — B reviews permissions and phone limits (30–45 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Offline Queue primer](../primers/OFFLINE-QUEUE.md)

**Before you start:**

- [ ] W08-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Test denied biometrics/push/location and foreground/background network behavior; manual authenticated check-in remains usable without tracking.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** No permission denial blocks required attendance flow unexpectedly.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): b reviews permissions and phone limits`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W08-S09 — Run one cross-client acceptance story (30–45 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Offline Queue primer](../primers/OFFLINE-QUEUE.md)

**Before you start:**

- [ ] W08-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create current acceptance script and fixture setup; one employee logs in/checks in/submits leave/request and manager decides from other client.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash scripts/acceptance/w08.sh happy
```

**Expect:** Both clients observe same committed versions; no contract forks.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-08): run one cross-client acceptance story`.
- [ ] Tagged `w08-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
