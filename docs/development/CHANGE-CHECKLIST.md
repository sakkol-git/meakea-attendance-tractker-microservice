# Cross-system change checklist

## Before editing

- Read the owning requirement, accepted ADR, current implementation and tests.
- Run `python3 scripts/project-context.py`; inspect each affected repository status.
- Identify the owning business requirement and Given/When/Then acceptance examples, including failure behavior.
- Choose proportional BDD/TDD verification and record any assumption that affects business behavior.
- Identify old deployed consumers and data that must remain compatible.

## Assess each boundary

Record changed or unaffected with a concrete reason in the PR template. A dependency needs assessment, not an automatic edit.

- Web and mobile: screen state, forms, validation, loading/error/empty states and accessibility.
- API: routes, DTOs, pagination, errors, authentication, ETags and command replay semantics.
- Logic/security: business transitions, tenant scope, relationship authorization and concurrent actions.
- Data: constraints, clean/upgrade migrations, existing records, backups and rollback limits.
- Events: producer/consumer schema compatibility, semantic meaning, duplicate delivery and replay.
- Infrastructure: environment examples, service routing, resource needs, secrets and deployment order.
- Documentation: current handover guide and owning specification; preserve dated historical evidence.

## Implement and synchronize

- Update the canonical contract and synthetic fixtures for the implemented slice.
- Preserve old wire definitions or add a compatible version; never waive compatibility by bumping a label.
- Run `python3 scripts/contracts.py sync` and include bundle changes in both client repositories.
- Update adapters and behavior tests where needed; never change a mock merely to hide a real mismatch.
- Test critical success and failure paths, including stale writes and cross-tenant/role denial when a provider exists.
- Link coordinated changes and record the exact backend/web/mobile revisions tested together.

## Structure and decision review

- Review cohesive responsibilities, dependency direction and small consumer-focused interfaces.
- Review handwritten files over 300 lines; split mixed responsibilities and record justified exceptions.
- Confirm names, directories and control flow are understandable to a beginner.
- Reject speculative abstractions; explain material adoption, cost and maintenance tradeoffs.

## Before finishing

- Run the relevant checks using `scripts/verify-project.py`; run full scope for an integration candidate.
- Run contract compatibility against the actual target branch or merge base.
- Inspect all affected repository diffs, including new untracked files and generated bundles.
- State executed commands, results, omitted scopes and unresolved blockers.
- Separate consumer fixtures from live backend evidence; do not claim production readiness from this checklist.
