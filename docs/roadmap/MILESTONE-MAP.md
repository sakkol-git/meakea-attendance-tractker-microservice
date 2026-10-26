# Milestone evidence and demonstration ladder

## Every gate needs evidence

No weekly gate is currently executed. Evidence includes commit, actual tag, command/result, deployment digest, backup ID and scratch restore result. Screenshots alone do not prove concurrency or isolation.

| Week | Demo on Friday | Exact evidence required | Gate |
| --- | --- | --- | --- |
| 01 | A protected workspace; One database-backed service | `w01-s38-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 02 | Credentials plus gateway and identity-service JWT gates; isolation before exposure | `w02-s35-green`, both validation-gate negatives, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 03 | JWT gates in the five remaining owner services; employees, hierarchy, schedules and calendar | `w03-s41-green`, per-service token negatives, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 04 | A safe check-in interval; Checkout and classification | `w04-s36-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 05 | Publication intent is atomic; Replay-safe Kafka consumer | `w05-s36-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 06 | Entitlement and eligible days; Reservation and approval | `w06-s36-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 07 | Drafts and bounded forms; Safe decisions and fulfillment | `w07-s42-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 08 | One wire contract for both clients; Offline and authentication edges | `w08-s33-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 09 | Rebuildable facts and protected audit; CSV and storage lifecycle | `w09-s41-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 10 | Boot and rollback are proven; Backup leaves this machine | `w10-s42-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 11 | Adversarial authorization; Recovery and load evidence | `w11-s34-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 12 | Freeze a reproducible release; Re-run restoration and rollback | `w12-s34-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |

## Critical versus conditional

Auth/isolation, attendance/leave correctness, durable event intent, deployed smoke, and restore failures are **FAIL**. A conditional pass names only noncritical residual work with owner/date. The next weekly README inherits the exact failing action; no clean-looking table resets the failure.

[Weekly status template](WEEKLY-STATUS-TEMPLATE.md) and [roadmap](ROADMAP.md).

## Calendar and mapping

Start 2026-10-26; Week-12 release rehearsal 2027-01-15, twelve-week boundary 2027-01-18. All weeks use eight stones under ADR 008 services/Kafka/Nuxt, ADR 010 dual JWT validation and ADR 011 Spring Security OAuth2 Resource Server. [ID mapping](STEP-ID-MAP.md) translates old positions/tags and [coverage](QUALITY-COVERAGE.md) enforces the senior bar. Dates are goals; a critical gate failure remains visible and can affect completion.
