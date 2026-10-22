# Week 03 build guide

> **Navigation:** [Previous: Week 02](../02-IDENTITY-AND-HOST/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 04](../04-ATTENDANCE-CORE/README.md)

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

## Stone 1 — Employees and hierarchy

**You will have:** Profile/admin form succeeds without broadening server authorization.

### W03-S01 — Add employee master migrations (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Repository primer](../primers/REPOSITORY.md)

**Before you start:**

- [ ] Week 02 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create department, position, employee tables and same-tenant composite FKs in new migration; enforce no self manager and test cyclic hierarchy validation.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Two synthetic tenants can have same employee number; invalid tenant FK fails.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): add employee master migrations`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W03-S02 — Add owner-scoped employee APIs (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Repository primer](../primers/REPOSITORY.md)

**Before you start:**

- [ ] W03-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement OpenAPI employees list/create/detail/update with HR scope and If-Match; never bind JPA entities directly.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** HR can create and edit; self reads permitted fields; wrong tenant returns 404.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): add owner-scoped employee apis`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W03-S03 — B renders employee profile (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Repository primer](../primers/REPOSITORY.md)

**Before you start:**

- [ ] W03-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Read mock and real profile; handle no employee link, inactive status and loading states; A uses same contract for narrow React admin form.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Profile/admin form succeeds without broadening server authorization.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): b renders employee profile`.
- [ ] Tagged `w03-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Schedules and calendar

**You will have:** Device/emulator and React show the same work date and grace rule.

### W03-S04 — Add site and shift templates (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Flyway primer](../primers/FLYWAY.md)

**Before you start:**

- [ ] W03-S03 committed with its check passing; tag `w03-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement work-site/shift tables and DTOs; validate timezone/grace/day mask and same-day end > start; no background location collection.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Invalid zones/coordinates/overnight input are rejected by tests.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): add site and shift templates`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W03-S05 — Assign effective shifts and holidays (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Flyway primer](../primers/FLYWAY.md)

**Before you start:**

- [ ] W03-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement employee shift intervals and holiday owner endpoints; lock employee for overlap validation; snapshot assignment revisions for attendance.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Overlapping assignments cannot both commit; removed holiday retains audit.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): assign effective shifts and holidays`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W03-S06 — B displays today schedule (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Flyway primer](../primers/FLYWAY.md)

**Before you start:**

- [ ] W03-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Render assigned shift and site timezone; missing projection means unavailable state, never guessed on-time; A verifies admin creation on React.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Device/emulator and React show the same work date and grace rule.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): b displays today schedule`.
- [ ] Tagged `w03-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Builder only after fixture pain

**You will have:** Before/after tests pass; no new production abstraction or accidental shared state.

### W03-S07 — Build two plain fixture variants (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Builder primer](../primers/BUILDER.md)

**Before you start:**

- [ ] W03-S06 committed with its check passing; tag `w03-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create proposed EmployeeFixtureTest with plain constructors for active/terminated and two shifts; keep real production constructors simple.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Existing domain tests remain green with plain fixture setup.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): build two plain fixture variants`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W03-S08 — Measure repeated fixture setup (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Builder primer](../primers/BUILDER.md)

**Before you start:**

- [ ] W03-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Show duplicated optional setup in at least two tests; record four admission answers in PATTERN-LESSON. If pain is absent, keep plain methods.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** A explains exact duplicated fields and an unchanged test demonstrates behavior.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): measure repeated fixture setup`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W03-S09 — Refactor test fixtures if admitted (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Builder primer](../primers/BUILDER.md)

**Before you start:**

- [ ] W03-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Introduce a test-only Builder with safe defaults and explicit tenant; keep same assertions; B reviews that tenant is never defaulted across fixtures.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Before/after tests pass; no new production abstraction or accidental shared state.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-03): refactor test fixtures if admitted`.
- [ ] Tagged `w03-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
