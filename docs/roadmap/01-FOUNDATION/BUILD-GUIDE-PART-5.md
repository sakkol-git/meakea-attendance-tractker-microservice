# Week 01 build guide part 5

> **Navigation:** [Overview](README.md) · [Dashboard](../YOU-ARE-HERE.md) · [Golden slice](../GOLDEN-SLICE.md)

## Stone 5 — Repeatable tools and event proof

**You will have:** Syntax passes; bind-mount source and event source directory exist and host/container listener addresses agree.

### W01-S23 — Keep PostgreSQL integration tests real (60–60 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/TESTCONTAINERS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `services/identity-org/pom.xml`, `services/identity-org/src/test/java/com/hrflow/identityorg/DiagnosticDatabaseTest.java`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Review the container fixture from W01-S08: pin PostgreSQL 16 image, use mapped connection details, and remove any fallback to H2 or silent skip when Docker is absent. Prove an invalid DB constraint reaches PostgreSQL.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
mvn -f services/identity-org/pom.xml test
```

**Expect:** Container-backed test passes; stopping Docker produces an environment failure, never a skipped success.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Silently skipping a required integration test when Docker is unavailable creates a false green gate.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s23` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S24 — Inspect the existing Avro and listener paths (30–30 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/KAFKA-BASICS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W01-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Read the actual avsc under libs/hrflow-contracts/events, libs poms and Compose. Record host/container listener addresses and SQL bind-mount source; identify Registry bootstrap mismatch without changing volumes.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
docker compose -f infra/docker/docker-compose.yaml config --quiet
```

**Expect:** Compose syntax exits zero; findings separately name bad mount/listener values because syntax validation does not prove connectivity.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Silently skipping a required integration test when Docker is unavailable creates a false green gate.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s24` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S25 — Time-box Avro Registry compatibility (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/KAFKA-BASICS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `scripts/acceptance/avro-spike.sh`, `docs/validation/W01-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. After disk/Docker gates, use a disposable isolated broker/Registry environment to trial the exact serializer/runtime versions from the spike. Record accepted compatible evolution and rejected incompatible schema. Output is a finding, not a production publisher. Create the proposed spike script with explicit synthetic produce/consume, Registry compatible/breaking-schema assertions and nonzero failures; record its exact pinned serializer/tool versions. Use an isolated topic and bounded waits.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/avro-spike.sh compatibility
```

**Expect:** Finding records resolved versions, actual round-trip result or exact blocker; an unavailable image/dependency is not a business pass.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Calling a command invocation proof without inspecting the recorded assertion/output.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s25` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S26 — Record the first event round-trip result (45–45 min)

**Why:** A owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/KAFKA-BASICS.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `docs/validation/W01-EVIDENCE.md`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Use only synthetic tenant/employee data and the inspected event schema in the isolated spike. Record produce/consume and a duplicate replay observation with a bounded wait. Explain why this is not yet outbox/inbox exactly-once processing.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash scripts/acceptance/avro-spike.sh round-trip
```

**Expect:** Actual producer/consumer evidence is recorded or the spike is blocked; no durable business delivery is claimed.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Treating broker acknowledgment and the database mark-sent as one atomic action misses duplicate publication.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s26` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S27 — Build pinned private candidate images (60–60 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/DOCKER-COMPOSE.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `infra/selfhost/Service.Dockerfile`, `ops/build-images.sh`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect actual jar names from Maven outputs. Add the proposed shared Dockerfile/builder with pinned reviewed Java 21 runtime image digests, non-root users and no secrets; build each with the matching existing service context. Record digests, total size and memory gate. This does not prove the full seven-service host fits.
1.  Use one proposed generic Service.Dockerfile and an allowlisted ops/build-images.sh that reads actual Maven jar final names, packages selected reviewed modules and records immutable digests. Week 1 builds only identity/gateway; the same tested template builds the other owners after Week-3 context alignment. Pin Java 21 base digest, non-root runtime and copy only the verified jar; no secrets or shell-supplied arbitrary module paths. Bash syntax alone is not the image build proof: run the reviewed builder and inspect both digests before scanning/deploy.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
bash -n ops/build-images.sh
```

**Expect:** Both local candidate images build from reviewed jars, run as non-root and have recorded digests; absent/pull-failed images block the CI image-scan gate.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Accepting an empty context/class import as a successful service test hides dependency or configuration defects.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s27` after staging only exact reviewed paths and recording command/exit/report evidence.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

### W01-S28 — Repair the actual dependency Compose paths (30–30 min)

**Why:** B owns one observable result in the preserved weekly objective.

**Read first (up to 10 min, included):** [One concept](../primers/DOCKER-COMPOSE.md). Reuse prior learning; no new concept on Thursday/Friday.

**Before you start:**

- [ ] Earlier prerequisite result is green; an intended red assertion is isolated on the work branch. A critical failed prior gate replaces this action.

**Do:**

1. Inspect first: review current equivalents for `infra/docker/docker-compose.yaml`, `libs/hrflow-contracts/pom.xml`. Use those actual names; if absent these paths are explicitly proposed. Inspect and choose the next unused Flyway version before any schema change, never edit an applied migration.
1. Inspect the real files: use ./postgres-init.sql relative to Compose, point Registry to kafka:29092 and Avro sourceDirectory to the existing events directory under this library. Bind local development ports to loopback, pin the formerly floating UI image after verification or omit optional UI. Preserve data volumes.
1. Run the exact check below after target creation. Record the semantic assertion, command/exit and report path in this step commit; do not treat syntax/config validation as proof of deployment, recovery or broker delivery.

```bash
docker compose -f infra/docker/docker-compose.yaml config --quiet
```

**Expect:** Syntax passes; bind-mount source and event source directory exist and host/container listener addresses agree.

```text
Evidence: exact command, exit code, safe result line and report path.
Missing target/prerequisite: BLOCKED; never a fabricated successful result.
```

**Common mistake:** Calling a command invocation proof without inspecting the recorded assertion/output.

**Undo:** For this isolated un-deployed commit run `git revert HEAD`. Applied SQL/data needs a new forward migration or the reviewed [rollback/restore procedure](../runbooks/ROLLBACK.md); Git does not reverse data. For temporary read-only findings this reverses only their recorded commit.

**Senior habit:** Keep a reproducible evidence trail and a safe rollback boundary.

**Check:**

- [ ] This result and its stated denial/failure assertion have real report evidence; red commits stay untagged.
- [ ] Committed with message `feat(w01): w01-s28` after staging only exact reviewed paths and recording command/exit/report evidence.
- [ ] Tagged `w01-s28-green` after the [ten-minute review](../CODE-REVIEW-CHECKLIST.md), all stone assertions and current private app startup pass; never overwrite an old tag.

**If it fails:** Save the exact failed assertion or prerequisite; use [week troubleshooting](TROUBLESHOOTING.md) and the [stuck protocol](../STUCK-PROTOCOL.md). Stop at the stated cap, keep the next smallest repair visible and spend only reserved recovery time. Security/data/restore failures cannot be waived.

**Status:** **Not run**; proposed implementation has not been executed by this documentation upgrade.

## Continue

[Next part](BUILD-GUIDE-PART-6.md).
