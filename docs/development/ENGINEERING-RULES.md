# Business-first engineering standards

## What quality means

Business requirements and accepted acceptance criteria are the primary measure of success. A technically elegant feature that implements the wrong HR rule is incorrect. Start with the owning requirement, actors, tenant boundaries, happy path, failure paths and data invariants. Separate approved policy from proposed policy and client-demo behavior. Resolve business ambiguity that changes correctness before implementing dependent behavior; make routine technical choices autonomously within accepted constraints.

Choose the simplest design that meets correctness, security, reliability, readability and realistic operating constraints. For a material decision, explain which requirement it serves, the simplest viable alternative, adoption effort, ongoing cost, maintenance burden and evidence supporting the choice. Existing dependencies and patterns are preferred when they meet the requirement. No universal best architecture or zero-error guarantee is implied.

HRFlow's non-overengineering rule is [ADR 013](../adr/013-DOMAIN-PATTERN-PLAN.md): use the accepted, familiar enterprise stack and an explicit feature service/repository/transaction by default. Add a named pattern, interface, layer or infrastructure component only for demonstrated variation, external I/O isolation or a required correctness boundary. Never add a second mechanism beside an existing one solely to increase pattern count or appear more "enterprise." Security, tenant isolation, event reliability and restore are necessary requirements, not optional complexity cuts.

For Java object conversion, follow [ADR 014](../adr/014-MAPSTRUCT-MAPPING-POLICY.md): use MapStruct for straightforward deterministic mappings and fail compilation on unmapped target properties. Keep business rules, authorization, tenant derivation, repository access, guarded PATCH/version behavior and contextual event construction in explicit Java. Do not hide application orchestration in mapping annotations or map an untrusted request directly into persistence around the owning service checks.

## SOLID in practical terms

| Principle | Apply it this way | Avoid |
| --- | --- | --- |
| Single responsibility | Keep cohesive business policy, UI orchestration and transport/storage mapping in their appropriate modules | One service or component handling all features |
| Open for extension | Add extension points where a real integration or variation already exists | Frameworks for hypothetical future variants |
| Substitution | Implementations honor the same errors, invariants and observable contract | Demo success hiding a remote failure |
| Interface segregation | Expose the small operations a consumer actually needs | All-feature interfaces every client must depend on |
| Dependency inversion | Keep business policy independent of framework and transport details; inject external boundaries | An interface for every class without a meaningful boundary |

Use these principles at code and component boundaries. Files and directories should make those responsibilities visible; SOLID is not a requirement to create five layers or five files for each operation.

## Clean architecture with a small footprint

Organize by feature, then split responsibilities when the feature requires it. Domain policy owns business invariants. Application orchestration coordinates a workflow. Adapters translate HTTP, database, queue or device formats. Presentation renders state and delegates actions. The composition root selects concrete dependencies. Follow existing accepted architecture and inspect actual code before creating new folders.

For backend work, keep controllers thin and transactions within owner-service application boundaries. Keep database/framework concerns out of business policy where practical. For web/mobile work, keep screen rendering separate from business policy and wire mapping. A pure calculation may remain one function with focused tests; it does not need a repository, interface, factory and use-case object.

## Beginner-friendly files and components

Use plain domain names, explicit inputs/outputs and straightforward control flow. Keep functions focused, explain non-obvious business reasons briefly, and avoid clever generic frameworks, magic configuration or deep navigation. Comments should explain why rather than narrating obvious statements. Keep module entry points and dependencies easy to find.

Aim for about 250 lines per handwritten source file and review files over 300 lines. These are review triggers, not architecture proofs or automatic deletion rules. Split by cohesive responsibility: calculation, state orchestration, transport mapping or independently understandable UI section. Do not split into arbitrary numbered fragments, one-line forwarding layers or unnecessary subdirectories. Generated files, fixture catalogs and genuinely cohesive exceptions need an explicit reason. Do not refactor unrelated legacy files simply to satisfy a length target.

## Proportional BDD and TDD

Use BDD examples to agree user workflows and policy, especially transitions and failure cases. Given an eligible pending leave request and an authorized approver, when approval succeeds, then the request and balance settlement follow the accepted policy. Also describe stale state, duplicate commands and cross-tenant denial. An example is a proposed requirement until accepted; do not invent rules to fill missing policy.

For meaningful logic, regression fixes and contracts, use the red-green-refactor loop:

1. Add a focused test expressing observable behavior; for a regression, reproduce the actual defect.
1. Run it and confirm failure for the expected reason, not an unrelated environment problem.
1. Implement the smallest correct behavior and rerun the focused checks.
1. Refactor for clarity while keeping the behavior checks passing.
1. Run the relevant broader checks once; repeat only after a new change or unresolved concern.

TDD verifies implementation against examples; BDD helps define those examples. Use both when appropriate. Documentation, formatting and simple mechanical edits generally need lint, a build or direct inspection rather than new unit tests. Avoid tests of private implementation details or tests that simply repeat the code. Cover critical failures, permissions, concurrency and data invariants with provider evidence when a provider exists.

## Evidence and uncertainty

Every factual implementation claim must be supported by an inspected source location, accepted decision, official primary documentation or executed command. Distinguish observed behavior, approved intent, proposal, assumption and unknown. Check current primary documentation when library behavior or external facts are uncertain. Do not invent APIs, paths, test outcomes, deployment status or HR policy.

For a claimed verification result, record the command, relevant revision/dirty state, result and limits. Mocks establish consumer expectations; they do not establish provider implementation, authorization or transactions. Missing evidence remains missing. The goal is zero unsupported claims, with correction and verification; no agent rule can guarantee zero hallucinations or zero defects.

## Repeated work and token discipline

Use the [local MCP tools](../../tools/project_mcp/README.md) for narrow context, source lookup, large-file review and named checks. Read the relevant entry points, then request bounded excerpts rather than dumping the entire repository. Keep tool schemas and results small. Outputs carry truncation indicators; retrieve the missing evidence when it matters.

Reuse a result only while its relevant source revision, working changes, dependencies and execution inputs still match. Prefer deterministic scripts for repeated mechanical checks. Add a new tool only after a repeated task demonstrates value; reuse existing project checks rather than creating parallel implementations. No embeddings, graph database, hosted model or recurring service subscription is needed for this toolset. Smaller output is intended to reduce unnecessary context; no token savings benchmark is claimed.
