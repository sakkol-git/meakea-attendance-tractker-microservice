# Week 11 verification and gate

> **Navigation:** [Previous: Week 10](../10-OPERATIONS-AND-RESTORE/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 12](../12-RELEASE-AND-DEMONSTRATION/README.md)

## Inspect first and build the test runner

The commands below target proposed `scripts/acceptance/w11.sh`, created in W11-S10 before execution. Each branch must execute a real JUnit/API or client assertion and propagate nonzero failure; an echo-only script is forbidden. Feature tests are added with their implementation steps, not postponed until Thursday. No runner or test is claimed to exist today.

Use two synthetic tenants, own/unrelated employees, direct-report manager and HR fixtures. Read auth tokens privately from test helpers; never put secrets in guide commands. For unavailable week-1 auth routes, unauthorized/cross-tenant cases assert diagnostic is private and DB tenant constraint fixtures; week 2 replaces those with authenticated API tests.

## Per-feature matrix

Repeat every case for each of this week first three stone outputs. Skip only if truly inapplicable and record why; critical path cases cannot be waved through.

| Case | Exact command after creation | Success looks like | Failure means |
| --- | --- | --- | --- |
| happy | `bash scripts/acceptance/w11.sh happy` | The three feature mini-wins above match their exact expected resource/state. | Owner route, state or setup is wrong. |
| validation | `bash scripts/acceptance/w11.sh validation` | Malformed dates/empty required fields produce documented 400/422, no row. | DTO validation or domain rule missing. |
| unauthorized | `bash scripts/acceptance/w11.sh unauthorized` | Missing/invalid token is 401; wrong role/hidden object 403/404. | Authentication or resource authorization defect. |
| cross-tenant | `bash scripts/acceptance/w11.sh cross-tenant` | Tenant B identifiers expose no tenant A rows/counts/files. | Critical isolation defect; block gate. |
| duplicate | `bash scripts/acceptance/w11.sh duplicate` | Same key returns same ID; changed body 409; one business effect. | Idempotency boundary is wrong. |
| concurrency | `bash scripts/acceptance/w11.sh concurrency` | Concurrent same-employee/approval/balance commands preserve DB invariant. | Lock/unique/version rule is missing. |
| recovery | `bash scripts/acceptance/w11.sh recovery` | Interrupted operation resumes or reconciles; no lost durable intent. | False success or incomplete recovery design. |
| integrity | `bash scripts/acceptance/w11.sh integrity` | Tenant FK/unique/check constraints reject corrupt rows. | Database cannot defend invariant. |
| regression | `bash scripts/acceptance/w11.sh regression` | Previously green critical flows still pass. | New change broke baseline; recover before moving on. |

## Mini-win evidence

- [ ] Stone 1: No token leak or stale identity silently regains scope. Save request/test ID and actual safe output.
- [ ] Stone 2: Core manual workflows recover and battery/permission limits are disclosed. Save request/test ID and actual safe output.
- [ ] Stone 3: Feedback is safe, bounded and tied to existing checklist IDs. Save request/test ID and actual safe output.

## Minimum Viable Gate

- [ ] Required matrix cases executed with zero critical failures; regression reports saved.
- [ ] Migrations start clean, integrity checks pass, app starts and all five actual stone tags exist.
- [ ] Friday confirmed-host redeploy and authenticated smoke pass; week 1 local-only exception is recorded.
- [ ] External-copy backup and scratch restore verified; week 1 disposable local dump recorded.
- [ ] Global checklist evidence and dashboard updated; no critical failure silently omitted.

## Stretch

- [ ] Optional style/channel/refactor completed only after admission and full gate.

## Gate decision

Result: **NOT RUN**. Record **PASS** only with all required evidence. **CONDITIONAL PASS** names noncritical outstanding item, owner and calendar due date. **FAIL** names critical check, minimal reproduction, recovery branch/tag and next action; that action becomes next week first stone before new scope.

## Retro

- What was slow: actual step ID, elapsed time, and why.
- What to cut: named stretch item; never security or restore tests.
- Energy: rested, steady, tired, or stop; protect weekend.
- Pattern review: admission answers, simpler alternative, and whether it was worth it.
- Next week: hours confirmed, carried failure owner, and exact first action.
