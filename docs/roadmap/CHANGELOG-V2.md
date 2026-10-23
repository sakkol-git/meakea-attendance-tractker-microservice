# Roadmap V2 change log

## Checkpoint status

Partial second pass, not a completed upgrade or product release. Final architecture correction is committed; Weeks 1–2 have 65 atomic draft steps replacing 32 original steps, but known dependency, client-security, first-time-reading and daily-cap gaps remain. Weeks 3–12 retain 150 original steps awaiting decomposition. No implementation has been completed by editing these documents.

## What changed and why

The user accepted the latest Architecture as final. ADR 008 supersedes C/React/local-dispatch decisions with seven services, JDBC, reactive gateway, Kafka/Avro and Nuxt. The subsequent latest user reply defers AWS/Kubernetes execution as future improvement; the complete production design is preserved in four bounded parts. Shared quality assets and the golden slice now use the final service boundaries.

The original step audit found missing undo instructions in every step, coupled changes and generic evidence/recovery actions. The first pair now states target paths, narrower outcomes, Common mistake, Undo, Senior habit and explicit status. This is a draft calibration, not proof that all steps are ready or fit the daily learning rhythm.

## Before and current draft metrics

| Metric | Before | Current checkpoint |
| --- | --- | --- |
| Total steps | 182 | 215 |
| Average upper duration in minutes | 57.53 | 50.93 |
| Maximum stated duration in minutes | 60 | 60 |
| Steps over 90 minutes | 0 | 0 |
| Steps missing Expect field | 0 | 0 |
| Steps missing Undo field | 182 | 150 |

These are textual upper-duration statistics, not actual implementation timings. They omit separately budgeted reading and cannot prove one-concept/three-file scope. Averages fall because the opening tasks are decomposed; this does not imply coding became faster. The untouched 150 steps still lack V2 fields. Full Q traceability gaps are tracked in QUALITY-COVERAGE; full implementation status is **NOT RUN** for all Q-items.

## Capacity at checkpoint

Week 1 draft scheduled A=14.5, B=10.5 hours; Week 2 A=15.25, B=7.75. Each includes a provisional 1-hour reading allowance; that allowance and daily distribution remain unvalidated. Other week READMEs still report old budgets; do not treat them as final-service estimates. Planned limit is 16.8 each, light Weeks 4/8 14.28. Current final-service/quality demand estimate is 489.5 combined versus 393.12 available, A bottleneck. No schedule guarantee is made.

Optional polish, extra reports, push/SSE and unadmitted refactors are proposed Stretch cuts; they are not completed cuts across every week yet. Cloud deployment is deferred by explicit latest user instruction. Security tests and restore stay mandatory.

## Traceability and preservation

STEP-ID-MAP inventories all 182 old IDs; first 32 rows have new target mappings and remaining 150 are explicitly pending conversion. Existing tags stay valid and unchanged; none existed as green application tags in the inspected baseline. W01-S01 remains current and unstarted.

Original per-step bodies, durations and paths are preserved in docs/validation/ROADMAP-V1-BASELINE.json, including incoming working-tree documentation content. First-pair structured draft is in docs/validation/ROADMAP-V2-PAIR-12-DRAFT.json. These data files support the audit and resumption; they are not application source or a second executable roadmap.

## Validation and remaining work

[V2 validation log](V2-VALIDATION-LOG.md) records actual safe commands, environment blockers and pair defects. Existing format lint passes before commits; new V2 linter checks and their broken-fixture proofs are **NOT RUN**. Full 36-step sampled dry-run, viewer checks, dependency/capacity/quality validation and final metrics remain outstanding. [Progress](PROGRESS-V2.md) names the exact resume sequence.
