# Week 10 build guide

> **Navigation:** [Previous: Week 09](../09-REPORTING-AUDIT-OBSERVABILITY/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 11](../11-HARDENING-AND-PILOT/README.md)

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

## Stone 1 — Boot and rollback are proven

**You will have:** New tokens work, expired keys fail, no secret appears in logs or Git.

### W10-S01 — Reboot the confirmed host safely (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] Week 09 critical gate is green or its carried failure replaces this action.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create read-only startup verification script; with synthetic data/verified backup schedule controlled reboot and prove Docker starts after documented login/unlock prerequisites.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/host-reboot-check.sh
```

**Expect:** App/PostgreSQL/Caddy/Kuma recover; unattended limitation is explicit if Desktop needs login.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): reboot the confirmed host safely`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W10-S02 — Roll back a failed deploy (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] W10-S01 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create release manifest of image/config/migration IDs; deploy synthetic failing candidate privately, restore last compatible image without downgrading applied migration.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/rollback-check.sh
```

**Expect:** Known previous version serves smoke; database data unchanged; no down -v.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): roll back a failed deploy`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W10-S03 — Rotate secrets without locking users out (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Backups Restores primer](../primers/BACKUPS-RESTORES.md)

**Before you start:**

- [ ] W10-S02 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create verification script and run staging rotation of JWT keys/tunnel/database credentials; overlap old validation keys for access lifetime; recheck least privilege.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/rotation-check.sh
```

**Expect:** New tokens work, expired keys fail, no secret appears in logs or Git.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): rotate secrets without locking users out`.
- [ ] Tagged `w10-s03-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 2 — Backup leaves this machine

**You will have:** Recovery story works and old device tokens are disabled in restore environment.

### W10-S04 — Verify nightly cron retention and mounts (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Reverse Proxy Https primer](../primers/REVERSE-PROXY-HTTPS.md)

**Before you start:**

- [ ] W10-S03 committed with its check passing; tag `w10-s03-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Nightly pg_dump -Fc to private temp path, pg_restore --list, atomic rename, checksum and external-disk copy; prune only matching owned files older than 14 days after successful verified copy.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/backup.sh
```

**Expect:** Unmounted disk causes loud failure, never writes into empty mount directory; latest backup age visible.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): verify nightly cron retention and mounts`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W10-S05 — Restore on a clean machine (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Reverse Proxy Https primer](../primers/REVERSE-PROXY-HTTPS.md)

**Before you start:**

- [ ] W10-S04 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create script operating on explicitly named scratch host/container with same PostgreSQL major; copy DB dump plus private files/key references; no production target accepted by default.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/restore-clean.sh
```

**Expect:** New clean instance migrates safely and passes auth/row-count/file-checksum smoke.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): restore on a clean machine`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W10-S06 — B verifies recovery from phone (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Reverse Proxy Https primer](../primers/REVERSE-PROXY-HTTPS.md)

**Before you start:**

- [ ] W10-S05 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Point test device at restored private candidate; login and verify known synthetic attendance/leave/request/export without sending real notifications.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
flutter test
```

**Expect:** Recovery story works and old device tokens are disabled in restore environment.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): b verifies recovery from phone`.
- [ ] Tagged `w10-s06-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Stone 3 — Failure modes are observable

**You will have:** Only approved Compose services run; one host is explicitly not high availability.

### W10-S07 — Test disk and log bounds (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Reverse Proxy Https primer](../primers/REVERSE-PROXY-HTTPS.md)

**Before you start:**

- [ ] W10-S06 committed with its check passing; tag `w10-s06-green` exists.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Create non-destructive disk/headroom and Docker log policy check; simulate threshold with test override, never fill disk; keep Kuma/admin private.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/disk-check.sh
```

**Expect:** Alert fires at threshold; logs rotate; no unbounded backup or export growth.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): test disk and log bounds`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W10-S08 — Test internet and database loss (45–60 min)

**Why:** A makes one observable improvement before changing anything else.

**Read first (10 min):** [Reverse Proxy Https primer](../primers/REVERSE-PROXY-HTTPS.md)

**Before you start:**

- [ ] W10-S07 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Controlled private drill stops app connectivity, not arbitrary host networking; restart PostgreSQL service after verified backup, observe failed commands versus uncertain committed keys.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/recovery-check.sh
```

**Expect:** No false success on DB loss; stable-key retry reconciles; remote access fails honestly when internet down.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): test internet and database loss`.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

### W10-S09 — Record broker and host availability limits (45–60 min)

**Why:** B makes one observable improvement before changing anything else.

**Read first (10 min):** [Reverse Proxy Https primer](../primers/REVERSE-PROXY-HTTPS.md)

**Before you start:**

- [ ] W10-S08 committed with its check passing.

**Do:**

1. Inspect first: find the current implementation, test and configuration for this action; if absent use the explicitly proposed path below.
1. Verify Kafka/Registry are not production services under outcome C; document no broker-recovery claim. Record how a future Kafka ADR would require real-broker outage/offset tests.
1. Run the following check only after its target exists; record actual output without secrets.

```bash
bash ops/selfhost-smoke.sh
```

**Expect:** Only approved Compose services run; one host is explicitly not high availability.

**Check:**

- [ ] Output matches Expect and required tests pass with zero failures.
- [ ] Committed with message `feat(week-10): record broker and host availability limits`.
- [ ] Tagged `w10-s09-green` only after tests pass and app starts.

**If it fails:** [Week diagnostics](TROUBLESHOOTING.md); after 45 minutes use the [stuck protocol](../STUCK-PROTOCOL.md). At 90 minutes use this stone’s exact cut in README; a failed critical check stays **FAIL**.

**Status:** **Not run**; authored from source inspection, not executed implementation.

Shutdown ritual for this day:

- [ ] Commit reviewed files or preserve the safe WIP branch.
- [ ] Save the next exact step and blocker in YOU-ARE-HERE.md.
- [ ] Record energy in words and close the laptop.

## Continue

[Part two: buffer and Friday](BUILD-GUIDE-PART-2.md).
