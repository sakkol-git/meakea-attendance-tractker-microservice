# ADR 014 — MapStruct mapping policy

## Status and scope

Accepted on 2026-10-27 by explicit user decision. This record governs Java object mapping inside the seven Spring services. It does not apply to Nuxt/TypeScript or Flutter/Dart, change an HTTP or event contract, approve business policy, or require a broad rewrite of working code. MapStruct 1.6.3 is already pinned in the root build and configured for the owner services; this decision adds no new runtime component or paid dependency.

## Decision

Use MapStruct for straightforward, deterministic Java mappings when source and target types already have approved meaning. Typical uses are entity-to-response, command-to-domain value, domain-to-DTO and structural event-payload mappings whose fields can be selected without business decisions or external I/O.

Use a handwritten mapper or an explicit application/domain method when mapping requires business rules, authorization, tenant derivation, relationship lookup, persistence access, encryption, parsing with policy, clock or request context, idempotency/event metadata, conditional workflow behavior, or other logic that is clearer as ordinary Java. PATCH mappings with absent-versus-null semantics, guarded entity updates or ETag/version behavior remain explicit unless a focused MapStruct update method demonstrably preserves those semantics.

MapStruct removes structural assignment boilerplate; it does not own validation, authorization, transactions or domain policy. Controllers must not map untrusted requests directly into persistent entities in a way that bypasses the owning application service and its checks. Generated mappers must not derive tenant or actor identity from request bodies or caller-controlled headers.

## Implementation rules

- New or materially changed straightforward Java mappings use MapStruct. Existing handwritten structural mappers migrate when their owning feature is changed; no unrelated bulk rewrite is required.
- Prefer a shared mapper configuration with Spring component model, constructor injection and `unmappedTargetPolicy = ERROR`. Any deliberately ignored target field must be named explicitly at the mapping method.
- Keep mapper interfaces in the owning feature. Do not create one cross-domain mapper or a generic reflection-based mapping layer.
- Qualifiers and small pure converters may handle representation differences. Do not hide business branching, repository calls or service orchestration in annotation expressions, callbacks or generated decorators.
- A handwritten mapper must have a concrete complexity reason visible in its name, code or a brief comment when the reason is not obvious. `OrganizationFactMapper`, for example, may remain handwritten where it creates event identity, version and serialization metadata rather than copying an approved payload shape.
- Generated sources are build artifacts and are not edited or committed unless an existing build rule explicitly requires them.

## Verification and consequences

The Maven compile/test lifecycle must execute the annotation processor. Unmapped target properties fail compilation under the shared configuration. Mapping tests focus on contract-sensitive conversions, null/optional behavior, enum/value conversions and regressions; they do not repeat every generated field assignment.

Provider, tenant, authorization, stale-write, transaction and event-replay tests remain required at their owning boundaries. A passing mapper test does not prove those behaviors or a weekly gate.

The policy reduces repetitive Java mapping code and exposes DTO drift at compile time. It adds annotation-processor build coupling and requires generated-code diagnostics to remain understandable. Reconsider the version or configuration when Java/Spring compatibility, compiler behavior or measured build cost requires it; do not adopt a beta release without a separate compatibility result.
