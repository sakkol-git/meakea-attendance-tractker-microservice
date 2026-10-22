# Repository audit

## Method and status

Inspected on 2026-10-23 using `git status --short`, `git log`, `rg --files`, build manifests, source/configuration reads, and the safe checks below. Labels describe evidence, not implementation aspirations.

| Feature | Status | Evidence | Consequence |
| --- | --- | --- | --- |
| Git baseline | **Unknown** release history | `git log` says no commits; staged and untracked work exists | Make documentation-only commits; preserve source index |
| Java services | **Partial** | `services/*/src/main/java` has seven startup classes | No working business services to preserve as deployables |
| Database tables | **Not started** | No migrations/entities in services | DDL in docs is proposed, not installed |
| Auth and tenant scope | **Not started** | No controllers/security policy | Security is week 2 gate |
| Outbox and Kafka | **Partial** | Dependencies/YAML and one Avro record only | Keep transactional outbox; park broker in outcome C |
| Web | **Verified** Nuxt scaffold | `web/package.json`, welcome component, passing build | React migration is future build work |
| Flutter | **Unknown** | No Flutter files found here | B establishes location in week 1 |
| Confidence model | **Verified** standalone prototype | Five passing tests in own directory | Deferred; not attendance integration |
| Compose | **Partial** | Syntax passes; mount/Registry defects in file | New narrow deployment config, no blind startup |
| Docker runtime | **Blocked** | `docker info` cannot find daemon socket | Start only after disk gate |
| CI and deployment | **Not started** | Empty `.github/workflows`; no production manifests | Week 1 CI, week 2 private host deployment |

## Actual command results

| Command | Result | Interpretation |
| --- | --- | --- |
| `mvn -o validate` | **PASS** ten reactor entries | POM validation only |
| `mvn -o test` | **FAIL** offline Avro compiler/Jackson dependencies unavailable | Stopped before compilation; Java target still unvalidated |
| `npm run build` in `web` | **PASS** Nuxt production build | Not React or HR functionality |
| `.venv/bin/python -m pytest -q` in confidence-service | **PASS** five tests, one warning | Prototype only |
| `docker compose -f infra/docker/docker-compose.yaml config --quiet` | **PASS** | Syntax, not connectivity |
| `docker info --format SERVER_VERSION` | **FAIL** daemon socket absent | No containers launched |
| `java -version` | **Verified** Java 17.0.19 | Project target is Java 21 |
| `/usr/libexec/java_home -V` | **Verified** JDK 17 and 23 only | Install/select target 21 during build-along |

`docker info` was actually invoked with Go template `{{.ServerVersion}}`; do not paste private daemon details into public issues. Build output was inspected; no running HR endpoints are claimed.

## Important mismatches

- `pom.xml` parent 4.1.1 differs from service parents 4.0.8; gateway Cloud 2025.1.3 exists but is unused in outcome C.
- Six `.yaml` resources contain properties syntax; attendance port is 8082 while old API design said 8102.
- Compose repeats `infra/docker` in its relative SQL mount and points Registry to nonexistent port 19092.
- Avro plugin reads an absent root contracts directory.
- Source is Nuxt while the latest user requirement explicitly selects React.
- Old architecture is seven services with Kafka-only integration; latest user scope allows consolidation and requires self-hosted Compose.

## Safe next action

Open the baseline and hosting assessment. Do not install large dependencies or start Docker while only about 5 GiB is free. The guide's proposed names must be inspected/created during implementation; they are not existing files.
