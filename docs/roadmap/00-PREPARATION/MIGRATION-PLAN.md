# Final service alignment plan

## Authority and inspect first

[ADR 008](../../adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md) supersedes consolidation/React instructions. Retain the seven existing service roots and Nuxt web. Inspect pom.xml, service poms, libs poms, all application YAML, infra/docker and web/package.json before selecting names. New business code since this audit must be inventoried before changing it; startup scaffolds are not migrated features.

## Ordered alignment

1. Preserve dirty/staged source on the application work branch without committing secrets. Record Java 21/Docker/disk prerequisites; the documentation branch is not a green app checkpoint.
1. Spike root Boot 4.1.1, Cloud gateway, Avro and test-library compatibility. Record exact resolved dependency versions and a passing gateway/business context test before aligning seven poms. Do not guess a compatible release train.
1. Replace JPA dependencies with JDBC in business services incrementally. Retain existing package roots, proposed api/application/domain/infrastructure direction, and actual identity_org schema name. No cross-service Java imports, SQL joins or foreign keys.
1. Change API gateway MVC scaffold to the approved reactive stack in an isolated change. Test routing/error serialization and token-derived identity header sanitization; business services remain private and enforce method/resource authorization.
1. Correct actual Compose SQL mount and Kafka/Registry listener addresses; validate syntax, then test real PostgreSQL/Kafka/Registry only after disk and daemon gates. Keep data volumes; never use down -v as troubleshooting.
1. Use libs/hrflow-contracts/events for Avro contracts. Prove schema compatibility and one synthetic event flow before copying an outbox starter. Owner write/audit/intent commit together; effect/inbox before consumer offset.
1. Create one proposed contracts/openapi.yaml; retain /api/v1 paths. Nuxt and Flutter use the same fixtures/mock. No React migration, second wire-authority file or unverified Flutter location.
1. Build the golden slice through Weeks 1–3, with real auth, JDBC, validation, tenant role/RLS, CI, safe logs and restore evidence. Later feature deltas repeat the same recipe.
1. Write a separate private self-host Compose for all final workloads; existing dependency-only Compose is not production. Measure total memory/disk/JVM/broker overhead before accepting host capacity. Add TLS access only after auth/secrets/firewall/restore gates.
1. Test clean multi-service restoration including DB/private files/event schemas/replay positions. Record data-loss/recovery measurements, not high-availability claims. AWS/Terraform/Kubernetes remain future improvement per latest instruction.

## Schedule rule

[Review](../ARCHITECTURE-REVIEW-V2.md) records the service/quality demand shortfall against unchanged 70 percent hours. Atomic decomposition must expose remaining Must work; it may not erase it or quietly relabel all work thirty minutes. Cut optional polish and unadmitted patterns first. If still over budget, report scope/hours/deadline conflict while preserving the final architecture and security/restore requirements.

## Recovery

Use a clean recovery branch from a verified old tag; do not reset dirty work or move tags. No green application tag exists yet. Unconverted weeks contain superseded monolith/local-worker instructions: consult PROGRESS-V2.md before execution. Every changed ID must map forward once its pair is upgraded.
