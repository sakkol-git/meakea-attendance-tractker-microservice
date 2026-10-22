# Milestone evidence and demonstration ladder

## Every gate needs evidence

No weekly gate is currently executed. Evidence includes commit, actual tag, command/result, deployment digest, backup ID and scratch restore result. Screenshots alone do not prove concurrency or isolation.

| Week | Demo on Friday | Exact evidence required | Gate |
| --- | --- | --- | --- |
| 01 | A protected workspace; One database-backed service | `w01-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 02 | Credentials and tenant context; Isolation before exposure | `w02-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 03 | Employees and hierarchy; Schedules and calendar | `w03-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 04 | A safe check-in interval; Checkout and classification | `w04-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 05 | Publication intent is atomic; Replay-safe scheduled worker | `w05-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 06 | Entitlement and eligible days; Reservation and approval | `w06-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 07 | Drafts and bounded forms; Safe decisions and fulfillment | `w07-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 08 | One wire contract for both clients; Offline and authentication edges | `w08-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 09 | Rebuildable facts and protected audit; CSV and storage lifecycle | `w09-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 10 | Boot and rollback are proven; Backup leaves this machine | `w10-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 11 | Adversarial authorization; Recovery and load evidence | `w11-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |
| 12 | Freeze a reproducible release; Re-run restoration and rollback | `w12-s15-green`, week runner zero critical failures, smoke and restore report | **NOT RUN** |

## Critical versus conditional

Auth/isolation, attendance/leave correctness, durable event intent, deployed smoke, and restore failures are **FAIL**. A conditional pass names only noncritical residual work with owner/date. The next weekly README inherits the exact failing action; no clean-looking table resets the failure.

[Weekly status template](WEEKLY-STATUS-TEMPLATE.md) and [roadmap](ROADMAP.md).
