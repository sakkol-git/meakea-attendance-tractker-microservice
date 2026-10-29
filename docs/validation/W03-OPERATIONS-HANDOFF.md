# Week 3 operations handoff

## Scope and evidence boundary

Prepared 2026-10-29 for Chat 1. This report covers deployment preparation, operational scripts, observability prerequisites, Kafka/Schema Registry procedures and recovery preparation. It does not decide the Week 3 gate. The intended Acer Nitro 5 and separate encrypted backup disk are unavailable; all physical-host, deployment, authenticated/denied routed smoke, backup and scratch-restore gates remain blocked or not run.

## Gate table

| Operational gate | State | Direct evidence or exact blocker |
| --- | --- | --- |
| Repository/operator configuration guards | **PASS** (local only) | `env -u HRFLOW_OPS_CONFIG bash ops/deploy.sh` exited 1 before mutation: required operator config absent. |
| Shell syntax | **PASS** (local only) | `bash -n ops/*.sh infra/scripts/*.sh` exited 0 on the development Mac. |
| Focused operational policy checks | **PASS** (local only) | Tenant-boundary regression/negative and demonstration negative checks exited 0; output explicitly identifies policy simulation rather than live authorization. |
| Shell static analysis | **PASS** (local only) | `shellcheck -x -s bash ops/*.sh infra/scripts/*.sh` exited 0 after executable defects and stale warnings in the operational check scripts were repaired. |
| Compose syntax | **PASS** (local only) | Compose v5.5.1 lists `web`, `kafka`, `postgres`, `identity-org`, `api-gateway`, `caddy`, `schema-registry` with dummy nonsecret interpolation. This is configuration parsing only. |
| Full MVP service manifest | **FAIL** | `infra/selfhost/compose.yaml` omits attendance, leave, request, notification and reporting. Accepted Week 3 `ops/build-images.sh` is absent. |
| Private listener policy | **PASS** (configuration only) | Inspected Compose publishes only Caddy edge ports; owner/dependency ports are not published. Host firewall and actual listeners are **NOT RUN**. |
| Secrets and role separation | **BLOCKED** | Local guards require protected config/keys and distinct identity roles. Full per-owner runtime/migration/job grants require Acer database execution and inspection. |
| Health/readiness ordering | **PARTIAL** | Foundation Compose has health/dependency conditions. Five owner services are absent, Kafka and Schema Registry have no health checks, and full startup ordering cannot be verified. |
| Authenticated/denied gateway smoke | **BLOCKED** | Acer unavailable; accepted `ops/selfhost-smoke.sh` is absent. Existing local auth evidence is not intended-host evidence. |
| Kafka topics and Registry subjects | **BLOCKED** | Registration helpers exist, but no Acer broker/Registry execution, compatibility report or publish/consume/replay report exists. |
| Logs, metrics and diagnostics | **PARTIAL** | Compose bounds JSON logs. Application health exists; deployed private metrics collection, alerting and Acer resource measurements are unverified. |
| Backup creation and encryption | **BLOCKED** | Separate encrypted disk unavailable. `ops/backup.sh` is a guarded draft; no Acer artifact or encryption evidence exists. |
| Isolated scratch restore | **BLOCKED** | No Acer backup/disk. `ops/restore-scratch.sh` is a guarded draft and does not yet prove files, grants, auth and replay completeness. |
| Acer resource suitability | **BLOCKED** | Reported i5/16 GB/2 TB only. Exact OS, architecture, disk type/health, IOPS, usable RAM, Docker runtime and sustained workload measurements are **NOT RUN**. |

## Local Mac facts

- Development host: Darwin arm64; Docker 29.6.2; Docker Compose 5.5.1; ShellCheck 0.11.0.
- No container deployment, backup or restore was executed in this checkpoint.
- The local checks establish parsing and fail-closed policy behavior only. They do not establish production readiness, service authorization, event delivery, backup usability or Acer capacity.

## Backend/owner coordination required

1. Supply the five owner services in the self-host manifest with separate reviewed runtime/migration/job credentials and private health checks.

1. Supply or review the accepted all-service image build runner and authenticated/denied self-host smoke runner.

1. Confirm gateway routes and implemented operations used by smoke tests; pending contract operations must not be represented as deployed success.

1. Confirm topic/subject inventory and feature switches for only the relays/consumers whose provider tests have passed.

1. Define concrete restored-data assertions for each owner schema, private files, outbox/inbox/offset state and reporting exports.

## Intended-host execution owner

Follow [the Acer checklist](../roadmap/runbooks/ACER-NITRO-5.md) when the machine and encrypted disk are available. Chat 1 should retain the Week 3 gate as **NOT RUN**/**BLOCKED** until direct evidence supports every critical item.
