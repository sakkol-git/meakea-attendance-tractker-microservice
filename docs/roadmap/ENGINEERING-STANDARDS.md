# Senior engineering standards

## Meaning and limits

Enterprise standard here means repeatable, testable engineering practices on one computer. It is not certification, regulatory compliance, guaranteed availability, or a guarantee that a junior team finishes in twelve weeks. Each rule below has a beginner minimum and an observable acceptance check; every item is planned until executed evidence exists.

Observed package roots are com.hrflow.attendance, com.hrflow.leave and other service roots in services. Retain those service roots. Proposed api/application/domain/infrastructure subpackages follow the final design; no hrflow-app monolith is created. Inspect actual files before selecting class names. Java target 21 and root Boot 4.1.1 come from pom.xml; services still use 4.0.8 and must not be called aligned. Maven wrapper is the build tool. No dependency/tool version is silently upgraded by this document. JDBC replaces scaffold JPA; reactive gateway replaces scaffold MVC only after a compatibility spike.

One OpenAPI file owns wire shapes, canonical API document owns persistence semantics, and ADR 007 owns tenant defense. Code snippets are proposed examples, not classes discovered in source.

## Q01 — API contract

Rule: One versioned OpenAPI file is wire authority.

Beginner minimum: Extend proposed contracts/openapi.yaml only for implemented slice; mock Flutter from it and validate examples/live schema.

Acceptance check: CI fails on invalid spec, broken example or response mismatch; /api/v1 is explicit.

Introduced: Week 01; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q02 — Problem details

Rule: All HTTP failures use one RFC 9457-style problem shape with stable codes.

Beginner minimum: Spring MVC advice plus Security entry-point/access-denied handlers return application/problem+json; no exception text leaks.

Acceptance check: Assert content type, type/title/status/detail/instance/code/traceId for validation, 401, 403, conflict and 500.

Introduced: Week 01; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q03 — Validation layers

Rule: Edge validates shape, domain validates rules, DB rejects impossible persisted state.

Beginner minimum: Bean Validation DTO; one domain invariant test; tenant FK/unique/check in a new migration.

Acceptance check: Invalid input creates zero rows; bypassing Java validation still hits the DB constraint.

Introduced: Week 03; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q04 — Tenant isolation

Rule: Trusted tenant from verified principal, mandatory scoped queries, tenant-leading key/index and tested RLS.

Beginner minimum: Shared two-tenant endpoint fixture plus ENABLE/FORCE policy on personal tables; non-owner runtime role; transaction-local context.

Acceptance check: Every implemented OpenAPI route is registered in tenant matrix; absent context, denied insert and reused pool connection cannot leak.

Introduced: Week 03; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q05 — Authentication and authorization

Rule: Short access JWT, rotating refresh with family reuse detection, method/resource checks and throttling.

Beginner minimum: One synthetic tenant/user path; hash passwords, validate issuer/audience/signature/expiry; bounded login limiter; independent approver.

Acceptance check: Wrong password and unknown user produce identical safe response; repeated login throttles; old refresh revokes family; cross-role calls fail.

Introduced: Week 02; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q06 — Secrets and config

Rule: No secret in Git or logs; runtime environment/secret mounts are separated from migrations.

Beginner minimum: Ignored host config, test-only keys, secret scan on push, rotation manifest with no values.

Acceptance check: Gitleaks flags synthetic test-secret fixture; rotation allows new key and bounded old-key expiry; git diff contains no actual secret.

Introduced: Week 02; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q07 — Data integrity

Rule: Flyway only, no applied edits, exact numeric balances, tenant FKs/uniques/checks and optimistic versions.

Beginner minimum: Read clean/upgrade migration history; numeric balance ledger; If-Match version plus deterministic lock for races.

Acceptance check: Clean and upgrade DB tests pass; stale update 412, duplicate decision once, concurrent reservations never overspend.

Introduced: Week 03; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q08 — Reliability

Rule: Retryable commands have durable keys; bounded outbound I/O and graceful shutdown; atomic owner outbox and consumer inbox receipts before Kafka offset acknowledgment.

Beginner minimum: Store command result with owner write/intent; handler effect plus receipt transaction; HTTP timeout/jitter; drain/release leases on shutdown.

Acceptance check: Lost response retry returns same ID; worker crashes before/after commit preserve intent and one effect; no unbounded provider call.

Introduced: Week 04; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q09 — Testing pyramid

Rule: Small domain tests, real PostgreSQL integration tests and a few end-to-end stories cover critical behavior.

Beginner minimum: JUnit 5 with Testcontainers PostgreSQL 16 and real Kafka for retained event flows; JaCoCo only for auth/attendance/leave domain and service packages.

Acceptance check: Critical packages target at least 80 percent lines and 70 percent branches, zero failing tests; negative/race/recovery cases cannot be replaced by coverage.

Introduced: Week 01; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q10 — Architecture fitness

Rule: Foreign internals and dependency cycles fail the build.

Beginner minimum: ArchUnit test imports actual compiled classes; api/internal boundaries, own controllers/services and no domain-to-web/persistence dependency.

Acceptance check: Deliberately forbidden import and module cycle make test fail; zero imported classes is a failure, not a vacuous pass.

Introduced: Week 03; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q11 — Observability

Rule: Safe structured logs include correlation/technical tenant ID, readiness and bounded useful metrics.

Beginner minimum: Built-in structured logging/MDC, private Actuator/Kuma, counters for accepted/rejected check-in, outbox retry/age and export failure.

Acceptance check: One command is traceable by correlation; no token/name/location in log; bounded metrics have no tenant/employee labels; worker/backup failures visible.

Introduced: Week 02; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q12 — CI and supply chain

Rule: Every push builds/tests/formats/scans dependencies, image and secrets; base images and actions are pinned.

Beginner minimum: GitHub Actions within existing free quota; Maven Spotless+PMD; Trivy filesystem/image; Gitleaks; reviewed versions/digests.

Acceptance check: Synthetic insecure dependency/image/secret fixtures trigger the right blocking job; unwaived HIGH/CRITICAL findings block; no job is success() on scanner failure.

Introduced: Week 01; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q13 — Operations and release

Rule: Deploy/rollback/restore/reboot/disk/internet runbooks and scheduled restore evidence accompany versioned release.

Beginner minimum: Nightly custom dump/external copy, weekly scratch restore, clean host Weeks 10/12, changelog and protected v0.x.y tags.

Acceptance check: Operator follows runbook without guessing target; row/ledger/file checks prove restore; release tag points to exact tested digest/config/migration manifest.

Introduced: Week 10; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Q14 — Code quality and review

Rule: Formatter/static checks and ten-minute self-review run before every stone tag; patterns pass admission.

Beginner minimum: Spotless Java formatting plus PMD findings gate; reviewed test/data/secret/migration diff; plain code before optional abstraction.

Acceptance check: Formatting/static fixture fails CI; each stone records completed review evidence, tests, startup and exact tag commit.

Introduced: Week 01; final enforcing IDs and literal gate tasks appear in [quality coverage](QUALITY-COVERAGE.md).

## Tool and configuration discipline

All proposed tools are free/open source and run locally or within the account's existing free CI quota. Spotless formats Java; PMD is static analysis; ArchUnit checks dependencies; JaCoCo measures selected critical packages; Trivy scans filesystem/dependency and built-image content; Gitleaks scans secrets. None requires a new runtime service. Pin reviewed plugin/action/image versions when actually installed, retain lockfiles, verify downloaded tool checksum, and record tool versions in build output. Do not invent current plugin versions or paid scan services.

CI runs the same commands locally and on push. Scanner update/network failure is an observable blocked check, not clean scan evidence. Free runner quotas/availability are unverified; if unavailable, block release until the same checks run on an existing self-hosted runner, never auto-purchase capacity. Do not give fork workflows production credentials.

## Error logging and testing conventions

Use stable uppercase error codes with problem details; clients never branch on human detail. JSON camelCase, SQL snake_case, /api/v1 plural kebab-case routes, UTC instants and explicit shift-local dates remain. Never log credentials, personal names, raw evidence or private storage paths. Technical tenant/correlation identifiers stay only in protected logs; metrics avoid per-employee/tenant cardinality.

Tests live under each existing service src/test/java with mirrored package names. Domain tests use fixed clocks and no database; integration tests exercise PostgreSQL constraints/locks/RLS, clean/upgrade migrations and outbox. Real Kafka tests are mandatory because Kafka remains in the final design: producer acknowledgment/mark-sent crash, effect/inbox/offset crash, duplicate delivery, schema compatibility, bootstrap and retention-gap handling. A red test must fail for the intended assertion, not a missing import, absent Docker or absent project.

## Pattern admission and self-review

All four answers must be yes: current requirement, plain tested version hurts, two real variations or isolation/composition need, and a junior explains it in two sentences. A clear method or switch wins otherwise. Optional pattern refactors move to Stretch before mandatory quality work.

[Self-review](CODE-REVIEW-CHECKLIST.md), [tests](TEST-STRATEGY.md), [security](SECURITY-BASELINE.md), [API guide](API-GUIDELINES.md), [commands](DAILY-COMMANDS.md), [golden slice](GOLDEN-SLICE.md), and [runbooks](runbooks/README.md) define the reusable recipe.

## Final service boundary and license limits

Gateway strips caller identity headers, verifies JWT, then creates trusted internal identity; services enforce role/resource checks at method level. No direct business port is published. Shared tests cover forged headers and bypass attempts as well as cross-tenant IDs. Until those tests prove the boundary, Q04/Q05 are conditional, never accepted merely because a header is present. Runtime database roles stay non-owner with no BYPASSRLS.

Q10 checks per-service package direction and Maven dependencies, not a fictional com.hrflow.app package. Domain imports no Spring/JDBC/Kafka. No service imports another service artifact or implementation; shared libraries contain contracts and infrastructure without domain repositories. SQL tenant predicates require integration/RLS tests in addition to ArchUnit.

The final design's Prometheus/Grafana, structured logs and trace propagation remain observability targets; optional full log/trace storage must fit measured host resources. Health, correlation, lag/error metrics and alerting are minimum gates. Final cloud topology is future improvement per user instruction. Do not describe deferred cloud or unexecuted monitoring as delivered.

Confluent Registry licensing and Terraform licensing must be checked before redistribution or future cloud work. Existing final architecture names them; this guide does not claim every named product is OSI-licensed merely because a container is downloadable. No new paid product is introduced by the quality practices.
