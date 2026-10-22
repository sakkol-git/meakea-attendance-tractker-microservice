# Week 08 — Integration and accessibility

> **Navigation:** [Previous: Week 07](../07-REQUESTS-AND-APPROVALS/README.md) · [You are here](../YOU-ARE-HERE.md) · [Next: Week 09](../09-REPORTING-AUDIT-OBSERVABILITY/README.md)

## At a glance

| Field | Value |
| --- | --- |
| Week | 08 |
| Owners | A backend/web/infra; B Flutter and client acceptance |
| Planned maximum | A 14.28 h, B 14.28 h; 70 percent of 24, light-week reduction where applicable |
| Depends on | Week 07 critical gate or its explicit carried failure |
| Gate | **NOT RUN** |

## Objective and starting point

Build integration and accessibility as a visible mini-win. The audit found scaffolds, not this feature. Prerequisites are earlier green stones; none are pre-checked. Every named new path/class/script is a proposed implementation target: inspect for existing work first, then create only what is missing.

## Stepping stones

| Stone | Visible win | Steps | Upper step time |
| --- | --- | --- | --- |
| 1 | One wire contract for both clients | W08-S01 to S03 | 135 min, shared steps counted per owner |
| 2 | Offline and authentication edges | W08-S04 to S06 | 135 min, shared steps counted per owner |
| 3 | Usability without new scope | W08-S07 to S09 | 135 min, shared steps counted per owner |
| 4 | Buffer evidence and failure recovery | W08-S10 to S12 | 135 min, shared steps counted per owner |
| 5 | Friday gate deploy and recovery | W08-S13 to S15 | 135 min, shared steps counted per owner |

## Minimum Viable Gate

- [ ] Live phone and React show same resources and recover after API errors.
- [ ] Same persisted resource appears once; stale dashboards are labeled.
- [ ] Both clients observe same committed versions; no contract forks.
- [ ] Friday redeploy, authenticated smoke and scratch restore evidence exist.
- [ ] Security/correctness/event/restore failures are fixed or this critical gate stays **FAIL**.

## Stretch and exact cuts

- [ ] Improve wording/visual styling only if all critical tests are green.
- [ ] Add no more than the current lesson refactor after admission passes.

At 90 minutes stuck, simplify Stone 1 to one synthetic tenant and one happy-path fixture for diagnosis only; retain two-tenant security acceptance before the gate. Stone 2 drops optional filters/channels/extra forms, never locking or authorization. Stone 3 drops optional UI polish or pattern refactor and keeps the plain tested implementation. Stone 4 cuts all new scope. Stone 5 never skips backup/restore or substitutes a development machine for the agreed host.

## Friday demo and risks

I can demonstrate one wire contract for both clients, offline and authentication edges, and usability without new scope. If prerequisites remain blocked, I show the failing check and recovery evidence instead of calling it done.

Main risks are missing prerequisites, unmeasured junior learning speed and host readiness. Do not start next week until this critical gate passes; when deadline pressure requires moving a noncritical item, record a conditional pass with owner/date and cut stretch first. No weekend work.

## Learn just in time

- [Openapi](../primers/OPENAPI.md).
- [Offline Queue](../primers/OFFLINE-QUEUE.md).

Introduce at most two new concepts on any build day. Primers are 10 to 20 minutes; Thursday revises familiar concepts only. Required code/tests are not replaced by reading.

## Out of scope

Kafka runtime, ML, Kubernetes, cloud databases, tracking, new frameworks and unapproved patterns are excluded. A simpler tested switch beats an unadmitted abstraction.

## Files

- [Build guide](BUILD-GUIDE.md).
- [Build guide part two](BUILD-GUIDE-PART-2.md).
- [Verify and gate](VERIFY-AND-GATE.md).
- [Troubleshooting](TROUBLESHOOTING.md).
- [Stuck protocol](../STUCK-PROTOCOL.md).
- [Definition of done](../DEFINITION-OF-DONE.md).
- [Dashboard](../YOU-ARE-HERE.md).
