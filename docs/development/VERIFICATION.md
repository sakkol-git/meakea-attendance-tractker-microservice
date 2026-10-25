# Context foundation verification

## Checkpoint

Date: 2026-10-24, Asia/Phnom_Penh. This records local execution against the working trees, including pre-existing changes. It is not an immutable release receipt; rerun verification after further edits or commits. The fresh context command prints exact current revisions and dirty state.

## Executed checks

| Check | Result | Scope |
| --- | --- | --- |
| OpenAPI and fixture validation | **PASS** | Two provisional leave operations; JSON Schema examples and header formats |
| Checker negative tests | **PASS** | Five tests covering missing/tampered bundles, operation response/fixture mismatches and wire changes |
| Cross-client bundle and seed checks | **PASS** | Both local client copies match canonical content |
| Standalone bundle checks | **PASS** | Both client receipts match their bundled files |
| New web adapter contract tests | **PASS** | Four tests; included in the full web test run |
| New mobile feature contract tests | **PASS** | Two tests using the actual leave repository with simulated transport |
| Maven batch verify | **PASS** | All ten reactor projects; 29 existing Java tests passed |
| Web npm run check | **PASS** | Formatting, type checking, data/unit tests and production build; 38 unit tests |
| Web browser journeys | **PASS** | Seven demo/mock-server journeys; no live backend acceptance |
| New/edited foundation Markdown | **PASS** | Instructions, context/checklist, contract guide, navigation and standards lint |
| Workflow YAML structure | **PASS** | Parsed jobs/triggers/steps locally; not a hosted GitHub Actions run |
| Full documentation lint | **FAIL** | Existing ADR/primer/roadmap/evidence formatting errors remain |
| Root git diff whitespace check | **FAIL** | Existing Compose/service YAML working changes contain extra EOF blank lines |
| Compose config validation | **FAIL** | Existing PostgreSQL password substitution uses invalid Compose interpolation |
| Full Flutter analysis/test suite | **FAIL** | Existing pending runtime code accesses secrets/namespace on SessionGateway; analyzer also reports an unused workflow import |

The combined foundation verifier correctly returned nonzero for documentation and whitespace failures. No failing gate was weakened or marked successful. Existing unrelated source/configuration changes were preserved.

## Commands and reproducibility

Python schema-check dependencies were installed in a temporary environment outside the workspace using `scripts/requirements-checks.txt`. The documented `.venv-checks` setup provides the same commands without depending on that temporary path.

Executed contract commands: `scripts/contracts.py validate`, `check`, `compat --base HEAD`, both clients' `contracts/check-bundle.py`, `scripts/sync-client-demo-data.py --check`, and unittest discovery in `scripts/tests`. The initial Git baseline has no canonical HTTP contract, so the CLI explicitly reported that compatibility was not established against an earlier version. Negative tests exercised the compatibility enforcement logic.

Executed application commands: root `./mvnw --batch-mode verify`, Compose config validation, web `npm run check` and `npm run test:e2e`, mobile pinned Flutter `analyze`, `test`, and the focused `test/shared_contract_test.dart` test file. Full mobile results included 36 passing tests and 12 load/compile failures at this checkpoint.

## Remaining acceptance

The shared leave contract remains provisional. No live provider route, real tenant authorization, atomic leave transaction, idempotency persistence, event delivery or database upgrade acceptance was introduced or verified by consumer fixtures. Other client APIs and authentication protocols still need reconciliation with the backend.

CI and PR templates are reviewable local files. They require commits/pushes in their owning repositories to run remotely. Required-check branch rules and private cross-repository checkout credentials require repository configuration. The manual integration workflow must select exact client SHAs containing the new bundles and tests. No commit, push, deployment or GitHub settings change was performed.

## Agent rules and local MCP extension

Date: 2026-10-24. Root, web and mobile agent rules now prioritize agreed business acceptance criteria, practical SOLID, cohesive beginner-readable modules, clean dependency boundaries, proportional BDD/TDD, adoption/cost/maintenance tradeoffs and evidence-backed claims. The 300-line threshold requests review; it does not prove architecture correctness or force arbitrary splits.

The local MCP toolset has five tools: fresh project context, numbered source reads, literal scoped search, file-size review and allowlisted checks. Runtime modules are each below 150 lines. Workspace acceptance tests were written first and initially failed because the module was absent. Ten tests subsequently passed, including a real stdio SDK client discovering and invoking all five tools, rejected traversal/arbitrary commands, bounded failed-check output, timeout handling and preservation of existing registration settings.

The isolated MCP environment was installed locally and this checkout was registered in ignored `.codex/config.toml`. No global settings, trust setting or external service was modified. The current chat's tool catalog was not dynamically updated; reload the trusted project/session to discover the server. No token-savings benchmark or guarantee of zero hallucinations is claimed. The earlier unrelated application/configuration/documentation blockers remain outside this extension.

## Structured test runner extension

Date: 2026-10-24. Four additional MCP tools start, inspect, drill into and cancel background test runs. Suite commands are allowlisted for backend, web unit, browser and mobile tests, with optional validated test-file selection. Per-run machine reports drive counts and failures; arbitrary shell arguments and file paths are not accepted as run handles.

Twenty-two tool tests passed, covering JUnit and Flutter parsing, missing/malformed reports, zero-test runs, nonzero process exit, timeouts, cancellation, stale Maven reports, bounded failure grouping and the real stdio tool catalog. The focused parser tests were run before implementation and initially failed because the module did not exist.

A real stdio SDK client exercised all four runners with focused selections: four shared-contract web unit tests, two shared-contract mobile tests, one leave-service environment test and two remote-contract browser tests passed. These are selected suite results, not a new full-system acceptance claim. The caller received compact structured results without reading the full console logs.

A temporary synthetic Vitest assertion failure was then created, run through the MCP tools and removed. The summary correctly reported one failed test, the expected-versus-actual assertion and its test-file line/column. The selected failure stack was retrievable through the details tool. The deliberate failure was a runner verification probe, not an application regression.

Logs, reports and summaries were retained in temporary artifact folders. Existing application behavior and pending unrelated working changes were not modified by this extension. The previous verification sections remain checkpoint-specific; no earlier failure is silently promoted to a current result.
