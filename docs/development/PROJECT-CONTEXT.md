# Project context and verification foundation

## Start with current evidence

Run `python3 scripts/project-context.py` from the repository root. It prints the exact backend, web and mobile revisions, working changes and reading entry points without reading secrets or copying application data. It generates navigation on demand; no persistent index or external AI service is required.

Read [source of truth](../SOURCE-OF-TRUTH.md) for intent and decision authority. Read [current progress](../roadmap/YOU-ARE-HERE.md) for roadmap status. Inspect source/tests for implementation and dated command results for verification. If these disagree, record the gap rather than promoting one into evidence of another.

## Boundaries and entry points

| Boundary | Inspect first | Evidence limits |
| --- | --- | --- |
| Product and business rules | Architecture, accepted ADRs, relevant domain chapter | Proposed policy is not approved by a client demo |
| Backend | Root pom.xml, services, libs | Business APIs remain pending; configuration tests are not HR acceptance |
| Web | web/docs/ARCHITECTURE.md and web/docs/INTEGRATION.md | Demo and mock-provider tests do not prove real server integration |
| Mobile | mobile/docs/ARCHITECTURE.md and mobile/docs/API-INTEGRATION.md | Inspect current source; platform permissions need native acceptance |
| HTTP | contracts/openapi.json and client adapter guides | Broad provisional consumer inventory; only the leave review fixture has cross-client adapter evidence |
| Events | libs/hrflow-contracts/events and ADR 003/008 | Avro compilation checks syntax, not delivery or business semantics |
| Persistence | Owner migrations, API/database design | Never edit applied migrations; verify upgrades and tenant isolation |
| Infrastructure | infra/docker/docker-compose.yaml and service configuration | Compose validation does not start or verify the stack |

The root, web and mobile are independent Git repositories. The parent ignores both client trees. Saving a root change does not save client changes. Each repository has instructions, a review template and CI checks; coordinated changes require links and the exact revisions tested together.

## Shared HTTP slice

The canonical `contracts/openapi.json` working copy inventories 101 paths under `/api/v1`, with business operations marked pending. Its older metadata still describes the initial leave slice, so scope classification is a required planning gate rather than evidence that all operations are accepted. Shared fixtures in `contracts/fixtures/leave-approval.json` exercise leave detail and approval through both actual client adapters. Other operations remain provisional until reconciled with the client handover guides, business owners and provider tests. See the [frontend-to-backend synchronization plan](../roadmap/FRONTEND-TO-BACKEND-ALIGNMENT.md).

The slice preserves opaque ETags and requires UUID-v4 command keys. Mobile supplies reason/note; web approval may omit the body. These variants are modeled deliberately. Authentication refresh protocols, leave creation half-day values, balance DTOs, pagination and advanced workflows differ between clients and remain integration work. Do not infer compatibility for them from this slice.

The server implementation must still verify tenant and approver relationships, atomically settle balances, persist idempotency receipts and publish owner events. Schema checks and successful consumer mocks do not establish any of these behaviors.

## Engineering rules and local tools

[Engineering rules](ENGINEERING-RULES.md) prioritize accepted business behavior, practical SOLID, readable cohesive files, proportional BDD/TDD and evidence. The same core rules are present in all three AGENTS.md files for standalone checkout. [Local MCP tools](../../tools/project_mcp/README.md) provide bounded context/read/search/structure/check operations and structured background test runs with an isolated setup and project-local registration. They are optional conveniences over CLI checks, not a prerequisite for implementation.

## Local commands

Create a development-only Python environment once:

```bash
python3 -m venv .venv-checks
.venv-checks/bin/python -m pip install -r scripts/requirements-checks.txt
```

Use its interpreter for commands requiring the schema validators:

```bash
.venv-checks/bin/python scripts/contracts.py validate
python3 scripts/contracts.py sync
python3 scripts/contracts.py check
.venv-checks/bin/python scripts/contracts.py compat --base origin/main
.venv-checks/bin/python scripts/verify-project.py --base origin/main
```

Replace `origin/main` with the actual target branch or merge-base revision. Compatibility explicitly reports when the baseline predates the initial contract. The baseline gate conservatively preserves all existing wire definitions and permits new paths/components and editorial changes. It intentionally rejects some safe additive edits inside an existing schema. Add a separately versioned definition/operation while retaining old consumers, or deliberately revise the policy with compatibility evidence; changing a version string does not waive the check. This is not a complete semantic compatibility analyzer.

`sync` copies canonical files and a content-hash receipt into both clients. `check` requires both checkouts and fails on missing or stale copies. A standalone client can run `python3 contracts/check-bundle.py` to detect local tampering or partial copies; only the parent cross-repository check proves freshness against the current canonical files. Neither command fetches, edits business adapters, commits or deploys.

The full verifier runs contracts, drift checks, negative checker tests, documentation lint, Maven verification, Compose validation, web checks/browser journeys, Flutter analysis/tests and local MCP tool tests. Install Java 21, Maven-wrapper prerequisites, Node matching the client manifest, Docker CLI/Compose, the pinned Flutter SDK, client dependencies and Playwright Chromium first. The tooling scope uses `.venv-mcp` when available; otherwise install `tools/project_mcp/requirements.txt` in the selected Python environment. For an intentional subset use `--scope foundation`, `backend`, `web`, `mobile` or `tooling`; the report explicitly lists omitted scopes. It continues independent checks after failures and exits nonzero if any fail.

## CI and coordinated integration

Root CI runs foundation checks, contract compatibility for pull requests, Maven verification and Compose validation. Client CI validates the bundled receipt and runs its own build/test checks. The web browser tests use demo and mock-server environments, not a deployed HR backend.

The root `integration.yml` workflow accepts exact 40-character web/mobile commit SHAs. It checks out both configured repositories, validates that their contract and shared-seed bundles match the chosen backend revision, and runs the full verification baseline. A private client repository requires the optional read-only cross-repository checkout token secret. Automatic per-repository CI does not establish cross-repository compatibility; run integration for the selected release combination.

CI definitions become active after the changes are committed and pushed in their owning repositories. Make the checks required in repository branch rules to block merges; adding workflow files alone cannot enforce those settings. This task does not configure GitHub permissions or publish changes.

## Local verification checkpoint

[Verification record](VERIFICATION.md) lists executed checks and existing blockers, including Compose interpolation, mobile runtime compilation and legacy documentation formatting. CI does not bypass these failures.

## Extending the foundation

For each next implemented feature, agree the wire contract and fixtures, add actual consumer adapter tests, implement provider tests and add a small real-backend journey including tenant/permission failures. Record deployment order and old installed mobile compatibility. Use [the change checklist](CHANGE-CHECKLIST.md) and the PR template rather than adding a second manually maintained project graph.
