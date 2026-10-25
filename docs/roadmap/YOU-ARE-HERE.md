# You are here

## Capacity inputs

**Capacity inputs:** A=24; B=24; fraction=0.70; light=0.85.

User confirmed no change on this second pass. These inputs are authoritative; weekly planned totals include reading, self-review and recovery. Start date is 2026-10-26 per next-week instruction; host readiness remains unconfirmed.

## Current position

Current week: 02. Current step: W02-S24. Step state: **Not run**. Gate: **NOT RUN**.

Next action: record the intended production computer and validate the private service manifest (W02-S24 to W02-S25), then complete secret, backup, restore and host-gate work through W02-S35.

Last green application tag: bd53438 (feat(w01): w01-s07). Documentation commits are not green application tags. Blockers: external backup disk and real host readiness unconfirmed; local Java 21, Spring Data JPA, reactive gateway and auth test suites are verified green. Energy: steady.

## Evidence and shutdown

Evidence path: `docs/validation/W01-EVIDENCE.md` and `docs/validation/W02-EVIDENCE.md`. Gateway 5/5 tests pass; identity-org 13/13 tests pass; live auth smoke exercises native tokens, refresh family rotation, brute-force throttling, Problem JSON and browser CSRF cookies. The OpenAPI ledger records 6 implemented operations and 152 pending. Web live authentication and mobile login, restore and logout pass through the gateway; physical-device secure-storage acceptance remains open.

Tomorrow's first action: W02-S24 to W02-S25 (host inventory and private service manifest validation). A failed critical gate replaces next week's first action; it never disappears from this dashboard.

## Twelve-week progress

| Week | Current step | Last green tag | Gate | Blocker |
| --- | --- | --- | --- | --- |
| 01 | W01-S38 | bd53438 | **PASS** | Completed local baseline |
| 02 | W02-S24 | bd53438 | **NOT RUN** | Physical devices, real host and external backup |
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

Development starts Monday 2026-10-26. Week 12 is 2027-01-11 through Friday 2027-01-15; weekend rest ends 2027-01-17 and the 84-day boundary is 2027-01-18. This is the twelve-week goal, not an implementation guarantee. Current position is W02-S24; the six-operation gateway authentication slice is complete and the operational half of Week 2 remains open.

The approved architecture is seven services with Spring Data JPA, reactive gateway, Kafka/Avro, Nuxt, Flutter and confidence integration. AWS/Kubernetes execution is future improvement. The V2 guide is the execution reference; open START-HERE first. Quality gates still require actual developer reports. No documentation commit is a green application tag.
