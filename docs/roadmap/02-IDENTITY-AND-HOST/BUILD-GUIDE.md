# Week 02 build guide

> **Navigation:** [Previous: Week 01](../01-FOUNDATION/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 03](../03-ORGANIZATION-AND-ADMIN/README.md)

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

## Stone 1 — Credentials and tenant context

**You will have:** Own profile reload works; stale refresh forces login once.

### W02-S01 — Add user and refresh migrations (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Jwt Refresh primer](../primers/JWT-REFRESH.md)

**Before you start:**

- [ ] Week 01 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Inspect foundation migration; add new identity_org user/role/refresh tables and tenant-scoped constraints, never edit V1. Seed two synthetic tenants through test fixtures only.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Clean DB migration and cross-tenant uniqueness tests pass.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): add user and refresh migrations`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S02 — Authenticate and rotate tokens (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Jwt Refresh primer](../primers/JWT-REFRESH.md)

**Before you start:**

- [ ] W02-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Implement existing contract auth/login, refresh/logout; use Spring Security encoders, RS256 signing and hashed refresh families with locking; no plaintext secrets in committed config.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Login yields short JWT; rotated refresh cannot be reused; token-family race test passes.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): authenticate and rotate tokens`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S03 — B stores credentials safely (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Jwt Refresh primer](../primers/JWT-REFRESH.md)

**Before you start:**

- [ ] W02-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Use platform secure storage, one serialized refresh path and logout; add tests for expired access/reused refresh and no token logs.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Own profile reload works; stale refresh forces login once.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): b stores credentials safely`.
- [ ] Tagged `w02-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Isolation before exposure

**You will have:** Both clients reject stale credentials and show safe errors.

### W02-S04 — Enforce token-derived tenant (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Multi Tenancy primer](../primers/MULTI-TENANCY.md)

**Before you start:**

- [ ] W02-S03 committed with its check passing; tag `w02-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add verified issuer/audience/signature checks and a scoped tenant context; strip client tenant/roles headers. Require typed request DTOs without tenantId.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Forged header/body cannot change query scope; invalid JWT returns 401.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): enforce token-derived tenant`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S05 — Test role and resource authorization (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Multi Tenancy primer](../primers/MULTI-TENANCY.md)

**Before you start:**

- [ ] W02-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Add AuthTenantTest covering own resource, other tenant UUID, unrelated manager and self approval; use real PostgreSQL constraints.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
mvn -f services/hrflow-app/pom.xml test
```

**Expect:** Unauthorized reads/mutations fail and target rows are unchanged.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): test role and resource authorization`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S06 — B builds React-compatible auth fixtures (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Multi Tenancy primer](../primers/MULTI-TENANCY.md)

**Before you start:**

- [ ] W02-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Agree token/error examples with A; exercise mock and live auth from Flutter; A checks React memory-token and protected refresh-cookie handling.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Both clients reject stale credentials and show safe errors.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): b builds react-compatible auth fixtures`.
- [ ] Tagged `w02-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — The skeleton reaches the real host

**You will have:** Scratch restore and login/profile smoke pass; source volume remains untouched.

### W02-S07 — Prove hardware and host identity (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] W02-S06 committed with its check passing; tag `w02-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Inspect intended host model/RAM/free disk/network/external disk. User says production setup is not ready; do not equate current laptop with production without confirmation.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
df -h .
```

**Expect:** Host inventory is confirmed or DEP gate remains FAIL with named blocker.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): prove hardware and host identity`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S08 — Deploy privately and add first backup (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] W02-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Read ACCESS-SETUP.md and surface domain/CGNAT/backup blockers; on confirmed host only, build/load same architecture images, use secrets outside Git, start private app; write nightly custom pg_dump and 14-day external-copy cron script with mount check.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
docker compose -f infra/selfhost/compose.yaml up -d
```

**Expect:** Private authenticated smoke succeeds; backup exists on mounted external disk; no public exposure.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): deploy privately and add first backup`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W02-S09 — Restore the first dump in scratch (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] W02-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create proposed ops/restore-scratch.sh using a separate scratch container/database and non-production volume; pg_restore --list then restore and compare synthetic row counts.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/restore-scratch.sh
```

**Expect:** Scratch restore and login/profile smoke pass; source volume remains untouched.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-02): restore the first dump in scratch`.
- [ ] Tagged `w02-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
