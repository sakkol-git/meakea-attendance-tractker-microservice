# Roadmap V2 progress

## Documentation completion

Final V2 authoring and static validation are complete on docs/roadmap-v2. All twelve weeks use the latest user-approved service design, 448 atomic steps and 96 stones. Every original ID maps forward, Q01–Q14 have complete planned coverage and the extended linter/proof harness passes. This is not application completion; current position remains W01-S01, **Not run**, with no green application tag.

## Confirmed plan

Start Monday 2026-10-26, release rehearsal Friday 2027-01-15, twelve-week boundary Monday 2027-01-18. A=24 and B=24 h weekly; at most 70 percent planned, Weeks 4/8 fifteen percent lower cap. Final planned total is 301.25 h including stated review/mentoring and reading inside durations, below 393.12 h capacity. These are estimates to recalibrate against actual Week-1 pace, not a guaranteed completion date.

Final design retains seven Java services, reactive gateway, JDBC, Kafka/Avro/Registry, Nuxt static client, Flutter and existing confidence integration. ADR 008 supersedes C/React/local-dispatch. AWS/Kubernetes execution is future improvement by latest user instruction; preserve its approved production topology and create no cloud resources.

## Phase ledger

| Phase | Authoring state | Evidence | Runtime state |
| --- | --- | --- | --- |
| 1 architecture review | **PASS** | Every ADR verdict, ADR 008, diagrams/data/event/failure boundaries, explicit initial/final capacity | Implementation **NOT RUN** |
| 2 objective audit | **PASS** | All 182 baseline rows, weekly scorecards/worst 15 and upgrade plan | Original implementation unbuilt |
| 3 shared assets | **PASS** | Standards/review/tests/security/API/commands/golden slice/runbooks/primers | Future tool/script creation and drills **NOT RUN** |
| 4 decomposition | **PASS** | All twelve weeks, two-to-six steps/stone, eight stones/week, owner budgets and old/draft maps | 448 steps **Not run** |
| 5 cross-cutting | **PASS** | Fourteen Q chains, week gates, final scorecards/global/risk/master consistency | Every product scorecard **NOT RUN** |
| 6 tooling | **PASS** | Standard-library semantic checks; 23 isolated broken-fixture proofs | No source/production command run by linter |
| 7 author simulation/checks | **PASS** with limits | Cold-read path, 36 static samples, safe commands, seven Mermaid/three Java examples, dependency/capacity/coverage and final metrics | Native viewers and business/restore/load acceptance **NOT RUN** |

## Decisions and constraints carried forward

- No extra architecture approval is required for the accepted local guide. HR policy/consent is not inferred from design approval.
- Active Java 17 must become target 21, Docker daemon must respond, at least 30 GiB free must precede pulls, and Flutter location/SDK must be established. Current free disk was about 4.8 GiB.
- Actual production host, external encrypted backup/recovery keys, public domain/network reachability and unattended reboot behavior remain unknown. Private Caddy internal TLS avoids removing Secure-cookie protection or assuming a purchased domain.
- Source remains scaffolding. Future ops/CI/contracts/classes are explicitly created in the guide, not claimed installed. Initial developer source baseline protects currently untracked files before CI; this documentation task did not stage or refactor source.
- All critical security/tenant/DB/event/restore checks remain required. If actual learning or work exceeds the itemized estimate, cut Stretch first and record the residual scope/date conflict; no weekends or hidden use of the 30-percent reserve.

## Actual checks and limitations

[V2 validation log](V2-VALIDATION-LOG.md) and [linter proofs](LINTER-PROOFS.md) contain commands/exits and all 36 sampled IDs. Current scaffold Maven validate passed; Maven tests stop at the Avro plugin's nonexistent configured schema directory. Nuxt build/static generation passed; five Python prototype tests passed with one deprecation warning. Docker daemon is unavailable, so real PostgreSQL/Kafka and application security/recovery are **NOT RUN**. No native IntelliJ/Obsidian/Notion visual result is claimed; CommonMark/GFM and text diagram fallback remain enforced.

The complete incoming Markdown input is durably archived in docs/validation/INCOMING-DOCS-V2-SNAPSHOT.json. All 94 source/config fingerprints and eight original non-document staged blobs match the pre-pass snapshot. Documentation commits use an isolated index, no source reset/history rewrite or cloud deployment. Final saved-tree validation is run after the finishing commit so tracked artifacts cannot be missing behind a clean working-tree lint.

## Resuming development

No documentation phase remains intentionally pending. Open [START-HERE](START-HERE.md), [YOU-ARE-HERE](YOU-ARE-HERE.md), then [W01-S01](01-FOUNDATION/BUILD-GUIDE.md). It records dirty work, reviews ignore/secrets and preserves the actual source baseline on the implementation branch before code/CI changes. Do not mark it Run from this authoring pass.

[STEP-ID-MAP](STEP-ID-MAP.md), [DRAFT-ID-MAP](DRAFT-ID-MAP.md) and [finalization map](FINALIZATION-ID-MAP.md) preserve older positions/tags. [CHANGELOG-V2](CHANGELOG-V2.md) contains final metrics and per-week hours; [QUALITY-COVERAGE](QUALITY-COVERAGE.md) contains every senior-quality gate. Five-minute routine: read blocker, check branch/status, choose one next mapped ID, read prerequisite/Expect/Undo, record energy/time and start or stop. End by saving the next exact command and protecting rest days.
