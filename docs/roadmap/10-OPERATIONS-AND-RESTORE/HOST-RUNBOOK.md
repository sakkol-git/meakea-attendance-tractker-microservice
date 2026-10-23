# Single-host operations runbook

## Inspect before copying commands

These are implementation instructions, not installed scripts. Target files are `ops/backup.sh`, `ops/restore-scratch.sh`, `ops/restore-clean.sh`, `ops/deploy.sh` and the read-only checks named in the weekly guide. Create and test them first on synthetic data. Use Compose project name `hrflow` consistently through environment configuration. PostgreSQL service is proposed `postgres`; gateway is proposed `api-gateway`, business workloads retain owner service names. Inspect actual names before running anything.

The host is not ready according to the user. Disk headroom, domain/DNS, external disk, Docker and reboot/login behavior must be proven. Do not expose a debug route to speed setup.

## Private Compose boundaries

Use an internal app/database network. Publish Caddy only to loopback while private. For tunnel mode, cloudflared reaches Caddy over their private network, so no inbound host port is required. No database/Kafka/Registry/Docker API/Kuma administrative port is publicly published. Caddy origin HTTP is allowed only on that private boundary; public client connection uses automatic Cloudflare TLS. Do not disable origin verification for an HTTPS origin instead of configuring it correctly.

Set `restart: unless-stopped`, Docker health checks, and bounded `json-file` logging with `max-size: 10m` and `max-file: 3`. These are initial per-container limits, not a capacity guarantee. Readiness checks application/database acceptance; liveness is not tied to optional notification provider availability.

## Backup script baseline

Copy this baseline into proposed `ops/backup.sh` and adapt only the explicitly configured paths/roles. `BACKUP_ROOT` must be a confirmed encrypted external mount, not an ordinary directory. The private-file snapshot and recovery secrets are separately protected; a dump alone does not back up attachments.

```bash
#!/usr/bin/env bash
set -euo pipefail
: "${HRFLOW_ROOT:?set the verified repository root}"
cd "$HRFLOW_ROOT"
: "${BACKUP_ROOT:?set the confirmed external backup root}"
: "${BACKUP_MOUNT:?set the confirmed external mount path}"
mount | grep -F " on ${BACKUP_MOUNT} (" >/dev/null
mkdir -p "$BACKUP_ROOT/hrflow"
stamp=$(date -u +%Y%m%dT%H%M%SZ)
file="$BACKUP_ROOT/hrflow/hrflow-${stamp}.dump"
temp="${file}.partial"
cleanup() { test ! -f "$temp" || unlink "$temp"; }
trap cleanup EXIT
umask 077
compose=(docker compose -f infra/selfhost/compose.yaml)
"${compose[@]}" exec -T postgres pg_dump -U backup_operator -d hrflow -Fc > "$temp"
"${compose[@]}" exec -T postgres pg_restore --list < "$temp" >/dev/null
mv "$temp" "$file"
shasum -a 256 "$file" > "${file}.sha256"
find "$BACKUP_ROOT/hrflow" -name 'hrflow-*.dump' -type f -mtime +13 -delete
find "$BACKUP_ROOT/hrflow" -name 'hrflow-*.dump.sha256' -type f -mtime +13 -delete
printf 'Backup verified structurally: %s\n' "$stamp"
```

The baseline writes directly to the encrypted external disk to avoid consuming scarce internal disk; the database volume remains the primary copy, external dump is the independent copy. For a separate local staging copy, require measured free space, write to a private temporary file, verify checksum after external copy, then prune only owned temporary artifacts. The chosen direct-stream path avoids an unbounded local staging backlog while satisfying an independent external copy.

`pg_restore --list` proves archive structure only. The weekly scratch restore proves usability. Directory/name pruning never touches database volumes or unrelated files. The backup operator needs explicit read/sequence grants and any applicable tenant/RLS bypass through a controlled backup role; runtime credentials are not reused. If mount fails, script must exit before writing, even if an empty mount-point directory exists.

This script is a baseline, not the complete approved production backup: additionally snapshot private attachment/export directories consistently, record checksums and a manifest tying files to the dump, and protect JWT/config recovery material through encrypted offline recovery storage. During the dump/file snapshot window quiesce attachment mutation or use a tested consistent snapshot method. Review legal retention before storing real employee evidence.

## Nightly scheduler and age checks

Install cron as the host account only after manual backup and restore pass. Give cron an explicit Docker/pg_restore executable PATH and mounted disk configuration; never print secret environment values. Schedule nightly at an agreed awake time and record last success on the external media. A sleeping or logged-out Mac can miss cron, so daily age checks are required; configure awake/login policy with owner agreement rather than claiming unattended startup.

```text
0 2 * * * /Users/apple/Documents/hrflow/ops/backup.sh
```

The actual crontab also needs safe configured environment and rotated log output, confirmed in the host account. Check last-success age; older than 24 hours is an alert and blocks a fresh public release. Keep 14 days of owned backup files only after current copy is valid; never delete the only known-restorable backup merely to satisfy age policy.

## Scratch restore recipe

Create a separate scratch Compose project/manifest with no production bind mount or volume, PostgreSQL major 16 and disposable synthetic credentials. Name it `hrflow-restore-check`; do not reuse the production project or pass production volume names. Verify backup checksum and `pg_restore --list` before loading. Apply migrations only in the tested order for that dump/release combination.

```bash
shasum -a 256 -c BACKUP_CHECKSUM_FILE
docker compose -f infra/selfhost/compose.yaml exec -T postgres pg_restore --list < BACKUP_DUMP_FILE
```

`BACKUP_CHECKSUM_FILE` and `BACKUP_DUMP_FILE` are replacement tokens, not literal filenames. Connect only to the scratch target and restore using its private role. Record employee/session/leave/request row counts, ledger totals, file checksums and auth smoke. Suppress real outbound delivery and invalidate old provider/refresh tokens in restore validation. Remove only the explicitly named scratch project after evidence; never use an unreviewed volume deletion command.

## Reboot and failed deployment

Record old and candidate image digests, config revision, migration checksums and previous smoke result before deployment. Validate Compose, run clean/upgrade migration tests and stage additive migrations before changing images. If smoke fails, switch to the compatible previous digest/config. Do not edit an applied migration or attempt a blind database down-migration. An incompatible schema change blocks automatic rollback and requires tested forward repair or explicit recovery plan.

During a planned synthetic reboot, note whether macOS requires login/unlock before Docker starts. `restart` policies do not start Docker Desktop itself. Demonstrate database healthy before app readiness, Caddy/tunnel connection after app availability, and missed cron detection. If unattended startup cannot be achieved on this host, disclose the outage window and obtain operator agreement before pilot.

## Power internet disk and PostgreSQL failure

Use non-destructive drills: stop the app's dependency connection in a controlled test environment rather than disable arbitrary host networking. Internet loss removes public tunnel access; authenticated private LAN access may remain. App command before commit fails; command after uncertain commit is reconciled by stable key. Worker pause retains durable intent until recovery.

Disk alarms inspect free space and simulate thresholds with a test override. Never deliberately fill the production disk. Bound logs, dumps, evidence and export retention, and stop admission with explicit retryable error before accepting work without durable storage.

Restart PostgreSQL after verified backup; allow crash recovery, then run health, migration, ledger/outbox and key-retry checks. Never delete/recreate its volume to make readiness green. Kafka is retained under ADR 008: restore Registry schemas/config, record DB backup cut and replay position, test duplicate/offset recovery and retention-gap snapshot bootstrap. A single broker/host has no failover.

## Clean-machine restore in Weeks 10 and 12

Use a different clean host or fresh isolated VM without production volumes, with architecture-compatible images and PostgreSQL 16. If no such target exists, BKP-02 is **FAIL**, not a scratch-restore substitute. Restore database plus private files, reconfigure secret references, keep notifications disabled and run full synthetic business smoke. Measure elapsed recovery and actual data-loss window; no RPO/RTO promise precedes evidence.

## Monitoring limits and escalation

Private Kuma checks authenticated-safe readiness and last backup/restore status. Never expose it or give it database credentials unnecessarily. If the entire host loses power Kuma on that host cannot notify; the operator's external availability check and agreed contact process must cover host-wide outage. No new hosted monitoring subscription is required; the limitation remains explicit.

Record alert owner, phone/contact channel outside public docs, and safe rollback/restore evidence. One computer, one database volume and one ISP are not high availability.

## Offline release artifacts

An image digest or Git commit ID is a reference, not the image/source itself. Before a clean-host drill, the encrypted external backup must contain the verified release image archive and Git source bundle (or a tested independently available registry/repository), plus config/model-version inventory. The local-only finish line uses the external offline archive; no cloud repository availability is assumed.

Save immutable release images and the reviewed Git bundle once per release, checksum them, and reference that artifact set from nightly data manifests instead of duplicating it every night. In scratch/clean recovery verify the bundle, load the images, compare their IDs to the release manifest, then restore DB/private files/roles/schema/replay metadata with live delivery disabled. Test access to the recovery keys separately; do not place secret values in Git or reports.

Use the actual allowlisted image references and protected release directory chosen by the script interface; no arbitrary user-supplied shell/image arguments. git bundle verify and docker image load must succeed on the isolated target before the clean-host application smoke. Data-only restore does not claim a complete lost-host recovery.
