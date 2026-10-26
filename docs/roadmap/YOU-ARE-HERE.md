# You are here

## Capacity inputs

**Capacity inputs:** A=24; B=24; fraction=0.70; light=0.85.

User confirmed no change on this second pass. These inputs are authoritative; weekly planned totals include reading, self-review and recovery. Start date is 2026-10-26 per next-week instruction; host readiness remains unconfirmed.

## Current position

Current week: 02. Current step: ADR-010/011 resource-server verification for W02-S07, W02-S14, W02-S15 and W02-S22. Step state: **Not run**. Gate: **NOT RUN**.

Next action: repeat W02-S07, W02-S14, W02-S15 and W02-S22 to verify the gateway and identity-org Spring Security resource-server chains independently validate the same bearer JWT with no trusted identity headers. Do not add custom JWT filters. After that, resume W02-S24 to W02-S35 for host, secret, backup, restore and gate work.

Last green application tag: bd53438 (feat(w01): w01-s07). Documentation commits are not green application tags. Blockers: the complete ADR-010/011 two-gate validator/routing matrix and the other five service resource-server gates are unverified; external backup disk and real host readiness are unconfirmed. Local Java 21, Spring Data JPA and the earlier reactive gateway/auth suites remain verified evidence for their tested scope. Energy: steady.

## Evidence and shutdown

Evidence path: `docs/validation/W01-EVIDENCE.md` and `docs/validation/W02-EVIDENCE.md`. All 40 current identity unit/HTTP tests, five gateway authorization tests and six Docker-backed identity integration tests pass, including the profile/password PostgreSQL scenarios and corrected browser/native refresh/logout token selection. The OpenAPI ledger records 8 implemented operations and 150 pending; forgot/reset password remain blocked on tenant resolution, token lifetime and delivery. The complete Postman API collection covers all 158 operations across 14 categories with dynamic extraction scripts. Web live authentication and mobile login, restore and logout pass through the gateway; physical-device secure-storage acceptance remains open.

Tomorrow's first action: revise and rerun W02-S07, then W02-S14/S15/S22, for the two-gate Spring Security resource-server boundary. A failed critical gate replaces next week's first action; it never disappears from this dashboard.

## Twelve-week progress

| Week | Current step | Last green tag | Gate | Blocker |
| --- | --- | --- | --- | --- |
| 01 | W01-S38 | bd53438 | **PASS** | Completed local baseline |
| 02 | ADR-010/011 verification before W02-S24 | bd53438 | **NOT RUN** | Complete dual-gate resource-server matrix, physical devices, real host and external backup |
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

Development starts Monday 2026-10-26. Week 12 is 2027-01-11 through Friday 2027-01-15; weekend rest ends 2027-01-17 and the 84-day boundary is 2027-01-18. This is the twelve-week goal, not an implementation guarantee. Current position is the ADR-010/011 resource-server verification before W02-S24; the eight-operation authentication/profile/password slice has focused evidence, while password recovery, the complete two-gate matrix and operational half of Week 2 remain open.

The approved architecture is seven services with Spring Data JPA, reactive gateway, Kafka/Avro, Nuxt, Flutter and confidence integration. ADR 010 requires JWT validation at the gateway and each Java service; ADR 011 requires Spring Security OAuth2 Resource Server instead of custom JWT filters. AWS/Kubernetes execution is future improvement. The V2 guide is the execution reference; open START-HERE first. Quality gates still require actual developer reports. No documentation commit is a green application tag.
