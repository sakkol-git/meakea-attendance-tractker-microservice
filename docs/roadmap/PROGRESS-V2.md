# Roadmap V2 progress

## Resume checkpoint

Branch docs/roadmap-v2, created from the roadmap branch. This is a partial second pass; the requested seven-phase upgrade is not complete. Architecture correction is committed. Weeks 1–2 contain 65 atomic draft steps and mapped old IDs, but are not beginner-ready until the explicit draft gaps below are repaired. Do not execute unconverted monolith/React instructions from Weeks 3–12.

## Confirmed facts and latest user steering

- Current position W01-S01, not started. A=24 and B=24 h/week unchanged; start date unknown; intended host not ready.
- Latest Architecture.md is the final system design: seven Java services, reactive gateway, JDBC, Kafka KRaft, Avro/Registry, Nuxt, Flutter and Python confidence service. User explicitly superseded the prior C/React decision.
- AWS/Kubernetes were briefly required inside twelve weeks, then the latest answer said to skip for now as future improvement. Preserve the final cloud topology, defer cloud execution, create no paid resources. ADR 008 records this order.
- One capacity input remains in YOU-ARE-HERE.md: A=24; B=24; fraction=0.70; light=0.85. Normal limit 16.8 each; Weeks 4/8 14.28 each. No weekends or secret use of the 30 percent reserve.
- Current final-service/quality planning demand estimate is 489.5 combined h versus 393.12 available, A bottleneck. It is an estimate to refine, not measured velocity or a funded schedule. No scope/hours/deadline change is approved.

## Phase ledger

| Phase | State | Evidence | Remaining action |
| --- | --- | --- | --- |
| 1 architecture | **PASS** design review | Final design preserved, ADR 008, capacity/ADR/diagram/impact review | Propagate through every weekly/shared dependent file |
| 2 measurement | **PASS** baseline audit | All 182 old steps scored; worst 15; upgrade plan | Recompute final atomic estimates and close trace gaps |
| 3 shared assets | **CONDITIONAL PASS** drafts | Q01–Q14 rules, golden slice, tests/security/API/commands and six runbooks | Complete version-specific commands and tie to final enforcing IDs |
| 4 decomposition | **NOT RUN** to completion | Weeks 1–2 draft 65 steps; 150 legacy steps remain | Repair pair 1–2, then pairs 3–4 through 11–12 |
| 5 cross-cutting | **NOT RUN** to completion | First-pair Enterprise tasks and partial Q matrix | Every later gate, scorecards/global/risk/master consistency |
| 6 tooling | **NOT RUN** | Existing linter unchanged and retained | Add V2 rules and prove each with broken temporary fixtures |
| 7 validation | **NOT RUN** to completion | Safe authoring checks and interim change metrics | Cold start, 36 sampled steps, dependency/capacity/coverage/viewers and final report |

## Completed commits

- e7ebd07: original C reconciliation and tenant defense; topology portion superseded by latest user decision.
- 3e9cf9a: all-182-step baseline audit and quality upgrade plan.
- 3e411c1: shared standards/golden-slice first drafts.
- c787a08: accept final service design, preserve latest full text, ADR 008 and cloud deferral.
- 55c31c1: reconcile shared assets to JDBC/Kafka/Nuxt and broker-aware recovery.
- Next checkpoint commit contains the first-pair draft, partial coverage, ID map, validation log and explicit remaining work; its hash can be read with git log.

## Resume first: repair Weeks 1–2 before calling the pair complete

1. Read ARCHITECTURE-REVIEW-V2, V2-VALIDATION-LOG, STEP-ID-MAP and the two new week READMEs. Keep final service topology; do not restore C to make hours fit.
1. Audit every draft step against the atomic specification. Some three-source-file actions also write weekly evidence: split to keep the preferred count. Replace generic output checks with actual version-specific validator/mock/CI/Registry commands after an inspect/spike. Keep each spike at 45 minutes, not 60.
1. Fix first-time reading and daily topic allocation. Current README totals include provisional 1-hour reading each, which is not yet validated. Feature/quality/reading work must fit Monday–Wednesday without moving new concepts to Thursday or Friday. Do not declare capacity passed just because total is below 16.8.
1. Add explicit own-profile read and Nuxt refresh-cookie/CSRF implementation/tests before W02-S19 review. W02-S18 examples and the mobile step do not implement browser security. Split auth code/test paths into small red/green/negative actions while retaining the approximately twenty-step calibration.
1. Repair Week-1 private deployment/restore ordering. The draft currently refers to candidate/restore behavior before all executable scripts/manifests are introduced in Week 2. Add or reorder atomic creation steps; never invoke an absent future target or call shell syntax a restore pass.
1. Confirm first-pair CI actually builds the pinned candidate images and executes build/test/format/static/contract/dependency/image/secret checks. A missing remote/runner, image or scan DB is a blocker. No guessed plugin/action/image version is accepted without compatibility evidence.
1. Recompute pair owner/day/reading budgets, stone counts, gate tasks and ID/tag maps after corrections. All original ID rows remain mapped; old tags are never deleted or moved. Lint, update this file and commit the repaired pair.

## Then finish pairs in order

1. Weeks 3–4: JDBC golden slice, org/employee/shift/holiday baseline, shared tenant endpoint suite, non-owner RLS and pool/rollback tests, package/Maven architecture tests, attendance check-in/out/correction decomposition and retry/race rules. Replace optional Builder work with required quality; keep Week 4 light.
1. Weeks 5–6: real Kafka/Avro publisher/outbox/inbox/offset/retry/dead-letter/replay/bootstrap steps, notifications, exact leave ledger, approval/cancellation/concurrent reservation and attendance conflict. Never substitute local in-process handlers for Kafka.
1. Weeks 7–8: request approval/fulfillment, actor/tenant/race tests and secure files if included; Nuxt/Flutter integration/accessibility/offline failure states. Optional patterns/push/style first to Stretch; Week 8 light.
1. Weeks 9–10: reporting projections/export/audit, confidence/event integration estimate, Prometheus/Grafana/correlation/lag/business metrics/uptime alerts; repeatable seven-workload deployment/rollback/backup/clean-host restore/reboot/disk/internet runbooks. Cloud execution remains deferred.
1. Weeks 11–12: security/load/replay/device/recovery evidence, all-Q readiness scorecards with honest limitations, semantic release/changelog and freeze/demo. No unbuilt feature is described as done.
1. After every pair update README stone/hour tables, all BUILD parts, VERIFY Enterprise/Minimum gates and diagnostics only for new failures; extend ID map, recompute budgets, run linter, update progress and commit.

## Cross-cutting completion list

1. Reconcile every candidate in V2-REMAINING-REFERENCES.md; it currently lists 41 files. Preserve historical explanations, correct normative execution guidance. Also review the full architecture impact matrix; search candidates are not exhaustive.
1. Finish QUALITY-COVERAGE with all fourteen introducing weeks, actual enforcing IDs and literal gate tasks. Current rows intentionally identify missing Week-3/4/5/9/10/12 enforcement. Do not fill gaps with imaginary IDs.
1. Update GLOBAL-CHECKLIST with stable old requirements and new Q/step IDs, nothing prechecked. Update RISK-REGISTER with quality overload, split overhead, service/broker resource/recovery risk, header-trust limits and host blockers.
1. Update ROADMAP, MILESTONE-MAP, PATTERNS-LEARNING-PATH, START-HERE and DEFINITION-OF-DONE to final services/Nuxt/Kafka and new mappings. Link every new shared asset from both folder README and START-HERE. Preserve pattern admission, Thursday buffer, two rest days and Friday deploy.
1. Keep FORMAT-GUIDE and FORMAT-SELFTEST's new Common mistake/Undo/Senior habit fields; current linter does not enforce them yet.

## Tooling extension still required

Extend tools/docs-lint.py using standard library only; retain existing format checks. Enforce every step's Why/Do/Expect/Common mistake/Undo/Senior habit/Check/If it fails/Status, duration at most 90, unique sequential week IDs and all map targets, Enterprise/Minimum gates, README hours against the single dashboard input, complete Q-to-week/ID/gate traceability. Vague-verb title heuristic is a warning. Other false-positive heuristics remain warnings with reasons. Prove each rule fails an isolated broken temp fixture and passes real docs. Do not add a skip flag to hide unconverted weeks.

## Final validation still required

Cold-start from START-HERE/dashboard to W01-S01 within fifteen minutes; fix unexplained terms. Three selected steps per week, risky/middle/failure, checked against actual source/commands and safe outputs. Execute only safe read/build/test/config commands; do not provision/publish, overwrite data, delete volumes or install a large stack to get a checkmark. Mark whole-step Run only if all its result is actually obtained, otherwise Not run with the partial-command evidence.

Check prerequisites, Stretch independence, every MVP feature mapping, all fourteen Q chains, every week/day/reading budget, lighter Weeks 4/8 and no buffer/rest/deploy violations. Parse Mermaid and test viewers honestly; no Notion/Obsidian/IntelliJ rendering was performed. Run full extended lint with zero errors and recorded warnings. Replace interim CHANGELOG-V2 metrics with final before/after step/scope/field/Q/hour metrics and issue the requested eleven-part final report.

## Preservation and validation evidence

Safe current commands and actual output are in V2-VALIDATION-LOG.md. mvn -o validate passed all ten modules; Compose config syntax passed. Active Java is 17.0.19, target 21 unmet; Docker daemon socket absent; free disk 4.8 GiB. No containers pulled or production resources created.

Ninety-four source/config fingerprints and eight non-document staged entries matched the pre-pass snapshot with zero changes. Temporary-index docs commits exclude user-staged source. Never blanket-stage or reset source. Original 182-step bodies/durations are durably preserved in docs/validation/ROADMAP-V1-BASELINE.json; first-pair structured draft is in docs/validation/ROADMAP-V2-PAIR-12-DRAFT.json. Existing format linter passed 202 Markdown files before this checkpoint update; run again before committing.

Current draft metrics: 215 steps total, average upper duration 50.93 minutes, max 60, none over 90, none missing Expect, 150 still missing Undo. These are interim text metrics, not actual coding times. Current position remains W01-S01/not started; no green application tag, implementation completion or project guarantee.

## First file tomorrow and daily routine

Open this file first to continue the documentation upgrade. For application work, remain at W01-S01 until its prerequisites pass. Five-minute routine: read dashboard and blocker for one minute, check branch/status for one minute, choose the single next mapped step for one minute, confirm its prerequisite/Expect/Undo for one minute, record available energy/time and start or stop for the final minute. No failed critical gate disappears overnight.

## Finalization pair 01–02

Weeks 01–02 now contain the final-service atomic sequence, eight stones, explicit fields/status, computed owner budgets, required negative paths and Enterprise/Minimum gates. All implementation remains Not run. First-time reading is included in step durations; allocated review/mentoring is separate and counted. Remaining validation and global/tooling reconciliation are subsequent phases.

## Finalization pair 03–04

Weeks 03–04 now contain the final-service atomic sequence, eight stones, explicit fields/status, computed owner budgets, required negative paths and Enterprise/Minimum gates. All implementation remains Not run. First-time reading is included in step durations; allocated review/mentoring is separate and counted. Remaining validation and global/tooling reconciliation are subsequent phases.
