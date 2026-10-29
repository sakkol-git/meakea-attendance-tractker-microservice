# Gemini execution contract for HRFlow

This file is mandatory for Gemini. Read it before planning, editing, running generators or claiming progress. If a prompt conflicts with project authority, follow `AGENTS.md`, `docs/SOURCE-OF-TRUTH.md`, accepted ADRs and the owning roadmap guide.

## Hard execution boundary

Work on exactly one roadmap stone at a time. A stone is the exact step range in the current week's README and BUILD-GUIDE. Do not invent, merge, split, reorder or skip stones.

After a stone is fully reviewed and recorded, continue automatically to the next eligible stone. Do not edit files for two stones at once.

`docs/roadmap/YOU-ARE-HERE.md` controls the verified position. A later week may begin only when that dashboard explicitly identifies it as the current week or explicitly authorizes that exact later candidate. A blocked earlier gate does not grant permission to implement arbitrary future weeks. Work prepared ahead of a gate is `CANDIDATE — locally verified`; it does not advance progress.

The dirty workspace contains user and agent work. Preserve it. Do not treat an existing file, test or evidence entry as accepted until you trace it to its owning requirement and rerun the applicable check.

## Gate A — no code before a stone contract

Before editing, write a stone contract in your working response containing all of these:

1. Current week, stone, exact step IDs and title.
1. Current dashboard state and prerequisite stones/gates.
1. Owning requirement, BUILD-GUIDE section, VERIFY-AND-GATE assertions and accepted ADRs.
1. Approved behavior and explicit unresolved decisions.
1. Given/When/Then success and required failure examples.
1. Affected and unaffected boundaries with reasons: backend, web, mobile, HTTP, security, tenant/relationship authorization, database, events, configuration, operations and documentation.
1. Exact focused red command, expected failure and later regression command.
1. Exact files expected to change.
1. Dependencies and patterns to be used, with the existing owner that supplies each dependency.

If any item is unknown, inspect the repository and owning documents. If authority remains missing, mark the dependent work blocked. Do not choose business behavior yourself.

## Authority and decision rules

- Read root and nested `AGENTS.md`, `docs/SOURCE-OF-TRUTH.md`, `docs/development/PROJECT-CONTEXT.md`, `docs/development/CHANGE-CHECKLIST.md`, `docs/development/ENGINEERING-RULES.md`, `docs/roadmap/YOU-ARE-HERE.md`, the current week README, every BUILD-GUIDE part for the stone, VERIFY-AND-GATE, and every referenced accepted ADR/domain specification.
- Run `python3 scripts/project-context.py` before the first stone and refresh all three repository statuses before every later stone.
- Separate approved intent, implemented behavior and executed evidence.
- Do not invent HR policy, endpoints, DTO fields, roles, permissions, state transitions, event semantics, infrastructure, retention rules or acceptance criteria.
- ADR 013 governs pattern selection. Start with a cohesive service, explicit types and focused methods. Add Strategy, Specification, Adapter, Builder or another named pattern only when its documented admission test passes. Do not add generic command buses, handler registries, per-endpoint interfaces, pass-through layers or catch-all utilities.
- Existing mocks, fixtures, schemas, comments and AI summaries are not authority.

## Dependency policy: inspect before importing

Before adding or changing a Java dependency, read `docs/development/MAVEN-DEPENDENCIES.md`, the root `pom.xml`, `services/owner-service-parent/pom.xml` and the owning service POM.

- The root POM owns versions.
- The owner-service parent supplies shared owner dependencies.
- A service POM contains only service-specific dependencies justified by an accepted requirement and a test in the same stone.
- Do not duplicate Lombok, MapStruct, their processors or `lombok-mapstruct-binding` in an owner-service POM.
- Keep `lombok-mapstruct-binding` on the annotation-processor path.
- After a POM change run `python3 scripts/check-maven-dependencies.py`, then compile the affected reactor from clean sources.
- Dependency presence never proves feature behavior.

## Lombok policy

Lombok removes mechanical Java boilerplate. It must not define domain behavior or hide entity mutation.

Allowed when appropriate:

- `@RequiredArgsConstructor` for dependency injection where it keeps the class explicit and cohesive.
- `@Getter` for JPA entity observation.
- `@NoArgsConstructor(access = PROTECTED)` when JPA requires it.
- Focused constructors or builders for DTOs/test fixtures only when a Java record or ordinary constructor is unsuitable.

Forbidden on JPA entities:

- `@Data`.
- Broad `@Setter` or mutable public setters that bypass invariant methods.
- `@EqualsAndHashCode` or `@ToString` across lazy relationships.
- `@Builder` when it can create an entity without required invariants.

Prefer Java records for immutable DTOs and value carriers when compatible with the approved contract and framework. Do not convert existing types merely for style during an unrelated stone.

## MapStruct policy

ADR 014 is mandatory.

Use MapStruct for deterministic structural conversion such as entity-to-response and approved DTO/value conversion. Owner-service compilation uses MapStruct 1.6.3, constructor injection and `unmappedTargetPolicy=ERROR`.

MapStruct must not perform:

- Authorization or tenant/actor derivation.
- Repository, network, clock or request-context access.
- Business decisions or workflow transitions.
- Idempotency, ETag/version or guarded PATCH policy.
- Contextual event identity/version metadata.
- Hidden orchestration in expressions, callbacks, decorators or qualifiers.

Use explicit Java for those cases. A custom mapper requires a concrete complexity reason visible in code or the stone evidence. Explicitly name intentionally ignored MapStruct targets. Do not edit or commit generated sources.

Mapping tests cover contract-sensitive conversion, null/optional behavior, enums and regressions. Do not write tests that repeat every generated assignment.

## Gate B — red before implementation

For business logic, security, contracts, persistence, events and regressions:

1. Run the selected test before implementation.
1. Confirm it fails for the intended semantic reason.
1. Record command, exit, assertion and report path.
1. Implement the smallest cohesive behavior.
1. Rerun the focused test and required negative cases.

An unrelated compilation error, Docker outage, missing target, zero-test run or skipped test is not valid red or green evidence. If the behavior already exists, verify it and do not manufacture a failure.

## Implementation boundaries

- Tenant and relationship authorization is enforced in the destination service using independently validated token identity.
- Controllers translate transport. Application services coordinate owner transactions. Repositories persist owner state. Domain methods protect invariants. Mappers translate approved structures.
- Update `contracts/openapi.json` before shared HTTP implementations and run `python3 scripts/contracts.py sync` plus compatibility and adapter checks.
- `libs/hrflow-contracts/events/` owns Avro. Verify producer and consumer semantics; `mock://` does not prove a live Registry.
- Never edit an applied migration. Use additive expand/migrate/contract changes.
- Preserve idempotency keys, opaque ETags and unknown command outcomes.
- Do not add silent demo fallback in remote mode.
- Do not create aggregate source dumps such as `*_all_code.java`, temporary production sources, giant services or mixed-responsibility files.
- Preserve unrelated changes. Do not commit, tag, push, merge, deploy, reset, clean or delete unless the user explicitly requests it.

## Gate C — a stone cannot finish without evidence

Before moving to another stone:

1. Run all focused success and required denial/failure tests.
1. Run the owning module/client regression check.
1. Run contract, migration, event and dependency checks applicable to the stone.
1. Inspect root, `web/` and `mobile/` status and complete diffs, including untracked files.
1. Run `git diff --check` in all affected repositories.
1. Perform a senior review for security, tenant scope, relationship authorization, concurrency, replay, compatibility, cohesion and overengineering.
1. Fix every Critical or Major finding and rerun affected checks.
1. Update the current week's evidence with exact commands, exits, counts, failures, skipped tests, report paths and limitations.
1. Run documentation lint after evidence changes.
1. Assign exactly one disposition: `VERIFIED COMPLETE`, `CANDIDATE — LOCALLY VERIFIED`, `PARTIAL`, `BLOCKED` or `FAILED`.

Compilation, mocks, consumer fixtures, configuration syntax and documentation cannot by themselves complete a stone or weekly gate. Never convert an environmental failure into a passing product result.

## Gate D — permission to continue

Proceed to the next stone only when Gate C is recorded and the next stone's prerequisites are supported by current evidence. Refresh `YOU-ARE-HERE.md` and repeat Gate A.

If the next stone belongs to a later week and the dashboard does not authorize that week or exact candidate, stop development and report the blocker. Do not create evidence or code for additional future weeks.

## Required stone checkpoint output

After each stone, output this compact checkpoint and then continue when Gate D allows:

```text
STONE: <week/stone and step IDs>
AUTHORITY: <owning paths>
RED: <command, exit, intended failure>
IMPLEMENTATION: <files and behavior>
GREEN: <commands, exits, counts, skipped>
BOUNDARIES: <affected/unaffected reasons>
REVIEW: <findings and fixes>
EVIDENCE: <path and classification>
DISPOSITION: <allowed state>
NEXT: <next eligible stone or exact blocker>
```

At context or usage exhaustion, finish the current safe unit, update evidence, list exact dirty paths in all three repositories, and state the next command. Do not begin another stone.
