# You are here

## Capacity inputs

**Capacity inputs:** A=24; B=24; fraction=0.70; light=0.85.

User confirmed no change on this second pass. These inputs are authoritative; weekly planned totals include reading, self-review and recovery. Start date is 2026-10-26 per next-week instruction. The Acer Nitro 5 is now the intended self-host, while its readiness remains unverified.

## Current position

Current week: 02. Current step: W02-S26 (W02-S25 manifest validation completed; W02-S24 host inventory recorded). Step state: **Blocked on key rotation, dependency audit and Acer access**. Gate: **NOT RUN**.

Next action: complete W02-S26 key rollover and resolve the web dependency audit; execute guarded W02-S27–S29 drafts on the designated Acer and encrypted external disk when accessible. The user requested local work now and explicitly left the host gate blocked.

Last green application tag: bd53438 (feat(w01): w01-s07). Documentation commits are not green application tags. Blockers: the Acer's direct physical boot/runtime, free disk/health, uptime policy and network facts are unverified; external backup disk and physical-device credentials remain open. A web dependency audit reports seven high-severity findings pending triage. Local Java 21, Spring Data JPA, reactive gateway and identity-org tests remain verified for their tested scope. Energy: steady.

## Evidence and shutdown

Evidence path: `docs/validation/W01-EVIDENCE.md` and `docs/validation/W02-EVIDENCE.md`. The current identity suite has 54 unit/HTTP cases, including configured-issuer regression; 17 gateway tests and six Docker-backed identity integration tests passed in the local Maven verifier. W02-S07 gateway resource-server and caller identity sanitization verified green; W02-S14 Gate 2 method security, role mapping and forged header resistance verified green; W02-S15 tenant boundary isolation verified green across all auth routes; W02-S22 complete auth test ladder verified green; W02-S24 intended production computer inventory and hardware sizing analysis recorded (BLOCKED on physical host command verification); W02-S25 private service manifest and Caddy TLS edge proxy syntax verified green. The OpenAPI ledger records 8 implemented operations and 150 pending; forgot/reset password remain blocked on tenant resolution, token lifetime and delivery. Web checks pass 89 unit and 12 browser scenarios, including the role-gated synthetic audit view; mobile analysis and 152 tests pass with one live test skipped by default. Both clients use the implemented profile/password routes. Audit remains provider-pending, and physical-device secure-storage acceptance remains open.

Tomorrow's first action: finish W02-S26 key rotation and the failed web dependency audit, then obtain direct Acer/external-disk evidence for W02-S24 and W02-S27–S35. Local deploy/backup/restore scripts are drafts with syntax/config checks, not completed drills. A failed critical gate replaces next week's first action.

## Progress percentage

- **Overall 12-Week Roadmap Progress:** **14.7%** (66 of 448 planned steps verified; 1 of 12 weekly quality gates passed).
- **Week 02 Execution Progress:** **74.3%** (26 of 35 steps verified; W02-S25 completed; W02-S26 active).
- **OpenAPI Contract Coverage:** **5.1%** (8 of 158 contract operations implemented and verified in the authentication/profile slice).

## Twelve-week progress

| Week | Current step                            | Progress | Last green tag | Gate        | Blocker                                                                                    |
| ---- | --------------------------------------- | -------- | -------------- | ----------- | ------------------------------------------------------------------------------------------ |
| 01   | W01-S38                                 | 100%     | bd53438        | **PASS**    | Completed local baseline                                                                   |
| 02   | W02-S26                                 | 74%      | bd53438        | **NOT RUN** | Key rollover, web audit, Acer, physical devices and external backup/restore                |
| 03   | Not started                             | 0%       | None           | **NOT RUN** | Week 2                                                                                     |
| 04   | Not started                             | 0%       | None           | **NOT RUN** | Week 3                                                                                     |
| 05   | Not started                             | 0%       | None           | **NOT RUN** | Week 4                                                                                     |
| 06   | Not started                             | 0%       | None           | **NOT RUN** | Week 5                                                                                     |
| 07   | Not started                             | 0%       | None           | **NOT RUN** | Week 6                                                                                     |
| 08   | Not started                             | 0%       | None           | **NOT RUN** | Week 7                                                                                     |
| 09   | Not started                             | 0%       | None           | **NOT RUN** | Week 8                                                                                     |
| 10   | Not started                             | 0%       | None           | **NOT RUN** | Week 9                                                                                     |
| 11   | Not started                             | 0%       | None           | **NOT RUN** | Week 10                                                                                    |
| 12   | Not started                             | 0%       | None           | **NOT RUN** | Week 11                                                                                    |

## Calendar and final design

Development starts Monday 2026-10-26. Week 12 is 2027-01-11 through Friday 2027-01-15; weekend rest ends 2027-01-17 and the 84-day boundary is 2027-01-18. This is the twelve-week goal, not an implementation guarantee. Current position is W02-S26 with W02-S24 still blocked: the Acer Nitro 5 target is selected, but direct readiness evidence is open. The eight-operation authentication/profile/password slice has focused evidence; password recovery and the operational half of Week 2 remain open.

The approved architecture is seven services with Spring Data JPA, reactive gateway, Kafka/Avro, Nuxt, Flutter and confidence integration. ADR 010 requires JWT validation at the gateway and each Java service; ADR 011 requires Spring Security OAuth2 Resource Server instead of custom JWT filters. AWS/Kubernetes execution is future improvement. The V2 guide is the execution reference; open START-HERE first. Quality gates still require actual developer reports. No documentation commit is a green application tag.
