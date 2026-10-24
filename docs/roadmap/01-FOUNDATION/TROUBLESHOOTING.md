# Week 01 troubleshooting

> **Navigation:** [Previous: Preparation](../00-PREPARATION/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 02](../02-IDENTITY-AND-HOST/README.md)

## Runtime never becomes ready

- Symptom: Compose app unhealthy or request refuses connection.
- Likely cause: Docker stopped, wrong JDK, migration error or database not ready.
- Diagnose: `docker compose -f infra/selfhost/compose.yaml ps`; inspect target first, because scripts are proposed until their build step.
- Healthy result: Healthy app and PostgreSQL with no public DB binding.
- Fix: Check disk then Docker/JDK; inspect last safe log lines; correct one config on branch, never delete volume.
- Verify: Re-run readiness and current happy scenario.

## Schema or key assertion fails

- Symptom: Migration/constraint/idempotency test returns an unexpected failure.
- Likely cause: Applied migration edited, missing tenant/key predicate or stale fixture.
- Diagnose: `mvn -f services/identity-org/pom.xml test`; inspect target first, because scripts are proposed until their build step.
- Healthy result: Zero failures with real PostgreSQL fixtures.
- Fix: Reproduce smallest row pair; add a new migration or correct scoped query; preserve applied history.
- Verify: Run integrity and duplicate/concurrency scenarios.

## Client and contract disagree

- Symptom: Flutter/Nuxt parses wrong field or sees unexpected HTTP code.
- Likely cause: Stale mock/example, wrong base URL or changed token state.
- Diagnose: `flutter test`; inspect target first, because scripts are proposed until their build step.
- Healthy result: Mock and live schema agree and token failure is explicit.
- Fix: Compare sole OpenAPI fixture to raw safe response; fix contract and both clients together.
- Verify: Run validation/unauthorized plus happy tests.

## Host or backup gate blocked

- Symptom: No confirmed real host/external disk, old dump, failed scratch restore.
- Likely cause: Production setup not ready, host sleeping, cron missed, disk unmounted or incompatible dump.
- Diagnose: `bash ops/restore-scratch.sh`; inspect target first, because scripts are proposed until their build step.
- Healthy result: Scratch restore passes with recent verified external copy.
- Fix: Stop external exposure; check mount/age/major versions and recover in scratch only. Assign owner and cut stretch.
- Verify: Friday backup/restore and private deployed smoke must pass.

## Current mini-win fails

- Symptom: Flutter test passes; repository location is known.
- Likely cause: Implementation does not satisfy this week invariant or prerequisite.
- Diagnose: `bash scripts/acceptance/w01.sh diagnose`; inspect target first, because scripts are proposed until their build step.
- Healthy result: One minimal failing scenario with safe evidence or a documented healthy diagnostic.
- Fix: Use 45/90 minute protocol; preserve WIP, recover from last green branch, fix smallest test first.
- Verify: Re-run current feature matrix and regression before tag.

## Stop rule

[Stuck protocol](../STUCK-PROTOCOL.md) applies after 45 minutes. Never run destructive cleanup, disable tenant checks, or skip restore to manufacture a green gate.

## V2 additions for the final service architecture

| Symptom | Inspect first | Safe next action |
| --- | --- | --- |
| MVC advice does not catch gateway failure | Nuxtive dependency and filter error path | Assert the WebFlux error writer directly; do not add JPA/blocking calls to gateway |
| Registry cannot connect despite valid Compose YAML | Host/container listeners and Registry bootstrap env | Correct the one actual mismatch, then test connectivity; preserve volumes |
| Tests green with Docker stopped | Skipped test flags and fallback DB | Remove silent skip/H2 fallback for required integration tests; environment failure stays visible |
| Empty coverage or architecture result | Actual compiled packages and selectors | Fail on absent implemented critical packages; record not-yet-built packages honestly |
| Auth test runs out of time | Earliest red assertion and its fixture | Commit isolated red work on work branch, keep tag/deploy blocked, resume exact assertion |
| Backup script syntax passes but no restored data | External copy, roles, files and replay inventory | Execute isolated restore and verify counts/auth; syntax is not recovery evidence |
| Cookie transport/CSRF not implemented | Actual browser request/cookie path | Keep web security gate failed; add explicit source/test steps before publishing |

## Final-service V2 diagnostics

| New failure | Inspect first | Safe response |
| --- | --- | --- |
| A red test will not compile | Resolved fixture/API versions and proposed class names | Repair setup separately; only semantic assertion failure is intended red |
| A query leaks or RLS test passes as owner | Runtime role, transaction-local tenant and predicate | Use non-owner role and same transaction; pool/rollback/insert negatives are mandatory |
| Kafka duplicate/stale fact appears | Owner intent, inbox uniqueness, revision and offset ordering | Commit effect/receipt before offset and suppress stale full snapshot; retain failed event |
| A script exists but behavior is unproved | Actual script case and exit propagation | Execute its isolated assertion, never treat bash syntax/config validation as deployment/restore |
| Client refresh loops or cookie is rejected | Origin/CSRF flags, credentials mode and single-flight guard | Fail closed once; keep refresh off web storage and do not retry hostile CSRF |
| Archive restores but files/projection differ | Backup cut, file checksums, schemas and replay range | Repeat isolated restore and snapshot bootstrap; never overwrite source DB |
| Learning/time cap exceeded | Actual next smallest step and known/unfamiliar primer count | Stop at cap, use recovery reserve, cut Stretch first and record schedule impact |
