# V2 validation log

## Scope and date

2026-10-23, Asia/Phnom_Penh. These are safe authoring checks against current source. They do not execute the future implementation steps or advance W01-S01. Full Phase 7 sampled dry-run and visual validation remain **NOT RUN**.

## Commands actually run in this correction

| Check | Exact command | Exit | Actual result |
| --- | --- | --- | --- |
| reactor | `mvn -o validate` | 0 | Ten-project reactor BUILD SUCCESS; validation only |
| compose | `docker compose -f infra/docker/docker-compose.yaml config --quiet` | 0 | No output, syntax valid; connectivity/mounts unproved |
| java | `java -version` | 0 | OpenJDK 17.0.19; target 21 not met |
| disk | `df -h .` | 0 | 4.8 GiB available; 30 GiB pull prerequisite not met |
| docker | `docker info --format '{{.ServerVersion}}'` | 1 | Docker socket absent; daemon unavailable |

The baseline earlier recorded Nuxt build success, five passing Python tests and Maven offline test dependency failure. They were not repeated during this correction and are not new implementation evidence.

## Preservation and format checks

Ninety-four source/config fingerprints match the pre-pass snapshot; zero changed. All eight non-document staged index entries match their original blob/mode/stage records. User-staged source was excluded from documentation commits through a separate temporary index. No source reset, history rewrite, image pull or production action was performed.

The existing linter is run before every commit. Latest result before this log: 199 Markdown files, zero errors. Final checkpoint records the later file count. The existing linter has not yet gained the requested V2 semantic checks; its success does not certify atomic scope, capacity, client transport, quality coverage or viewer rendering.

## Pair draft problems requiring repair

- First-time primer reading is not yet measured against the provisional one-hour allowance. Recompute per contributor; do not reread already-known topics as new work or assume unproven prior knowledge.
- Monday–Wednesday feature/learning distribution may overflow the 12-hour window. Thursday buffer and Friday deploy must not introduce new concepts. A total below 16.8 is insufficient proof.
- Exact Nuxt refresh-cookie/CSRF source/test steps and own-profile read behavior are incomplete in the draft auth ladder. Add ordered atomic work or carry a critical failed gate; examples alone do not implement security.
- Some spike/test/CI actions still require their actual version-specific commands and validated expected output. Replace inspect findings with exact recorded commands before calling a step beginner-ready. Never call generic Maven success proof of a Registry/network assertion.
- Three-source-file steps plus the weekly evidence file can exceed the preferred file count. Split or use a separately committed evidence step; do not silently omit evidence from the audit.
- Opening-week private deployment/restore needs an executable manifest/script sequence; currently some scripts are introduced in Week 2. Fix dependency order instead of executing an absent future target.
- W02-S19 refers to unimplemented web transport as a critical blocker. That is honest but cannot remain the final execution plan: add its implementation/test steps before that review.

## Outstanding required validation

Run the linter extension negative fixtures only after implementing them. Sample three finalized steps per week, check safe command/path/output realism and label actual step execution honestly. Parse Mermaid diagrams and perform IntelliJ/Obsidian/Notion checks separately. Verify all MVP/Q mappings, Stretch independence, daily/weekly caps, light Weeks 4/8, buffer and Friday restore/deploy. No result is predeclared.
