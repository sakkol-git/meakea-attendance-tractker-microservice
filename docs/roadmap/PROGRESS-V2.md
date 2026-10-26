# Roadmap V2 progress

## Documentation completion

The original V2 authoring checkpoint completed. ADR 010 adds the dual-validation boundary and ADR 011 selects Spring Security OAuth2 Resource Server instead of custom JWT filters without changing the 448 IDs or 96 stones. W02-S01 through W02-S23 retain evidence for their prior scope, including the identity-org vertical slice and client/API collections, but W02-S07/S14/S15/S22 must be repeated because that evidence predates the complete service-side JWT validation matrix. The last recorded green application tag remains `bd53438`; the current position is the ADR-010/011 verification rework before W02-S24.

## Confirmed plan

Start Monday 2026-10-26, release rehearsal Friday 2027-01-15, twelve-week boundary Monday 2027-01-18. A=24 and B=24 h weekly; at most 70 percent planned, Weeks 4/8 fifteen percent lower cap. Final planned total is 301.25 h including stated review/mentoring and reading inside durations, below 393.12 h capacity. These are estimates to recalibrate against actual Week-1 pace, not a guaranteed completion date.

Final design retains seven Java services, reactive gateway, Spring Data JPA, Kafka/Avro/Registry, Nuxt static client, Flutter and existing confidence integration. ADR 008 supersedes C/React/local-dispatch; ADR 010 supersedes its gateway-header trust clause and requires independent JWT validation at the gateway and every Java service; ADR 011 requires Spring Security OAuth2 Resource Server for each gate. AWS/Kubernetes execution is future improvement by latest user instruction; preserve its approved production topology and create no cloud resources.

## Phase ledger

| Phase | Authoring state | Evidence | Runtime state |
| --- | --- | --- | --- |
| 1 architecture review | **PASS** | Every ADR verdict, ADR 008, diagrams/data/event/failure boundaries, explicit initial/final capacity | Local foundation **PASS**; production topology unverified |
| 2 objective audit | **PASS** | All 182 baseline rows, weekly scorecards/worst 15 and upgrade plan | Auth slice 7/158 implemented |
| 3 shared assets | **PASS** | Standards/review/tests/security/API/commands/golden slice/runbooks/primers | Contract verifier, Compose and auth smoke now run; operational drills remain |
| 4 decomposition | **PASS** | All twelve weeks, two-to-six steps/stone, eight stones/week, owner budgets and old/draft maps | 61 steps have evidence; current step W02-S24 |
| 5 cross-cutting | **PASS** | Fourteen Q chains, week gates, final scorecards/global/risk/master consistency | Auth and client checks pass; full product gates remain open |
| 6 tooling | **PASS** | Standard-library semantic checks; 23 isolated broken-fixture proofs | Current documentation lint passes 305 files |
| 7 author simulation/checks | **PASS** with limits | Cold-read path, 36 static samples, safe commands, seven Mermaid/three Java examples, dependency/capacity/coverage and final metrics | Physical devices, host, restore, business, load and user acceptance remain **NOT RUN** |

## Decisions and constraints carried forward

- No extra architecture approval is required for the accepted local guide. HR policy/consent is not inferred from design approval.
- Java 21, Docker and the pinned Flutter SDK run on the development Mac. The user designated a separate Acer Nitro 5 with a reported Core i5, 16 GB RAM and 2 TB hard-disk capacity as the intended self-host; W02-S24 must still verify its exact hardware, OS, free/healthy disk and runtime before deployment work.
- The production target is selected, but its external encrypted backup/recovery keys, public domain/network reachability, power policy and unattended reboot behavior remain unknown. Private Caddy internal TLS avoids removing Secure-cookie protection or assuming a purchased domain.
- Source now contains the local foundation, synchronized OpenAPI bundles and the eight-operation authentication/profile/password slice. Most business endpoints remain scaffolding or pending; the contract ledger is the implementation authority.
- All critical security/tenant/DB/event/restore checks remain required. If actual learning or work exceeds the itemized estimate, cut Stretch first and record the residual scope/date conflict; no weekends or hidden use of the 30-percent reserve.

## Current application checks and limitations

[V2 validation log](V2-VALIDATION-LOG.md), [Week 1 evidence](../validation/W01-EVIDENCE.md), [Week 2 evidence](../validation/W02-EVIDENCE.md) and [linter proofs](LINTER-PROOFS.md) contain commands and results. Docker Compose now runs PostgreSQL, Kafka, schema registry, identity-org and the gateway locally; gateway health passes. The full Maven reactor, Nuxt checks/build, Flutter analysis/tests/build, contract validation, 158-operation Postman collection and documentation lint pass for their recorded revisions. The current contract records 8 implemented operations and 150 pending operations. All 40 current identity unit/HTTP tests, five gateway authorization tests and six Docker-backed identity integration tests pass, including the profile/password PostgreSQL scenarios. Password recovery remains blocked on its tenant and token-delivery contract. Real-host deployment, external-backup restore, physical-device acceptance, business modules and release gates remain **NOT RUN**.

The complete incoming Markdown input is durably archived in docs/validation/INCOMING-DOCS-V2-SNAPSHOT.json. All 94 source/config fingerprints and eight original non-document staged blobs match the pre-pass snapshot. Documentation commits use an isolated index, no source reset/history rewrite or cloud deployment. Final saved-tree validation is run after the finishing commit so tracked artifacts cannot be missing behind a clean working-tree lint.

## Resuming development

Open [START-HERE](START-HERE.md), [YOU-ARE-HERE](YOU-ARE-HERE.md), then repeat W02-S07/S14/S15/S22 under ADR 010/011 before [W02-S24](02-IDENTITY-AND-HOST/BUILD-GUIDE-PART-5.md). Verify the existing gateway and identity-org resource-server chains against the full negative and routed two-gate matrix; do not add custom JWT filters. After both gates have current evidence, record the production-host facts and continue through the private manifest, secret, backup, restore and Week 2 gate steps. W03-S01–S05 own OAuth2 resource-server configuration for the remaining five services.

[STEP-ID-MAP](STEP-ID-MAP.md), [DRAFT-ID-MAP](DRAFT-ID-MAP.md) and [finalization map](FINALIZATION-ID-MAP.md) preserve older positions/tags. [CHANGELOG-V2](CHANGELOG-V2.md) contains final metrics and per-week hours; [QUALITY-COVERAGE](QUALITY-COVERAGE.md) contains every senior-quality gate. Five-minute routine: read blocker, check branch/status, choose one next mapped ID, read prerequisite/Expect/Undo, record energy/time and start or stop. End by saving the next exact command and protecting rest days.

## Saved-tree verification

A fresh git archive of committed checkpoint e2ca576, including docs and root README, passed the extended linter: 277 Markdown files, zero errors and zero warnings. This verifies tracked-file completeness independently of untracked working-tree files. The final tenant-worker metadata clarification and this receipt are also linted before their finishing commit; runtime/native-viewer limits above remain unchanged.
