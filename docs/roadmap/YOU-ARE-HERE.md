# You are here

## Capacity inputs

**Capacity inputs:** A=24; B=24; fraction=0.70; light=0.85.

User confirmed no change on this second pass. These inputs are authoritative; weekly planned totals include reading, self-review and recovery. Start date is 2026-10-26 per next-week instruction; host readiness remains unconfirmed.

## Current position

Current week: 01. Current step: W01-S01. Step state: **Not run**. Gate: **NOT RUN**.

Next action: inspect the dirty working tree, save a safe code checkpoint without staging secrets, and verify at least 30 GiB free before container pulls.

Last green application tag: none. Documentation commits are not green application tags. Blockers: low disk, missing Java 21, stopped Docker, host/domain/external backup disk not ready; final service architecture capacity not yet validated. Energy: unknown; choose rested, steady, tired, or stop before starting.

## Evidence and shutdown

Evidence path: record local test report path and one safe output line here after the step. Do not paste tokens, real employee records, IP addresses, or passwords.

Tomorrow's first action: W01-S01 until its checkpoint and disk checks pass. A failed critical gate replaces next week's first action; it never disappears from this dashboard.

## Twelve-week progress

| Week | Current step | Last green tag | Gate | Blocker |
| --- | --- | --- | --- | --- |
| 01 | W01-S01 | None | **NOT RUN** | Disk and toolchain |
| 02 | Not started | None | **NOT RUN** | Week 1 and real host |
| 03 | Not started | None | **NOT RUN** | Week 2 |
| 04 | Not started | None | **NOT RUN** | Week 3 |
| 05 | Not started | None | **NOT RUN** | Week 4 |
| 06 | Not started | None | **NOT RUN** | Week 5 |
| 07 | Not started | None | **NOT RUN** | Week 6 |
| 08 | Not started | None | **NOT RUN** | Week 7 |
| 09 | Not started | None | **NOT RUN** | Week 8 |
| 10 | Not started | None | **NOT RUN** | Week 9 |
| 11 | Not started | None | **NOT RUN** | Week 10 |
| 12 | Not started | None | **NOT RUN** | Week 11 |

## Calendar and final design

Development starts Monday 2026-10-26. Week 12 is 2027-01-11 through Friday 2027-01-15; weekend rest ends 2027-01-17 and the 84-day boundary is 2027-01-18. This is the twelve-week goal, not an implementation guarantee. Current position stays W01-S01/not started.

The approved architecture is seven services with JDBC, reactive gateway, Kafka/Avro, Nuxt, Flutter and confidence integration. AWS/Kubernetes execution is future improvement. The V2 guide is the execution reference; open START-HERE first. Quality gates still require actual developer reports. No documentation commit is a green application tag.
