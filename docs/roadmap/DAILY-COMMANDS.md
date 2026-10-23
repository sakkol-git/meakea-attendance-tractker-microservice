# Daily commands

## Current source versus proposed targets

Run Git/Maven commands from repository root, Nuxt from web, Flutter from the confirmed mobile root. Existing service poms and infra/docker/docker-compose.yaml are real; contracts/openapi.yaml, infra/selfhost/compose.yaml and ops scripts remain proposed until their creation step passes. Do not run an absent target and call the missing file a valid red business test. No hrflow-app or React migration is part of the final design.

## Inspect and build

```bash
git status --short
rg --files services libs infra
mvn -o validate
mvn -pl services/identity-org -am test
mvn verify
npm --prefix web run build
python3 docs/roadmap/tools/docs-lint.py docs
```

The offline validate command passed the ten-project reactor; it does not compile Java. Full test/build requires Java 21, resolved dependencies and Docker for integration tests. Root Boot 4.1.1 and service 4.0.8 alignment is not complete. For another owner replace services/identity-org with its inspected existing module.

## One test and local run

Replace uppercase class/method tokens with names actually recorded by the inspect step. Proposed new golden-slice names are not existing test classes. Build required shared libraries first if the service consumes their artifacts.

```bash
mvn -pl services/identity-org -am install -DskipTests
mvn -pl services/identity-org -Dtest=RECORDED_CLASS#RECORDED_METHOD test
mvn -f services/identity-org/pom.xml spring-boot:run
docker compose -f infra/docker/docker-compose.yaml config --quiet
```

The install command prepares local reactor artifacts; skipped tests do not satisfy a gate. Existing Compose starts dependencies only and has known mount/listener defects; do not up it until repaired and disk/Docker prerequisites pass. Proposed production Compose is separate and must include all reviewed workloads.

## Private production Compose after creation

```bash
docker compose -f infra/selfhost/compose.yaml config --quiet
docker compose -f infra/selfhost/compose.yaml up -d
docker compose -f infra/selfhost/compose.yaml logs --tail=80 identity-org
docker compose -f infra/selfhost/compose.yaml exec postgres psql -U hrflow_runtime -d hrflow
```

Service names and DB role here are proposed manifest conventions; confirm generated config and role inventory first. Never dump credentials. Flyway executes through the reviewed migration/startup path with a separate role; no standalone unconfigured Maven Flyway goal is assumed.

## Checkpoint and return to green

Use an isolated commit for each step. Substitute the mapped actual tag; no existing tag is overwritten. A documentation commit is not a green application tag.

```bash
git diff
git diff --cached
git log -1 --oneline
git tag STEP_TAG
git switch -c recovery/current-stone LAST_GREEN_TAG
```

For a single un-deployed step commit, git revert HEAD preserves history. For a red/green pair, revert the green commit then its red commit only on a dedicated recovery branch after reviewing both. Applied migrations require forward repair or the reviewed recovery runbook: Git does not undo database changes.

## Operations after scripts are authored and checked

```bash
bash ops/deploy.sh
bash ops/selfhost-smoke.sh
bash ops/backup.sh
bash ops/restore-scratch.sh
bash ops/restore-clean.sh
bash ops/rollback-check.sh
```

[Runbooks](runbooks/README.md) define target, verification and failure behavior; destructive recovery never runs automatically. Restore includes DB, private files, event schema/config inventory and replay position. Kafka replay is not a substitute for a backup. AWS commands are deferred with cloud implementation.
