# Quality coverage V2

## Meaning of coverage

Every row has an introducing week, existing enforcing step IDs and a literal unchecked Q-item gate. This is complete planned traceability, not implemented quality: every implementation item remains **NOT RUN** until actual reports exist. Shared golden-slice checks are repeated for every later feature delta; they are not replaced by a coverage percentage or a prose promise.

| Quality | Introducing week | Enforcing step IDs | Verifying gate | Execution |
| --- | --- | --- | --- | --- |
| Q01 | 01 | W01-S10, W01-S14 | [Q01 checkbox](01-FOUNDATION/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q02 | 01 | W01-S16, W01-S17 | [Q02 checkbox](01-FOUNDATION/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q03 | 03 | W03-S07, W03-S08, W03-S11, W03-S12 | [Q03 checkbox](03-ORGANIZATION-AND-ADMIN/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q04 | 03 | W03-S25, W03-S26, W03-S27, W03-S34 | [Q04 checkbox](03-ORGANIZATION-AND-ADMIN/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q05 | 02 | W02-S10, W02-S11, W02-S12, W02-S13, W02-S14, W02-S20 | [Q05 checkbox](02-IDENTITY-AND-HOST/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q06 | 02 | W02-S05, W02-S26 | [Q06 checkbox](02-IDENTITY-AND-HOST/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q07 | 03 | W03-S01, W03-S02, W03-S03, W06-S28 | [Q07 checkbox](03-ORGANIZATION-AND-ADMIN/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q08 | 04 | W04-S28, W05-S10, W05-S11, W05-S12, W05-S13, W05-S14, W05-S15 | [Q08 checkbox](05-OUTBOX-AND-NOTIFICATIONS/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q09 | 01 | W01-S08, W01-S22, W01-S23, W05-S16, W05-S17, W05-S18 | [Q09 checkbox](01-FOUNDATION/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q10 | 03 | W03-S28, W03-S29, W03-S30 | [Q10 checkbox](03-ORGANIZATION-AND-ADMIN/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q11 | 02 | W02-S17, W09-S22, W09-S23, W09-S24, W09-S25, W09-S26, W09-S27, W09-S28, W09-S29, W09-S30 | [Q11 checkbox](09-REPORTING-AUDIT-OBSERVABILITY/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q12 | 01 | W01-S32, W01-S33, W01-S34 | [Q12 checkbox](01-FOUNDATION/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q13 | 10 | W10-S02, W10-S03, W10-S05, W10-S06, W10-S08, W10-S09, W10-S11, W10-S12, W10-S14, W10-S15, W10-S17, W10-S18, W10-S20, W10-S21, W10-S23, W10-S24, W10-S26, W10-S27, W10-S29, W10-S30, W10-S32, W10-S33, W10-S35, W10-S36, W12-S28 | [Q13 checkbox](10-OPERATIONS-AND-RESTORE/VERIFY-AND-GATE.md) | **NOT RUN** |
| Q14 | 01 | W01-S20, W01-S21, W01-S37 | [Q14 checkbox](01-FOUNDATION/VERIFY-AND-GATE.md) | **NOT RUN** |

## Coverage gate semantics

Q08 has a minimal owner-intent skeleton in Week 3, retry correctness in Week 4 and full Kafka crash/replay proof in Week 5. Q13 has early safe backup/restore in Weeks 1–2 and complete operations enforcement in Weeks 10–12. Q11 starts in Week 2 and completes Week 9. Q09 critical code coverage is 80 percent lines/70 percent branches only on nonempty auth/attendance/leave domain/application packages, plus required real PostgreSQL/Kafka tests.

No Q-item is a planned traceability gap after this mapping. Missing or failing runtime evidence remains **NOT RUN** or **FAIL**, never an implied pass. Week-11/12 all-Q scorecards must be populated by the developers. The linter checks IDs, introducing week and actual gate task; it cannot certify test quality, privacy policy approval or real uptime.
