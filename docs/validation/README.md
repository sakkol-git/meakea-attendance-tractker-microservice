# Documentation validation record

Date: 2026-10-23. Scope: documentation suite, not application release certification.

## Current validation

The hosted secret-scan metadata follow-up is recorded in [Week 08 evidence](W08-EVIDENCE.md) and CI-SECRET-SCAN.json. It adds only exact immutable fingerprints for inspected receipt hashes and one deliberate fake-token fixture. Current-range scans pass, and positive controls still detect the fake token in a new commit. Application checks below remain the dated code checkpoint; hosted CI status is separate.

The newer focused-forms and first-use checkpoint in [Week 08 evidence](W08-EVIDENCE.md) records 139 web unit cases, 25 browser scenarios and 196 mobile cases on macOS and Linux, with three explicit mobile live-environment skips. The coordinated integration run executed foundation, backend, both clients and tooling. Its first attempt failed a reporting broker setup timeout and a web CSS format check; CSS formatting was repaired, the complete reporting reactor slice passed on rerun, and the complete web check passed. Current fresh Maven XML reports total 486 cases with zero failures, errors or skips. The original failed run remains evidence rather than being relabelled successful. CLIENT-FORMS-FIRST-USE.json binds the final source and logs; earlier receipts below are historical. The owner now authorizes committing, merging to main and pushing all three repositories, while deployment and stakeholder/device acceptance remain separate.

The newer [29 October usability polish checkpoint](W08-EVIDENCE.md) records 139 web unit cases, 23 browser scenarios and 194 mobile cases on macOS and local Linux, with 3 explicit mobile skips. Its current source/log receipt is CLIENT-UX-POLISH.json. The earlier CLIENT-UX-CHECKPOINT.json remains a dated checkpoint. Full Khmer localization, actual-device profiling and human accessibility/UAT observations remain open; no backend/provider or deployment acceptance is inferred from this UI pass.

The [29 October client UX checkpoint](W08-EVIDENCE.md) records the later navigation, mobile layout, form action and feedback pass. Its source/log receipt is CLIENT-UX-CHECKPOINT.json. Earlier client manifests and the client section of CURRENT-UAT-CODE.json remain historical checkpoints; do not treat their hashes as the current UI after this pass. Backend/provider evidence retains its original revision and scope.

Use [V2 validation log](../roadmap/V2-VALIDATION-LOG.md) for the final roadmap checkpoint commands and limits, [linter proofs](../roadmap/LINTER-PROOFS.md) for semantic-check verification, and [source of truth](../SOURCE-OF-TRUTH.md) for authority. The sections below retain historical results; they are not current product acceptance. Old 112-route/schema export tooling was superseded when long documents were split.

## Service configuration baseline

[Service configuration](../SERVICE-CONFIGURATION.md) records the later implementation baseline, environment files, tests and limits. SERVICE-CONFIGURATION-RESULTS.json contains its dated Maven/Python/live-loopback check receipt. Earlier V2 and baseline records remain checkpoint-specific; no old test result is silently promoted to production acceptance.

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

## Current supported UAT code receipt

[W09 UAT code handoff](W09-EVIDENCE.md) records the complete cross-project run, its preserved failures, narrow repairs and final provider reruns. CURRENT-UAT-CODE.json binds source hashes, exact repository revisions, current client manifests and test evidence for the supported code slice. It is implementation evidence, not stakeholder UAT acceptance or completed deployment/restore. Historical roadmap gates remain unchanged.
