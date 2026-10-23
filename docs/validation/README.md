# Documentation validation record

Date: 2026-10-23. Scope: documentation suite, not application release certification.

## Current validation

Use [V2 validation log](../roadmap/V2-VALIDATION-LOG.md) for the final roadmap checkpoint commands and limits, [linter proofs](../roadmap/LINTER-PROOFS.md) for semantic-check verification, and [source of truth](../SOURCE-OF-TRUTH.md) for authority. The sections below retain historical results; they are not current product acceptance. Old 112-route/schema export tooling was superseded when long documents were split.

## Historical structural checks

```bash
python3 docs/validation/validate.py --export-dir /tmp/hrflow-doc-check
```


The standard-library validator checks Markdown link targets/heading anchors, fenced/inline JSON syntax, unique endpoint IDs/method-path pairs, requirement/open-decision IDs, balanced code fences, and ADR sections. It exports proposed SQL and Mermaid blocks for independent validators. It does not prove JSON schema conformance, correct authorization, or application implementation.

## Mermaid parser check

Temporary development-only dependencies were installed outside the repository:


Command reference: `npm install --prefix /tmp/hrflow-doc-validation --ignore-scripts --package-lock=false mermaid@11.17.2 jsdom@26.1.0`.

Command reference: `node docs/validation/check-mermaid.mjs /tmp/hrflow-doc-validation /tmp/hrflow-doc-check/diagrams.json`.


Result: 15/15 diagrams parsed successfully. Initial sequence-label semicolons caused parse failures and were replaced with commas; the final run passed. This is a Mermaid syntax check, not a browser-rendered visual layout review. Project package manifests/lockfiles were not changed.

## PostgreSQL DDL and integrity checks

The proposed SQL blocks were exported and executed with stop-on-error in a new isolated temporary cluster:


Command reference: `/opt/homebrew/opt/postgresql@18/bin/initdb -D /tmp/hrflow-doc-pg -A trust --no-locale`.

Command reference: `/opt/homebrew/opt/postgresql@18/bin/pg_ctl -D /tmp/hrflow-doc-pg -l /tmp/hrflow-doc-pg.log -o '-k /tmp -p 55439 -h 127.0.0.1' -w start`.

Command reference: `psql -h 127.0.0.1 -p 55439 -U apple -d postgres -v ON_ERROR_STOP=1 -f /tmp/hrflow-doc-check/schema.sql`.

Command reference: `psql -h 127.0.0.1 -p 55439 -U apple -d postgres -v ON_ERROR_STOP=1 -f docs/validation/constraint-smoke.sql`.

Command reference: `/opt/homebrew/opt/postgresql@18/bin/pg_ctl -D /tmp/hrflow-doc-pg -m fast -w stop`.


DDL execution passed: six schemas, 80 tables, associated indexes/constraints and integrity extensions were accepted by PostgreSQL 18. Five focused checks passed: cross-tenant employee FK rejected, second open session rejected, invalid checkout interval rejected, negative leave availability rejected, duplicate inbox event rejected. Fixtures run inside a transaction and roll back. No project application database was used. The first sandboxed initialization/connection was blocked; temporary-cluster operations succeeded with explicit approval. PostgreSQL 16 is the repository target; execution on version 16 remains a required implementation gate. These checks do not prove row-level security, grant policy, race handling, migration upgrades, or acceptable query plans.

## Repository review and earlier application checks

Reconnaissance inspected source/configuration, Maven manifests, the Nuxt application, Python scoring/training/tests, Compose/bootstrap/topic script, documentation and empty ADR/workflow directories. The review found no business controllers, persistence migrations, Java tests, Flutter files, CI workflows, Terraform, Kubernetes, or implemented outbox/inbox workers.

The earlier 2026-10-22 analysis executed these application checks before the documentation rewrite:

| Command | Observed result | Limit |
|---|---|---|
| `.venv/bin/python -m pytest -q` from confidence-service | 5 passed, one dependency warning | Prototype tests only; root invocation had import collection failure |
| `npm run build` from web | Passed | Nuxt starter only |
| `mvn -o validate` | All ten reactor projects passed | Does not compile/run Java; shell JDK was 17 while target is 21 |
| `docker compose -f infra/docker/docker-compose.yaml config --quiet` | Passed | Syntax, not dependency startup/connectivity |
| `bash -n infra/scripts/create-topic.sh` | Passed | Syntax, not broker provisioning |

Application checks were not rerun merely for a documentation change. No source/infrastructure dependency repair is claimed.

## Manual consistency review and remaining limits

Reviewed shared route prefixes/owners/ports, identity_org naming, role/resource checks, proposed event envelope/topic keys, leave compensation semantics, local transaction/offset boundaries, tenant-scoped FK/uniqueness, and requirement-to-milestone references. Corrected session event `sessionId` mapping, cutoff-kind uniqueness, preference-set ETag ownership, non-null delivery targets, and leave-day parent employee consistency during review. ADRs distinguish the pre-existing accepted boundary baseline from proposed policy/security/deployment changes. O01–O08 remain visible decision gates; future machine contracts must validate full request/response/event schema conformance. No load/security/end-to-end/projection-rebuild/production-restore claim is made by this documentation validation.

See [architecture](../Architecture.md), [contracts](../REST-API-DATABASE-DESIGN.md), and [plan](../PLAN.md).

## V2 roadmap audit data

The original 182-step inventory is preserved in ROADMAP-V1-BASELINE.json; the structured first-pair draft is in ROADMAP-V2-PAIR-12-DRAFT.json. Both are documentation audit data, not application inputs. [Progress](../roadmap/PROGRESS-V2.md), [validation log](../roadmap/V2-VALIDATION-LOG.md) and [interim metrics](../roadmap/CHANGELOG-V2.md) explain limitations and resumption.

Final parsed instructions/capacity are in ROADMAP-V2-FINAL-LEDGER.json; V2-SAMPLED-STEPS.json records the 36 static samples, V2-COMMAND-RESULTS.json the actual safe commands, and DOCS-LINT-V2-PROOFS.json the isolated negative fixtures. These are audit data; Markdown guides remain the execution reference.

INCOMING-DOCS-V2-SNAPSHOT.json preserves the complete pre-pass Markdown input, including incoming uncommitted documentation. It is a non-executable historical audit archive; the current Markdown/ADR 008 guide remains authoritative.
