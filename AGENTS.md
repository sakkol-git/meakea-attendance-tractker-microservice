# HRFlow project instructions

<!-- postman-cli:installation:start -->

Postman CLI is installed on this device, and can be used for API Engineering work.

<!-- postman-cli:installation:end -->

## Read before changing code

- Read `docs/SOURCE-OF-TRUTH.md` for authority, then the relevant accepted ADR/domain specification.
- Read `docs/development/PROJECT-CONTEXT.md` and `docs/development/CHANGE-CHECKLIST.md`.
- Run `python3 scripts/project-context.py` for fresh repository revisions, dirty state and entry points.
- Inspect actual source and tests. Distinguish approved intent, implemented behavior and verified results.
- Root, `web/` and `mobile/` are separate Git repositories. Inspect each affected status/diff; root commits exclude clients.

## Change across boundaries

- Assess UI, mobile, HTTP contracts, business rules, database, events, configuration and operations for every behavior change. Document affected and unaffected boundaries with reasons.
- `contracts/openapi.json` owns the provisional shared HTTP slice; `libs/hrflow-contracts/events/` owns Avro. Read contract status before assuming backend support. Other client routes remain provisional requirements.
- Update the canonical contract first, run `python3 scripts/contracts.py sync`, and include changed bundles and adapter tests in each client repository.
- Run contract compatibility against the target branch; preserve existing consumers and installed mobile versions. Use additive APIs and expand/migrate/contract database changes. Never edit applied migrations.
- Do not rewrite accepted requirements, historical evidence, or security checks to make implementation appear complete. Record contradictions and resolve them with the owning source.
- Tenant/relationship authorization belongs on the server. UI role checks, mocks and schema validation do not prove authorization or business correctness.
- No silent demo fallback in remote mode; preserve unknown command outcomes, idempotency keys and opaque ETags.

## Finish with evidence

- Follow `docs/development/CHANGE-CHECKLIST.md`; use `.github/pull_request_template.md` for review.
- Run `python3 scripts/verify-project.py` for the complete local baseline, or select an explicit scope and report omitted scopes.
- Update the relevant current guide alongside behavior changes and catalog new Markdown topics.
- Report exact checks, failures and limitations. Never claim backend acceptance from consumer fixtures, CI setup, or unexecuted tests.
- Preserve unrelated working changes. Do not commit, push or deploy unless requested.

## Business-first engineering rules

- Business requirements and agreed acceptance criteria are the primary success measure. Trace behavior to an owning requirement; do not invent HR policy, endpoints, fields or supported infrastructure. Record unknowns and confirm blocking business ambiguity before dependent work.
- Apply SOLID to responsibilities and dependency boundaries: one reason to change, extension at proven variation points, substitutable implementations, small consumer-focused interfaces, and business logic independent of transport/storage details. Do not create abstractions solely to satisfy an acronym.
- Organize by feature and responsibility. Keep domain rules separate from UI, HTTP and persistence; wire dependencies in a composition root. Follow existing accepted architecture and repository conventions; avoid new frameworks or layers without a concrete need.
- Keep files, components and directories cohesive and beginner-friendly. Use plain domain names, explicit types, small focused functions and shallow navigation. Prefer existing patterns and brief comments explaining why. Aim for about 250 lines per handwritten source file; review files over 300 lines for unrelated responsibilities. Extract meaningful modules, never arbitrary fragments. Generated artifacts and justified cohesive exceptions must be identified.
- Avoid monster components, catch-all utilities, giant services and mixed business/transport/UI logic. A small feature may stay simple; not every operation needs an interface, factory, repository or use-case class.
- Choose the simplest approach that meets correctness, security, maintainability and operating constraints. State material tradeoffs in adoption, delivery time, recurring cost and maintenance; do not add speculative extensibility or paid services without a justified requirement.
- Use BDD-style Given/When/Then acceptance examples for workflows and ambiguous business behavior; use TDD red-green-refactor for meaningful business logic, regression fixes and contracts. Confirm a test fails for the expected reason, implement the smallest correct change, then refactor. Use proportional verification for documentation, formatting and mechanical changes; do not create tests that merely mirror code.
- Ground factual claims in inspected files, accepted decisions, official primary documentation or executed commands. Separate observation, proposal, assumption and unknown. Do not claim zero hallucinations, complete context, passing checks or production readiness without evidence; disclose uncertainty and verification limits.
- Use narrow searches and bounded source excerpts. Prefer available HRFlow MCP tools for repeated context/read/search/check work; use existing CLI checks when the tools are unavailable. Reuse evidence only while its source revision, working changes and inputs remain valid; never sacrifice required verification to save tokens.

## Local tools and detailed standards

- Read `docs/development/ENGINEERING-RULES.md` for examples and decision criteria.
- Local MCP setup and tool limits are in `tools/project_mcp/README.md`. Run `python3 scripts/setup-project-mcp.py` to install and register it for this checkout. It does not edit global settings.
- Start with `get_project_context`, then narrow `search_source`/`read_source`. Use `inspect_structure` to identify cohesion reviews and `run_project_check` for named checks; missing or truncated evidence must stay visible.

## Compact test evidence

- When HRFlow MCP tools are available, use `run_tests` for a relevant suite/file, then `get_test_result` for structured counts and failures. Request `get_test_details` only for a selected stack or log excerpt. Use `cancel_test_run` for work that should stop.
- Do not infer success from a console tail, reuse stale reports or treat partial/zero-test runs as acceptance. Grouped messages identify repeated evidence, not a proven root cause. Run the broader verifier when required; narrow unit runs omit builds, lint and other scopes.
