# Daily commands

## Working directory and unavailable targets

Run Git/Maven/ops commands from repository root, Flutter from the confirmed mobile root. services/hrflow-app, contracts/openapi.yaml, infra/selfhost/compose.yaml and ops scripts are proposed targets, not existing source. Inspect/test their creation step first. Never interpret “file absent” as the expected red business test.

## Inspect and build

```bash
git status --short
rg --files services libs infra .github
mvn -o validate
mvn -f services/hrflow-app/pom.xml test
mvn -f services/hrflow-app/pom.xml verify
npm --prefix web run build
flutter test
python3 docs/roadmap/tools/docs-lint.py docs
```

mvn -o validate works on current reactor but does not compile Java. Full app commands require created app and target JDK 21; current source still uses Nuxt for the web build.

## One test and local run

Record exact test class/method from inspection, substitute the uppercase tokens before running. After creation, start private app through the checked Compose manifest.

```bash
mvn -f services/hrflow-app/pom.xml -Dtest=RECORDED_CLASS#RECORDED_METHOD test
docker compose -f infra/selfhost/compose.yaml config --quiet
docker compose -f infra/selfhost/compose.yaml up -d
docker compose -f infra/selfhost/compose.yaml logs --tail=80 app
```

## Database and migrations

Use the actual private role/DB from safe host config; these baseline names are proposed. Never print credentials, bypass RLS casually or mutate production through a debug shell. Flyway runs through the app's reviewed startup/deploy migration path; no unconfigured standalone Maven Flyway goal is assumed.

```bash
docker compose -f infra/selfhost/compose.yaml exec postgres psql -U hrflow_runtime -d hrflow
bash ops/deploy.sh
```

## Checkpoint and return to green

Before each command confirm branch/status and that commit/tag belongs to the exact reviewed stone. STEP_TAG is a replacement for the mapped new actual tag. Do not overwrite any existing tag.

```bash
git diff
git diff --cached
git log -1 --oneline
git tag STEP_TAG
git switch -c recovery/current-stone LAST_GREEN_TAG
```

For a single isolated un-deployed step commit, git revert HEAD preserves history. Deployed/applied migrations require the runbook's forward repair or explicit scratch/production restore decision; Git does not undo DB data.

## Repeated operations

```bash
bash ops/selfhost-smoke.sh
bash ops/backup.sh
bash ops/restore-scratch.sh
bash ops/restore-clean.sh
bash ops/rollback-check.sh
```

Each script is authored/tested before first invocation. [Runbooks](runbooks/README.md) define target checks and failure behavior; destructive recovery is never an automatic cheat-sheet command.
