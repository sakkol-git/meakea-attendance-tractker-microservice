# Pattern learning path

[ADR 013](../adr/013-DOMAIN-PATTERN-PLAN.md) owns the finalized domain-by-domain pattern plan and the conditional Strategy candidates for leave, requests and confidence scoring. This page governs optional lessons and the admission exercise; it does not add a funded feature or override an owning domain rule.

## Admission before quota

Nine named candidates were reviewed for the original lessons. Builder, Adapter and Specification remain Stretch reading/refactors. Strategy is now also a conditional domain candidate under ADR 013; it has no automatic implementation slot. A target count of seven to nine never overrides the admission test or critical gate schedule. DI and Repository are foundational explanations, not counted abstractions.

| Candidate | When | Outcome | Reason |
| --- | --- | --- | --- |
| Builder | Week 3 | Stretch only | Two real fixture variations may justify named optional construction |
| Factory | Review Week 3 | Rejected now | No second configured provider exists |
| Strategy | Review Weeks 4, 6, 7 and confidence integration | Conditional candidate | Rules/model scorers, approved leave-type policies or distinct request-type behavior must demonstrate two real variations first |
| Chain of Responsibility | Review Week 4 | Rejected now | Explicit validation order is clearer and safer |
| Adapter | Week 7 | Stretch only | Attachment disk/test isolation is a real requirement |
| Decorator | Review Week 5 | Rejected now | Framework transactions suffice; critical event gate |
| State and Template Method | Review Week 6/7 | Rejected now | Transition tables plus locked methods preserve distinct invariants |
| Specification | Week 9 | Stretch only | Admit only after two actual report filter combinations hurt |

State and Template Method are separate named candidates reviewed together; the original nine-candidate lesson inventory is unchanged. Strategy's added domain review does not create an implementation quota. Never add a pattern merely to reach a target count. No new refactors enter Week 11 hardening or Week 12 freeze.

## Lessons

- [Builder](03-ORGANIZATION-AND-ADMIN/PATTERN-LESSON.md).
- [Adapter](07-REQUESTS-AND-APPROVALS/PATTERN-LESSON.md).
- [Specification](09-REPORTING-AUDIT-OBSERVABILITY/PATTERN-LESSON.md).
- [Dependency injection primer](primers/DEPENDENCY-INJECTION.md).
- [Repository primer](primers/REPOSITORY.md).

Only if spare time remains after the mandatory gate, a lesson may use a separate three-step learning stone: plain tested baseline, documented pain, then refactor with unchanged tests. A rejection completes the stone with a simpler tested implementation. No week exceeds two patterns or shares a new pattern with its riskiest critical gate.

## V2 quality allocation

Original Week-3 Builder, Week-7 Adapter refactor and Week-9 Specification refactor are removed from the funded critical path; RLS/architecture checks, private-file recovery and observability take priority. Existing lesson examples remain optional teaching, not adopted code or prerequisites. No extra learning stone is added to a full week without recomputing its five-to-eight stone count, capacity and ID map.
