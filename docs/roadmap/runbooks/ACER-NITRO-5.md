# Acer Nitro 5 deployment and recovery checklist

## Status and boundary

This checklist prepares the intended self-hosted Acer Nitro 5 reported as an Intel Core i5 machine with 16 GB RAM and 2 TB storage. The Acer and its separate encrypted backup disk are unavailable, so every host, deployment, backup and restore item below is **NOT RUN**. Local Mac checks cannot satisfy these items.

Use only a reviewed revision and the operator-owned configuration described in [SCRIPT-INTERFACE](SCRIPT-INTERFACE.md). Do not expose database, Kafka, Schema Registry, management or owner-service ports. Do not use `docker compose down -v`, prune commands or an in-place production restore.

## 1. Record the actual host

Save command, UTC timestamp, operator and sanitized output in the Week 3 operations handoff.

```bash
uname -a
cat /etc/os-release
lscpu
free -h
lsblk -o NAME,SIZE,TYPE,FSTYPE,MOUNTPOINTS,MODEL
df -hT
docker version
docker compose version
systemctl is-enabled docker
systemctl is-active docker
```

- [ ] Confirm x86_64 Linux, exact CPU model and usable memory.
- [ ] Confirm whether the 2 TB device is HDD or SSD and record filesystem health.
- [ ] Confirm Docker data-root and PostgreSQL/Kafka volumes are on the intended host disk.
- [ ] Confirm AC power, lid/sleep behavior, wired/private network and operator recovery access.
- [ ] Record clock synchronization and the Asia/Phnom_Penh operator display time zone; application storage remains UTC.

## 2. Verify the 16 GB operating envelope

The current repository has no accepted host measurement and no verified per-container memory limits. Before deployment, record idle and representative-workload RSS for PostgreSQL, Kafka, Schema Registry, seven Java services, web and Caddy. Do not copy the local Mac resource profile.

- [ ] Leave measured headroom for the OS, filesystem cache, image build and recovery operations.
- [ ] Set JVM/container limits only from an observed Acer run; a limit that causes restart loops is a failed result.
- [ ] Run image builds separately from peak application load if the measured 16 GB envelope requires it.
- [ ] Record disk latency during PostgreSQL WAL and Kafka flush activity; the reported 2 TB capacity does not establish acceptable IOPS.
- [ ] Configure bounded JSON logs and verify rotation without deleting evidence.

## 3. Review release inputs

```bash
git rev-parse HEAD
git status --short
bash -n ops/*.sh infra/scripts/*.sh
docker compose -f infra/selfhost/compose.yaml config --quiet
```

- [ ] Checkout is the reviewed immutable revision and has no unreviewed production changes.
- [ ] Operator config is outside Git, owned by the operator and mode 600.
- [ ] Signing keys and private files are protected mounts; no secret value appears in Compose output or evidence.
- [ ] Runtime, migration, backup and job roles are distinct, NOSUPERUSER and NOBYPASSRLS as required.
- [ ] Compose lists the gateway, all six owner services, web, Caddy, PostgreSQL, Kafka and Schema Registry.
- [ ] Image/source artifacts and configuration identifiers are recorded before mutation.

The current candidate does not yet satisfy the full-service Compose item: attendance, leave, request, notification and reporting are absent, and the accepted `ops/build-images.sh` helper is absent.

## 4. Prepare Kafka and Schema Registry

- [ ] Start PostgreSQL, Kafka and Schema Registry privately and wait for their actual readiness checks.
- [ ] Create every accepted topic using `infra/scripts/create-topic.sh`; keep broker auto-creation disabled.
- [ ] Register reviewed subjects using the `register-*-schema.sh` scripts.
- [ ] Record topic configuration, Registry subject/version and compatibility result.
- [ ] Verify a real publish, consume and duplicate/replay-safe effect before enabling each relay/consumer switch.
- [ ] Verify restart and leader/process-loss recovery without deleting broker volumes or rewinding production offsets automatically.

Registration syntax or a mock Registry does not prove deployed compatibility or event delivery.

## 5. Deploy and smoke test

- [ ] Take and restore-verify the required pre-deploy backup first.
- [ ] Run `bash ops/deploy.sh` only with the exact target confirmation.
- [ ] Confirm every dependency and application readiness endpoint from the private network.
- [ ] Run authenticated login/profile and implemented business-route checks through Caddy/gateway.
- [ ] Run anonymous, malformed-token, wrong-role, forged-tenant and foreign-tenant denial checks.
- [ ] Run direct service token-validation checks where the owning gate requires them.
- [ ] Record correlation identifiers and sanitized logs for both allowed and denied outcomes.
- [ ] Exercise the reviewed compatible-image rollback path on failure; never downgrade an applied migration.

The current checkout has no `ops/selfhost-smoke.sh`; authenticated and denied intended-host smoke remains blocked until that accepted runner or an equivalent reviewed command set exists.

## 6. Back up to the separate encrypted disk

- [ ] Confirm the disk is a different physical device and its encrypted mapper is open.
- [ ] Confirm `findmnt` resolves the backup path to the expected mapper before writing.
- [ ] Run `bash ops/backup.sh` with exact target confirmation.
- [ ] Verify the custom database dump, globals/roles, private files, keys, source bundle, release images, image/config inventory, Kafka topics and Registry subjects.
- [ ] Verify every checksum on the mounted encrypted disk.
- [ ] Record encryption state, filesystem, device identity, artifact path, size, checksum and UTC timestamp.
- [ ] Apply retention only after a newer backup has passed an isolated restore; never remove the sole valid backup.

## 7. Restore into isolation

- [ ] Create a uniquely named scratch database/project/volume that cannot resolve to production.
- [ ] Keep outbound notifications and event publication disabled during restore validation.
- [ ] Run `bash ops/restore-scratch.sh` with the exact scratch confirmation.
- [ ] Verify roles/grants, known synthetic tenant counts, ledgers, private-file checksums, schema inventory and replay metadata.
- [ ] Run login/profile and required business smoke against the restored target.
- [ ] Prove the source database and source files are unchanged.
- [ ] Write the reviewed restore report, then and only then promote the backup receipt to `latest-verified`.

## 8. Evidence package

For every command record revision, operator, host alias, UTC start/end, exit code, safe output excerpt and artifact/report path. Keep secret values, tokens, personal data and private keys out of evidence. Classify each gate as **PASS**, **FAIL**, **BLOCKED** or **NOT RUN**; never replace Acer evidence with Mac output.
