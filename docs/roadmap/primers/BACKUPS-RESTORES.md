# Database backups and disaster recovery

> **Note:** An unverified backup is an illusion. True disaster recovery requires automated, encrypted backup snapshots paired with regular, automated scratch restoration testing on isolated clusters.

## Purpose and core concept

Database backup and disaster recovery strategies protect organizational data against hardware failure, operator mistakes, ransomware, and site disasters.

A complete disaster recovery strategy specifies two core operational metrics:

- Recovery Point Objective (RPO): The maximum acceptable duration of data loss measured in time (e.g., nightly backups provide a 24-hour RPO).
- Recovery Time Objective (RTO): The maximum acceptable duration of downtime required to restore service following a total disaster (e.g., bare-metal restore in under 60 minutes).

## Problem being solved

Relying on RAID disks or simple directory copies creates critical vulnerabilities:

- File system corruption: Copying active PostgreSQL data directories (`/var/lib/postgresql/data`) while the database is running copies torn pages and corrupted indexes.
- Single physical location: Keeping backup files on the same physical hard drive as the database offers zero protection if the drive suffers a mechanical failure.
- Untested restore scripts: Many organizations faithfully take backups for years, only to discover during a real disaster that their restore script fails due to version incompatibilities or missing encryption keys.

## Simple mental model

A backup is like packing a spare tire in your car trunk.

Disaster recovery testing is actually pulling over, jacking up the car, bolting the spare tire onto the wheel hub, and driving five miles to confirm the spare tire holds air pressure and fits your lug nuts.

## How it works

The HRFlow backup and recovery workflow combines PostgreSQL tools with scheduled automation:

1. Scheduled dump: A nightly cron job runs `pg_dump -Fc` against PostgreSQL. The custom format (`-Fc`) produces a compressed, flexible archive allowing selective schema or table restoration.
1. Local retention: Backups are stored in `/var/backups/hrflow` with a strict rolling retention policy (retaining the last 14 daily snapshots).
1. Encryption at rest: Backup archives are encrypted using GPG or age with a secure offline public key.
1. External replication: An automated rsync script copies encrypted archives and uploaded file attachments to a detached external physical USB drive.
1. Weekly scratch verification: Every Friday, an automated recovery script spins up an isolated temporary PostgreSQL cluster, restores the latest dump, runs database integrity constraint checks, and tears down the temporary cluster.

## Why HRFlow needs it

In HRFlow's self-hosted deployment architecture:

- Single physical host: Because the application runs on a single computer rather than a distributed cloud cluster, hardware failures require rapid recovery from backup archives.
- Labor compliance: Employee attendance records, disciplinary logs, and leave histories must be legally retained and tamper-evident.
- Operator confidence: Verifying restores every Friday guarantees that if the server motherboard fails over the weekend, the platform operator can restore the complete system onto a replacement computer within one hour.

## Minimal example

Automated backup shell script (`backup-nightly.sh`):

```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="/var/backups/hrflow"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DUMP_FILE="${BACKUP_DIR}/hrflow_${TIMESTAMP}.dump"

mkdir -p "${BACKUP_DIR}"

# Execute PostgreSQL custom-format dump
docker exec hrflow-postgres pg_dump -U hrflow -Fc hrflow > "${DUMP_FILE}"

# Prune local backups older than 14 days
find "${BACKUP_DIR}" -name "hrflow_*.dump" -type f -mtime +14 -delete

echo "Backup completed successfully: ${DUMP_FILE}"
```

Automated scratch restore test script (`restore-scratch.sh`):

```bash
#!/usr/bin/env bash
set -euo pipefail

LATEST_DUMP=$(find /var/backups/hrflow -name "hrflow_*.dump" | sort | tail -n 1)
SCRATCH_DIR=$(mktemp -d /tmp/hrflow_scratch_XXXXXX)
SCRATCH_PORT=55439

# Initialize temporary isolated cluster
initdb -D "${SCRATCH_DIR}/data" -A trust --no-locale > /dev/null
pg_ctl -D "${SCRATCH_DIR}/data" -l "${SCRATCH_DIR}/pg.log" \
  -o "-k ${SCRATCH_DIR} -p ${SCRATCH_PORT} -h 127.0.0.1" -w start

# Create target database and restore custom dump
createdb -h 127.0.0.1 -p ${SCRATCH_PORT} hrflow_test
pg_restore -h 127.0.0.1 -p ${SCRATCH_PORT} -d hrflow_test --clean --if-exists "${LATEST_DUMP}"

# Stop and clean up temporary cluster
pg_ctl -D "${SCRATCH_DIR}/data" -m immediate -w stop
rm -rf "${SCRATCH_DIR}"

echo "Scratch restore verified successfully from ${LATEST_DUMP}"
```

## Application-specific implementation

Cron scheduling and attachment backup in HRFlow:

```bash
# Add to host crontab: Run nightly backup at 02:00 AM
0 2 * * * /opt/hrflow/scripts/backup-nightly.sh >> /var/log/hrflow-backup.log 2>&1

# Add to host crontab: Run weekly scratch restore check on Friday at 16:00
0 16 * * 5 /opt/hrflow/scripts/restore-scratch.sh >> /var/log/hrflow-restore.log 2>&1
```

In addition to PostgreSQL database dumps, the backup routine archives the uploaded private file attachments directory (`/opt/hrflow/data/attachments`) using `rsync -avz` to ensure medical certificates and purchase order receipts are preserved alongside relational records.

## Common mistakes and pitfalls

Avoid these dangerous backup mistakes:

> **Pitfall:** Assuming database dumps include uploaded file attachments. PostgreSQL `pg_dump` only backs up relational table data and metadata. Uploaded PDFs, images, and signed contracts stored on the host filesystem must be backed up separately in the exact same backup window.

- Storing encryption keys next to encrypted backups: If the private GPG key is stored in the same directory as the encrypted backup dump, any attacker who compromises the backups also obtains the decryption key. Keep private recovery keys off-site.
- Restoring over production data by accident: Restore scripts must default to creating a temporary test database (`hrflow_test`) or isolated scratch port, requiring explicit manual flags before overwriting production databases.
- Failing to verify backup exit codes: Running `pg_dump` without `set -e` in bash scripts allows truncated dumps caused by out-of-disk errors to appear as successful backups.

## Testing and debugging

Validation routine:

- Zero-data restore test: Execute `restore-scratch.sh` and verify that `pg_restore` completes with zero exit code.
- Row count assertion: Query restored table row counts (e.g., `SELECT count(*) FROM identity_org.employees`) and compare against production counts.
- Integrity extension check: Run `docs/validation/constraint-smoke.sql` against the restored scratch database to verify that multi-tenant foreign keys and check constraints remain intact.

## Trade-offs and alternatives

Backup strategies:

- Logical dumps via `pg_dump` (HRFlow choice): Highly portable across PostgreSQL minor versions, compact custom format, selective restore capability; takes longer to restore multi-terabyte databases.
- Physical binary backups (pgBackRest / WAL-G): Continuous Write-Ahead Log archiving allowing Point-In-Time Recovery (PITR); requires dedicated storage servers and complex administration.
- Storage volume snapshots (ZFS / AWS EBS): Instant point-in-time snapshots; tied to specific cloud providers or file systems.

## When not to use it

Do not use full database dumps for:

- High-frequency undo operations: Reverting a single user action (such as cancelling a leave request) should be handled via business compensation transactions, not database restores.
- Ephemeral test data resets: Use Testcontainers or database transactions that roll back at the end of tests.

## Related documentation

- [Docker Compose primer](DOCKER-COMPOSE.md)
- [Week 10 operations and restore build guide](../10-OPERATIONS-AND-RESTORE/BUILD-GUIDE.md)
- [Operations host runbook](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md)
- [Hardware and hosting assessment](../00-PREPARATION/HARDWARE-AND-HOSTING-ASSESSMENT.md)
