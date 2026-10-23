# V2 validation log

## Scope and result

Documentation finalization checked on 2026-10-23, Asia/Phnom_Penh. All twelve weekly plans, 448 step blocks, 96 stones, old/draft/finalization mappings and fourteen quality chains are authored and statically checked. Implementation remains **Not run** for all 448 steps; source is mostly scaffolding. A command executed against the current scaffold does not complete a future feature step.

## Commands actually executed

| Command | Exit | Actual result |
| --- | --- | --- |
| `mvn -o validate` | 0 | Ten-project reactor BUILD SUCCESS; POM validation only. |
| `mvn -o test` | 1 | Stops in Avro schema generation: configured root contracts/events does not exist; no backend behavior test ran. |
| `docker compose -f infra/docker/docker-compose.yaml config --quiet` | 0 | Syntax exits zero; known mount/listener/schema-source corrections still need development. |
| `docker info --format '{{.ServerVersion}}'` | 1 | Docker daemon socket absent. |
| `npm --prefix web run build` | 0 | Nuxt 4.6.1 production build completed. |
| `PYTHONDONTWRITEBYTECODE=1 .venv/bin/python -m pytest -q -p no:cacheprovider` | 0 | Five confidence tests passed; one Starlette/httpx deprecation warning. Run in ml/confidence-service. |
| `npm --prefix web run generate` | 0 | Four routes prerendered; web/.output/public/index.html exists. |
| `node docs/validation/check-mermaid.mjs /tmp/hrflow-doc-validation /tmp/hrflow-v2-final-diagrams.json` | 0 | Seven of seven Mermaid diagrams parsed; no native-viewer rendering claim. |
| `javac --release 21 and java on three retained PatternExample sources in isolated temp directories` | 0 | builder checks passed; adapter checks passed; specification checks passed. JDK 23 compiler/runtime with Java 21 target. |

Installed tools were inspected: Maven 3.9.16, Java 17.0.19 active, Node 26.3.0/npm 11.16.0 and Postman CLI 1.75.0. Flutter command is absent from PATH and no Flutter project is present in this audit. Java target 21 remains unmet; the three isolated examples used the existing JDK 23 compiler with --release 21, not the application toolchain. Free disk was about 4.8 GiB, Docker daemon unavailable. No dependency images, Java/Flutter installation, production action or source refactor was performed.

The latest Maven test failure is the real configured Avro sourceDirectory defect, not a fabricated test assertion or the earlier uncached-dependency failure. Week 1 repairs the source directory, mount/listener paths, selected Java/tool versions and source checkpoint before meaningful backend tests can pass. Nuxt build/generation proves the existing scaffold only; five Python tests prove the existing standalone prototype only.

## Cold-start simulation

Read-through path: START-HERE first fifteen-minute actions, dashboard current-position/capacity block, decision summary, Week-1 overview capacity and BUILD-GUIDE W01-S01. All links resolve. Git/branch/commit/staged/untracked/tenant/JDBC/Kafka/Avro/Nuxt are explained inline or in the linked glossary/primer. W01-S01 is reachable in three navigation links; its only immediate commands are safe status/diff/ignore checks. Basic Java/SQL/Git or Flutter skill is an explicit entry assumption, not invented prior knowledge.

The required path is comfortably below a fifteen-minute reading budget when using those named sections; this is an author cold-read/navigation simulation, not a timed independent junior usability study. The forty-five-minute source-checkpoint step is separate from the time to reach it. It saves the currently untracked scaffold on the developer's implementation branch before CI; this documentation run has not made that source commit.

## Thirty-six sampled steps

Each week contributes its riskiest, a middle and a failure-path step. Existing owner roots/CLI structure and proposed target names were checked against actual files, canonical contracts and creation order. Unbuilt test/script leaves are explicitly proposed and are not falsely reported as existing. Destructive deploy/reboot/restore/load/failure commands were not executed. Detailed samples are preserved in docs/validation/V2-SAMPLED-STEPS.json.

| Week | Selection | Final step | Static finding | Whole-step execution |
| --- | --- | --- | --- | --- |
| 01 | risky | W01-S05 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 01 | middle | W01-S17 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 01 | failure | W01-S31 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 02 | risky | W02-S20 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 02 | middle | W02-S19 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 02 | failure | W02-S12 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 03 | risky | W03-S34 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 03 | middle | W03-S11 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 03 | failure | W03-S27 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 04 | risky | W04-S05 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 04 | middle | W04-S14 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 04 | failure | W04-S12 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 05 | risky | W05-S14 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 05 | middle | W05-S28 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 05 | failure | W05-S18 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 06 | risky | W06-S11 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 06 | middle | W06-S20 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 06 | failure | W06-S28 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 07 | risky | W07-S11 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 07 | middle | W07-S26 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 07 | failure | W07-S30 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 08 | risky | W08-S11 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 08 | middle | W08-S05 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 08 | failure | W08-S15 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 09 | risky | W09-S11 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 09 | middle | W09-S32 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 09 | failure | W09-S35 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 10 | risky | W10-S20 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 10 | middle | W10-S17 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 10 | failure | W10-S21 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 11 | risky | W11-S14 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 11 | middle | W11-S08 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 11 | failure | W11-S03 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 12 | risky | W12-S02 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 12 | middle | W12-S20 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |
| 12 | failure | W12-S12 | Owner/command and proposed creation/denial contract checked; runtime needs implementation/prerequisites | **Not run** |

## Findings repaired during simulation

- The known Avro source/listener repair now precedes Registry compatibility/round-trip; the department write follows its migration; every feature stone ends on a green/negative/quality result, never a red assertion whose implementation is in another stone.
- Client/credential/accessibility checks target client tests, and hardening checks use existing regressions/acceptance rather than inventing production Java service classes.
- First source checkpoint protects actual untracked scaffold before CI. Week-1 tool/test-runner, private manifest, generic image builder, deploy/smoke and disposable restore are created before use.
- Five business-owner JDBC/YAML/context alignments are explicit in Week 3. Nuxt uses a tested static generation artifact and the Java browser-cookie/CSRF adapter, not an unplanned server runtime.
- Exact script names, ignored target config, private internal TLS/Secure cookies, non-owner RLS, one-active-relay ordering and complete offline source/image artifacts are covered before their later gates.
- Tests report actual semantic results. Shell syntax/config validation, Maven validate and empty selectors are never passed off as domain/recovery evidence.

## Dependency capacity and feature checks

All 182 original IDs resolve; the 215-step intermediate draft and the later ten-ID ordering correction are mapped too. No old tag was moved/deleted; no green application tags exist. Required paths are introduced before invocation; later-feature stubs remain private/denied. Optional patterns, styling, push/SSE, richer report variants and cloud execution are not next-week prerequisites. Shared golden-slice tests are rerun for each route delta, not accepted from an unrelated feature's green result.

Every week has eight stones with two-to-six steps, Thursday buffer, Friday private redeploy/restore and weekend rest. Reading is inside durations, extra review/mentoring is counted once, each owner is below its 70-percent limit and light Weeks 4/8 use the 14.28-hour cap. Monday–Wednesday feature work is at most 12 hours each (10.2 in light weeks); A can place scheduled mentoring beside fixtures only within that remaining window or use Thursday for familiar review. Actual unfamiliar-topic count/velocity is checked by the developers, and overruns remain a visible schedule risk.

All stable MVP IDs and all 448 new IDs are in GLOBAL-CHECKLIST; all fourteen Q-items have introducing week, enforcing IDs and literal gate tasks. Week-11/12 scorecards deliberately remain **NOT RUN**. No unbuilt feature is described as done, and no cloud spending is required. The approved AWS/Kubernetes production topology remains future design.

## Preservation and validation limits

All 94 source/config fingerprints match the pre-pass snapshot; all eight original non-document staged blobs remain unchanged. Docs commits use an isolated index, excluding staged application source. Generated build/test artifacts are not application source edits. No reset, history rewrite, actual deployment, volume deletion or message to another person was performed.

The full extended linter and its isolated proof harness are recorded in LINTER-PROOFS; current real docs have zero errors/warnings. All seven Mermaid diagrams parse. IntelliJ/Obsidian/Notion visual/import smoke is **NOT RUN**; portable CommonMark/GFM plus required Text versions are preserved, but no native viewer result is invented. Real PostgreSQL/Kafka/auth/CSRF/security/CI/deployment/restore/load tests remain developer execution work; their absence blocks product gates, not hidden authoring claims. HR policy/consent, actual host, backup disk and public reachability remain unconfirmed.

## Saved-tree verification

A fresh git archive of committed checkpoint e2ca576, including docs and root README, passed the extended linter: 277 Markdown files, zero errors and zero warnings. This verifies tracked-file completeness independently of untracked working-tree files. The final tenant-worker metadata clarification and this receipt are also linted before their finishing commit; runtime/native-viewer limits above remain unchanged.
