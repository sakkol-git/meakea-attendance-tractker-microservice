# Week 01 build guide

> **Navigation:** [Previous: Preparation](../00-PREPARATION/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 02](../02-IDENTITY-AND-HOST/README.md)

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

## Stone 1 — A protected workspace

**You will have:** Flutter test passes, the minimal mobile shell starts, and repository location is known.

### W01-S01 — Protect current work and free disk (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] Audit read; no green app tag exists yet.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Review staged/untracked source without staging secrets; record checkpoint plan and request disk cleanup until at least 30 GiB is free.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
git status --short
```

**Expect:** Dirty source remains intact and disk gate is either proven or explicitly blocked.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): protect current work and free disk`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W01-S02 — Select Java 21 and start Docker safely (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W01-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Inspect installed JDKs; install/select 21 using your approved toolchain; only then start Docker Desktop after disk gate.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
java -version
```

**Expect:** Java major 21 and Docker daemon respond; no database ports are public.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): select java 21 and start docker safely`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W01-S03 — B opens the mobile workspace (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Git primer](../primers/GIT.md)

**Before you start:**

- [ ] W01-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Locate existing Flutter source or create proposed mobile on the weekly branch; create one test, launch the minimal mobile shell, and record lockfile/toolchain.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter --version
```

**Expect:** Flutter test passes, the minimal mobile shell starts, and repository location is known.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): b opens the mobile workspace`.
- [ ] Tagged `w01-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — One database-backed service

**You will have:** Compose validates and private web-to-Caddy-to-app-to-DB request succeeds.

### W01-S04 — Create the narrow application (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Dependency Injection primer](../primers/DEPENDENCY-INJECTION.md)

**Before you start:**

- [ ] W01-S03 committed with its check passing; tag `w01-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Inspect attendance scaffold; create proposed services/hrflow-app with Boot 4.1.1, Java 21, web/JPA/Flyway/validation/security/test dependencies and package com.hrflow.app.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** A context test passes under Java 21; old scaffolds remain intact.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): create the narrow application`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W01-S05 — Persist one diagnostic row (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Dependency Injection primer](../primers/DEPENDENCY-INJECTION.md)

**Before you start:**

- [ ] W01-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create V1__foundation.sql for a non-personal diagnostic table; add a scoped diagnostic repository/controller in identityorg; expose only private GET /api/v1/diagnostic/database.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Testcontainers PostgreSQL test proves one committed row and HTTP 200; diagnostic is disabled publicly.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): persist one diagnostic row`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W01-S06 — Start a private Caddy gateway (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Dependency Injection primer](../primers/DEPENDENCY-INJECTION.md)

**Before you start:**

- [ ] W01-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create proposed infra/selfhost/compose.yaml with app, PostgreSQL 16 and Caddy; keep database un-published and edge loopback-only. Add readiness and log limits.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
docker compose -f infra/selfhost/compose.yaml config --quiet
```

**Expect:** Compose validates and private web-to-Caddy-to-app-to-DB request succeeds.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): start a private caddy gateway`.
- [ ] Tagged `w01-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — React and contract-driven Flutter

**You will have:** Flutter test passes and device/emulator shows documented mocked state.

### W01-S07 — Write the first OpenAPI slice (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Docker Compose primer](../primers/DOCKER-COMPOSE.md)

**Before you start:**

- [ ] W01-S06 committed with its check passing; tag `w01-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Inspect old endpoint catalog; create sole proposed contracts/openapi.yaml with diagnostic/auth/check-in/today schemas and error examples; validate before starting local mock.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
npx @stoplight/prism-cli mock contracts/openapi.yaml
```

**Expect:** Mock returns documented JSON and rejects malformed requests; file is the only wire authority.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): write the first openapi slice`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W01-S08 — Replace the welcome view deliberately (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Docker Compose primer](../primers/DOCKER-COMPOSE.md)

**Before you start:**

- [ ] W01-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Preserve Nuxt on checkpoint; scaffold React/TypeScript in web, define dev/build/test scripts, create API client and diagnostic page routed through Caddy.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
npm --prefix web run build
```

**Expect:** React build and a component test pass; page displays DB diagnostic from local app.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): replace the welcome view deliberately`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W01-S09 — B calls the generated mock (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Docker Compose primer](../primers/DOCKER-COMPOSE.md)

**Before you start:**

- [ ] W01-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add mobile API client and a mock-backed diagnostic screen; test success, 401 and network error without requiring backend features.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Flutter test passes and device/emulator shows documented mocked state.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-01): b calls the generated mock`.
- [ ] Tagged `w01-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
