# Consolidation and client migration

## Inspect first

Read `pom.xml`, `services/attendance/pom.xml`, `services/api-gateway/pom.xml`, all application YAML files, and `web/package.json`. Verify again that business controllers/migrations are absent. If new code appears after this audit, inventory tests/routes before moving it.

## Safe migration order

1. Create a protected code checkpoint on the developer's weekly branch without including secrets or generated dependencies. The documentation branch is not an application checkpoint.
1. Create proposed `services/hrflow-app` using the current root Java 21/Boot 4.1.1 baseline; keep old service directories untouched until new app behavior and tests pass.
1. Retain domain names as packages and owner schemas. Initially implement only a database-backed diagnostic through private Caddy; never invent migrated behavior that did not exist.
1. Keep the root reactor until a deliberate build-profile change parks old scaffolds. Compile/test the new app explicitly; confirm no accidental launch of seven apps or broker stack.
1. Add Flyway migrations per schema in one ordered migration stream. Restrict repositories to owner packages; cross-domain reads use public module APIs or local projections, not cross-schema joins.
1. Write outbox in the domain transaction. A local scheduled dispatcher invokes independently idempotent notification/report handlers through application ports. Persist inbox plus effect in one transaction; mark completion only after all required handler receipts exist.
1. Preserve public `/api/v1` routes while OpenAPI becomes their single wire source. Controllers adapt domain objects to DTOs; do not publish JPA entities.
1. Preserve current Nuxt tree on a checkpoint, then create React sources in `web` on the build branch. Keep the lockfile only for its matching manifest; do not mix Nuxt dependencies into the React target.
1. B creates or locates Flutter source in proposed `mobile`; uses generated mock before backend is complete. Flutter dependency versions are selected and locked by B, not invented in docs.
1. Replace dependency-only Compose usage with proposed `infra/selfhost/compose.yaml`; app/PostgreSQL/Caddy only initially. Add private Kuma and tunnel connector after readiness. Keep old Compose as historical scaffolding, not production startup.

## Transaction and reliability change

Unlike the old distributed architecture, one JVM and one database can commit module operations locally. Keep domain ownership, audit, idempotency and outbox; do not imply Kafka acknowledgment or global exactly-once behavior. Dispatcher crash before receipt retries. Crash after handler commit finds inbox receipt. Provider sends remain outside DB transactions and can duplicate after timeout; in-app records are deduplicated.

## Contract and database reconciliation

Old endpoint coverage and owner schema names remain in reference documents. Replace owner service with owner package; runtime route is one app behind Caddy, port 8080 internally. Keep tenant constraints and no cross-module SQL joins. Proposed old outbox/inbox DDL is adapted to local handler receipts before implementation; legacy Kafka topic definitions are deferred compatibility references, not runtime requirements.

## Return to safety

Never delete or reset old scaffolds to make the build green. Use a new branch from a green tag, inspect diffs, and migrate one package at a time. If a new path fails, keep the old tree and reduce the next change. The new app needs its own tested migrations; copying proposed documentation SQL without tests is not migration completion.
