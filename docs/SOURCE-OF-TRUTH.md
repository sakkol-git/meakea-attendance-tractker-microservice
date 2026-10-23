# Documentation source-of-truth map

## Purpose and reading path

Use this map to find who owns a requirement, which procedure to follow and what evidence proves it. It covers product, architecture, domains, interfaces, security, quality, delivery, operations, learning and documentation governance. The [complete document catalog](DOCUMENTATION-CATALOG.md) classifies every tracked Markdown document, including root and prototype READMEs; this map assigns topic authority without duplicating their specifications.

For development, open [START-HERE](roadmap/START-HERE.md), then [YOU-ARE-HERE](roadmap/YOU-ARE-HERE.md), then the current week's README and next BUILD-GUIDE step. For a design question, use the topic tables below. For a claimed result, use the evidence section. [Documentation handbook](README.md) and [root README](../README.md) are introductions, not competing specifications.

## Authority and conflict resolution

Authority is scoped to the question. A newer timestamp, file location or longer document does not automatically override an accepted decision.

| Document role | Authority | Conflict rule |
| --- | --- | --- |
| Approved design | [Architecture](Architecture.md) and its four approved parts define topology and behavior | Apply formal supersession in the [ADR index](adr/README.md); ADR 008 governs the final service design |
| Decision status | ADR index and [decision register](roadmap/DECISION-REGISTER.md) identify accepted, proposed and superseded scope | Proposed HR policy is not approved by architecture acceptance; do not promote it silently |
| Interface and persistence specification | [API/database design](REST-API-DATABASE-DESIGN.md) owns cross-service contract and data rules | Keep it consistent with approved architecture; resolve material changes through an ADR |
| Execution | Current weekly BUILD-GUIDEs own ordered work; [standards](roadmap/ENGINEERING-STANDARDS.md) and gates define acceptance | A step cannot waive a security, tenant or recovery gate to meet the schedule |
| Progress and capacity | [Dashboard](roadmap/YOU-ARE-HERE.md) owns current position and numeric capacity inputs | Weekly totals derive from actual step estimates; summaries must not introduce another capacity input |
| Implementation evidence | Actual source, migrations, executable contracts and recorded commands establish what exists or ran | Evidence cannot silently change approved intent; record the mismatch and repair or supersede it |
| Navigation and teaching | READMEs, primers, pattern lessons and this map explain or route | They cannot add mandatory services, redefine wire contracts or claim implementation |
| Historical and audit records | Baselines, earlier progress, rejected alternatives and preserved references explain prior states | Keep dated findings intact; never execute superseded instructions as the current plan |

A requirement/implementation mismatch is a visible gap, not permission to pretend either side is correct. Record the paths and exact result, consult the owning specification and stop dependent work if correctness or security is unresolved. A design change needs a superseding ADR, decision-register update, affected contract/guide changes and revised evidence gates in the same reviewed change. Do not rewrite old ADRs or move existing tags.

### Confirmed baseline and limits

The accepted target is seven Java services, reactive gateway, explicit JDBC, six owner schemas, Kafka KRaft, Avro/Schema Registry, Nuxt/Vue, Flutter and the Python confidence integration. ADR 008 supersedes the monolith/React/local-dispatch clauses of earlier decisions. PostgreSQL schema ownership separates domains; tenant isolation additionally requires trusted tenant provenance, scoped queries, constraints and adopted RLS. Neither a schema nor an architecture diagram proves isolation.

Compose on one confirmed computer is the twelve-week deployment target. AWS/Terraform/Kubernetes remain future design, with execution deferred by the latest user instruction. Do not create cloud resources or infer free credits/budget. A single host is not high availability; an external disk is not automatically off-site; append-only audit rows do not establish cryptographic tamper evidence or legal compliance.

At the documentation checkpoint, the dashboard records W01-S01 unstarted, 24 hours each per week, 70 percent planned, lighter Weeks 4/8 and protected rest days. Start is 2026-10-26; target release rehearsal is 2027-01-15. The dashboard and weekly evidence own later progress: do not keep copying these values into new documents. All implementation steps were **Not run** at authoring completion.

## Product, scope and domain ownership

The approved domain chapters describe target behavior; the Must/Stretch boundary and checklist determine the twelve-week subset. They do not imply that every future-design detail is scheduled. Week links are execution owners, not evidence of completion.

| Topic | Specification owner | Execution or acceptance owner |
| --- | --- | --- |
| Product purpose, actors and system context | [Architecture part 01](reference/INCOMING-ARCHITECTURE-V2-01.md) | [Handbook introduction](README.md) explains; [global checklist](roadmap/GLOBAL-CHECKLIST.md) traces scope |
| MVP, Must/Should/Stretch and cuts | [Decision register](roadmap/DECISION-REGISTER.md) | [Roadmap](roadmap/ROADMAP.md) and global checklist |
| Tenant onboarding, identity and access | [Architecture parts 01](reference/INCOMING-ARCHITECTURE-V2-01.md) and [02](reference/INCOMING-ARCHITECTURE-V2-02.md) | [Week 02](roadmap/02-IDENTITY-AND-HOST/README.md) |
| Organization, departments, positions, employees and admin | [Service catalogue](reference/INCOMING-ARCHITECTURE-V2-02.md) and [API/data rules](REST-API-DATABASE-DESIGN.md) | [Week 03](roadmap/03-ORGANIZATION-AND-ADMIN/README.md) |
| Attendance, shifts, check-in/out and corrections | [Attendance deep dive](reference/INCOMING-ARCHITECTURE-V2-02.md) | [Week 04](roadmap/04-ATTENDANCE-CORE/README.md) |
| Leave, balances, reservation and cancellation | [Leave domain](reference/INCOMING-ARCHITECTURE-V2-02.md); [ADR 004](adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md) remains proposed policy | [Week 06](roadmap/06-LEAVE-WORKFLOW/README.md); obtain HR policy sign-off |
| Material/general requests and approvals | [Request workflow](reference/INCOMING-ARCHITECTURE-V2-03.md) | [Week 07](roadmap/07-REQUESTS-AND-APPROVALS/README.md) |
| Private attachments and download authorization | [API/data rules](REST-API-DATABASE-DESIGN.md) and [security baseline](roadmap/SECURITY-BASELINE.md) | Week 07 gate and [attachment primer](roadmap/primers/ATTACHMENT-SECURITY.md) |
| In-app notifications and delivery receipts | [Notification pipeline](reference/INCOMING-ARCHITECTURE-V2-03.md) | [Week 05](roadmap/05-OUTBOX-AND-NOTIFICATIONS/README.md); push/SSE remain Stretch |
| Reporting, read models, CSV and audit | [Reporting/data chapters](reference/INCOMING-ARCHITECTURE-V2-03.md) | [Week 09](roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md) |
| Confidence scoring and human review | [Confidence design](reference/INCOMING-ARCHITECTURE-V2-03.md) | [Attendance work](roadmap/04-ATTENDANCE-CORE/README.md); prototype tests do not prove integration or policy approval |
| Business policy, privacy, consent and retention | [Privacy goals](reference/INCOMING-ARCHITECTURE-V2-04.md), [ADR 004](adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md) and [risk register](roadmap/RISK-REGISTER.md) | Record stakeholder approval and fixtures before dependent behavior; no compliance claim |

## Architecture, contracts and client ownership

| Topic | Canonical owner | Supporting procedure or evidence |
| --- | --- | --- |
| Runtime topology, synchronous boundaries and service ownership | [Architecture](Architecture.md) and [ADR 008](adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md) | [Architecture review](roadmap/ARCHITECTURE-REVIEW-V2.md) diagrams and failure modes |
| Package layout, dependency direction and architecture fitness | [Engineering standards Q10/Q14](roadmap/ENGINEERING-STANDARDS.md) | [Architecture review](roadmap/ARCHITECTURE-REVIEW-V2.md) and [Week 03](roadmap/03-ORGANIZATION-AND-ADMIN/README.md) tests |
| Environment files, service ports and runtime baseline | [Service configuration](SERVICE-CONFIGURATION.md) | Actual application YAML/.env.example and security/configuration tests; no business acceptance implied |
| Tool/build versions, compatibility and dependency pins | [Toolchain reference](roadmap/TOOLCHAIN-REFERENCE.md) | Inspect actual manifests/lockfiles; distinguish observed versions from target alignment |
| HTTP routes, versioning, methods, status, pagination and errors | [API/database design](REST-API-DATABASE-DESIGN.md) | [API guidelines](roadmap/API-GUIDELINES.md) and [Week 01](roadmap/01-FOUNDATION/README.md) contract checks |
| OpenAPI and client/server contract agreement | [Engineering standards Q01](roadmap/ENGINEERING-STANDARDS.md) | One proposed contracts/openapi.yaml is created/checked in Week 01; no existing executable contract is inferred |
| Domain schema ownership, constraints and migrations | [API/database design](REST-API-DATABASE-DESIGN.md) and [ADR 008](adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md) | Q07; real Flyway migrations own installed DDL once created, not reference SQL exports |
| Event envelopes, Avro schemas, topic keys and compatibility | [Event backbone](reference/INCOMING-ARCHITECTURE-V2-02.md) and ADR 008 | Inspect libs/hrflow-contracts/events; [Week 01](roadmap/01-FOUNDATION/README.md) compatibility and [Week 05](roadmap/05-OUTBOX-AND-NOTIFICATIONS/README.md) replay tests |
| Outbox/inbox, acknowledgments, ordering and retry recovery | ADR 008 and [API/database design](REST-API-DATABASE-DESIGN.md) | [Week 05](roadmap/05-OUTBOX-AND-NOTIFICATIONS/README.md); local-only dispatch is superseded |
| Transactions, duplicate requests, exact balances and concurrency | [Engineering standards Q07/Q08](roadmap/ENGINEERING-STANDARDS.md) and API/data rules | [Weeks 04](roadmap/04-ATTENDANCE-CORE/README.md), [05](roadmap/05-OUTBOX-AND-NOTIFICATIONS/README.md), [06](roadmap/06-LEAVE-WORKFLOW/README.md) and [07](roadmap/07-REQUESTS-AND-APPROVALS/README.md) negative/race gates |
| Nuxt web, Flutter mobile, accessibility and offline commands | [Client chapters](reference/INCOMING-ARCHITECTURE-V2-03.md) | [Week 08](roadmap/08-WEB-AND-MOBILE-INTEGRATION/README.md); preserve inspected client roots and the shared contract |
| Python prototype setup and model experiments | [Prototype README](../ml/confidence-service/README.md) explains existing prototype commands | Approved confidence chapter governs integration; model training breadth is outside the minimum path |
| Cloud topology and infrastructure as code | [Architecture deployment chapter](reference/INCOMING-ARCHITECTURE-V2-03.md) | ADR 008 defers execution; no cloud deployment or cost approval is recorded |

## Security and engineering quality ownership

[ENGINEERING-STANDARDS](roadmap/ENGINEERING-STANDARDS.md) defines the fourteen Q rules. [QUALITY-COVERAGE](roadmap/QUALITY-COVERAGE.md) owns their introducing weeks, enforcing IDs and gate links; complete traceability is not a passing runtime result.

| Topic | Canonical owner | Verification owner |
| --- | --- | --- |
| Contract, error shape and layered validation | Standards Q01–Q03; [API guidelines](roadmap/API-GUIDELINES.md) | Weekly contract, error and invalid-input gates |
| Tenant provenance, query scope, RLS and pool isolation | [ADR 008](adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md), retained [ADR 007 safeguards](adr/007-QUALITY-AND-TENANT-DEFENSE.md), standards Q04 | [Week 03 gate](roadmap/03-ORGANIZATION-AND-ADMIN/VERIFY-AND-GATE.md) and shared cross-tenant suite for each route |
| Login, refresh reuse, method roles, cookies/CSRF and throttling | [Security baseline](roadmap/SECURITY-BASELINE.md), standards Q05 | [Week 02 gate](roadmap/02-IDENTITY-AND-HOST/VERIFY-AND-GATE.md); trusted gateway header/direct-ingress tests remain required |
| Secrets, environment config, rotation and sensitive logging | Security baseline, standards Q06/Q11 | [Week 02](roadmap/02-IDENTITY-AND-HOST/README.md), [Week 09](roadmap/09-REPORTING-AUDIT-OBSERVABILITY/README.md), rotation and CI evidence |
| Data integrity, reliability and graceful shutdown | Standards Q07/Q08 | Relevant owner tests plus crash/replay/concurrency gates; never waive for schedule |
| Test levels, naming, fail-first work and container debugging | [Test strategy](roadmap/TEST-STRATEGY.md), standards Q09 | Real PostgreSQL/Kafka tests, scoped coverage and a few client smoke tests |
| Architecture tests and code conventions | Standards Q10/Q14 | Build-enforced boundaries, formatter and static analysis |
| Logs, correlation/tenant fields, metrics, readiness and alerts | Standards Q11 and [observability design](reference/INCOMING-ARCHITECTURE-V2-04.md) | [Week 09 gate](roadmap/09-REPORTING-AUDIT-OBSERVABILITY/VERIFY-AND-GATE.md); earlier increments begin Week 02 |
| CI, secret/dependency/image scanning and pinned releases | Standards Q12 and [toolchain reference](roadmap/TOOLCHAIN-REFERENCE.md) | [Week 01 gate](roadmap/01-FOUNDATION/VERIFY-AND-GATE.md) and actual CI reports; missing scanner results are not passes |
| Self-review, pattern admission and definition of done | [Review checklist](roadmap/CODE-REVIEW-CHECKLIST.md), [pattern path](roadmap/PATTERNS-LEARNING-PATH.md), [definition of done](roadmap/DEFINITION-OF-DONE.md) | Review before every green stone tag; lessons do not mandate optional refactors |
| Golden feature recipe and later deltas | [Golden slice](roadmap/GOLDEN-SLICE.md) | Weeks 01–03 first reference feature; later steps repeat it with explicit deltas |
| Release readiness and known limitations | Standards Q13 and definition of done | [Week 11 scorecard](roadmap/11-HARDENING-AND-PILOT/VERIFY-AND-GATE.md) and [Week 12 scorecard](roadmap/12-RELEASE-AND-DEMONSTRATION/VERIFY-AND-GATE.md) |

## Delivery, operations and recovery ownership

| Topic | Canonical owner | Supporting procedure or evidence |
| --- | --- | --- |
| Position, next command, blockers and capacity inputs | [Dashboard](roadmap/YOU-ARE-HERE.md) | Update during development; do not use authoring PROGRESS as product status |
| Calendar, pace, weekly allocation and Must gates | [Roadmap](roadmap/ROADMAP.md), weekly READMEs and VERIFY-AND-GATE files | [PLAN summary](PLAN.md), [milestone map](roadmap/MILESTONE-MAP.md) and [global checklist](roadmap/GLOBAL-CHECKLIST.md) derive from them |
| Ordered implementation and intended result | Each week's BUILD-GUIDE and linked parts | Read its Why/Do/Expect/Undo and linked primer; [catalog](DOCUMENTATION-CATALOG.md) lists every part |
| Week-specific diagnosis and stalled work | Weekly TROUBLESHOOTING files | [Stuck protocol](roadmap/STUCK-PROTOCOL.md); log real blocker rather than guessing names |
| Git commits, green tags and return to known good | [Git safety](roadmap/GIT-SAFETY.md) and weekly steps | [Daily commands](roadmap/DAILY-COMMANDS.md); protect dirty/untracked work and never move old tags |
| Old and intermediate step positions | [Original ID map](roadmap/STEP-ID-MAP.md) | [Draft map](roadmap/DRAFT-ID-MAP.md) and [finalization map](roadmap/FINALIZATION-ID-MAP.md) apply only to their named checkpoints |
| Preparation, host suitability and migration from scaffold | [Preparation index](roadmap/00-PREPARATION/README.md) and [migration plan](roadmap/00-PREPARATION/MIGRATION-PLAN.md) | Historical [audit](roadmap/00-PREPARATION/REPOSITORY-AUDIT.md)/[hardware assessment](roadmap/00-PREPARATION/HARDWARE-AND-HOSTING-ASSESSMENT.md) require fresh inspection |
| Private TLS, ingress, trust and access | [Access setup](roadmap/02-IDENTITY-AND-HOST/ACCESS-SETUP.md) and ADR 008 | Week 02 gate; public exposure requires confirmed domain/network/security prerequisites |
| Command names, creation order and safe operation targets | [Script interface](roadmap/runbooks/SCRIPT-INTERFACE.md) | [Daily commands](roadmap/DAILY-COMMANDS.md); proposed ops scripts are not installed tools |
| Deploy and verify a release | [Deploy runbook](roadmap/runbooks/DEPLOY.md) | Current target guard, artifact manifest and smoke evidence |
| Roll back an application release | [Rollback runbook](roadmap/runbooks/ROLLBACK.md) | Compatibility check and post-rollback smoke; code rollback is not data recovery |
| Backup, restore and scheduled drills | [Restore runbook](roadmap/runbooks/RESTORE.md), standards Q13 | [Week 10 host runbook](roadmap/10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md) and actual isolated restore report |
| Host reboot and unattended recovery | [Reboot runbook](roadmap/runbooks/HOST-REBOOT.md) | Startup, health, private access and worker recovery evidence |
| Disk exhaustion and safe diagnosis | [Disk-full runbook](roadmap/runbooks/DISK-FULL.md) | Preserve source/volumes/backups; no destructive cleanup by guesswork |
| Internet interruption and offline limitations | [Internet-down runbook](roadmap/runbooks/INTERNET-DOWN.md) | Private/local behavior and reconnect/replay verification |
| Risk, open assumptions and stakeholder decisions | [Risk register](roadmap/RISK-REGISTER.md) and decision register | Use the unresolved-issues routing below; no invented approval IDs |
| Weekly reporting, acceptance and handoff | [Weekly status template](roadmap/WEEKLY-STATUS-TEMPLATE.md) | Dashboard, current week's gates and final release scorecard |

## Evidence, learning and document categories

| Category | Entry point | Authority boundary |
| --- | --- | --- |
| Current checks and sampled dry-run | [V2 validation log](roadmap/V2-VALIDATION-LOG.md) | Dated commands/results and 36 static samples; rerun relevant checks after changes |
| Validation artifacts and helper tools | [Validation index](validation/README.md) | JSON receipts, snapshots, SQL/diagram checks and scripts are evidence/tooling, not product requirements |
| Docs format and portability | [Format guide](roadmap/FORMAT-GUIDE.md) and [self-test](roadmap/FORMAT-SELFTEST.md) | CommonMark/GFM contract; native viewer checks remain separate from lint |
| Docs lint commands and negative proofs | [Tool index](roadmap/tools/README.md) and [linter proofs](roadmap/LINTER-PROOFS.md) | Lint proves documented structural rules, not security, contract correctness or application readiness |
| Terminology and beginner concepts | [Glossary](roadmap/GLOSSARY.md) and [primer index](roadmap/primers/README.md) | Teaching supports accepted standards; comparative JPA examples do not change the JDBC decision |
| Pattern lessons | [Patterns learning path](roadmap/PATTERNS-LEARNING-PATH.md) | Weekly PATTERN-LESSON files are teaching/Stretch subject to admission, not alternative runtime specifications |
| V2 audit, upgrade rationale and before/after metrics | [Roadmap audit](roadmap/ROADMAP-AUDIT.md), [upgrade plan](roadmap/UPGRADE-PLAN.md), [changelog](roadmap/CHANGELOG-V2.md) | Audit/planning snapshots; current weekly estimates supersede earlier coarse demand tables |
| V2 architecture reconciliation | [Architecture review](roadmap/ARCHITECTURE-REVIEW-V2.md) and [reference review](roadmap/V2-REMAINING-REFERENCES.md) | Explicit historical versus final estimates and corrections; no measured velocity guarantee |
| Documentation authoring progress | [PROGRESS-V2](roadmap/PROGRESS-V2.md) | Completed authoring ledger; does not advance the developer dashboard |
| Earlier analysis and authoring | [Project analysis](PROJECT-ANALYSIS.md), [V1 progress](roadmap/PROGRESS.md), [V1 validation](roadmap/ACTUAL-VALIDATION.md) | Historical baseline; old monolith/React/dispatch instructions are superseded |
| Reference archive and approved design parts | [Reference index](reference/README.md) | INCOMING-ARCHITECTURE-V2 parts are approved design under Architecture; other reference families are historical |
| Folder inventory and complete file lookup | [Document catalog](DOCUMENTATION-CATALOG.md) and [roadmap tree](roadmap/FOLDER-TREE.md) | Navigation only; catalog includes every Markdown file and identifies its category |
| Source-level prototype documentation | [Confidence README](../ml/confidence-service/README.md) | Prototype setup only; not production HR policy, integration acceptance or training-data consent |

Evidence records must identify the command, target, time/checkpoint, result and limits. **Run** means the complete step result was obtained, not that one safe preliminary command passed. **Not run** remains the status of unexecuted steps. **PASS**, **CONDITIONAL PASS**, **FAIL** and **NOT RUN** on release scorecards require evidence; documentation completeness cannot turn a product gate green. Never copy secrets or employee data into evidence.

### Unresolved issues and missing specifications

| Issue or topic without approval | Record and resolution owner | Work blocked or limited |
| --- | --- | --- |
| Java/Docker/disk/Flutter readiness and actual tool locations | Dashboard, toolchain reference and Week 01 inspection | Dependent builds, containers and client tests until checked |
| Production host, external encrypted backup/keys and unattended boot | Risk register, host assessment and recovery runbooks | Production readiness and restore acceptance |
| Public domain/network exposure and provider credentials | Access setup, risk register and decision register | Public ingress/provider delivery; private test TLS does not prove public readiness |
| HR rules, consent, data retention and access policy | ADR 004, privacy design and risk register | Policy-dependent behavior and real personal-data use; no certification or legal approval inferred |
| Cloud region, credits, spending and future deployment | ADR 008 and decision register | Cloud execution remains deferred; no authorized budget is inferred |
| New topic with no owner/specification yet | Add an explicit gap to the decision/risk register and this map | Inspect first; write/review the minimum specification before dependent implementation |

There is no separate approved legal/compliance, production SLA, localization or design-system specification identified by this map. Do not manufacture one from primer examples, aspirational performance targets or old open-question IDs. New requirements enter the decision/scope process and capacity math first.

## Keeping the source of truth current

1. Classify each new document as specification, decision, procedure, evidence, navigation, learning or history; add its topic owner here and its file/category to the catalog. Do not create a second canonical specification for the same topic.
1. Edit the owner first. Update affected summaries, Q coverage, checklist, weekly guides, runbooks and ID maps only where the change has an impact. A renamed step needs a mapping; a changed architecture choice needs an ADR.
1. Keep executable OpenAPI/Avro/migrations/lockfiles authoritative for their machine-readable details once created. Link to their owning Markdown guide and print inspected source paths as text; do not maintain competing copied schemas across primers.
1. When behavior changes, record a failing test and eventual result. Preserve dated historical evidence instead of relabeling an old result as current. Keep source implementation, approved intent and execution status visibly distinct.
1. Check catalog coverage against the tracked Markdown inventory, run the linter from the repository root, review the diff and commit only intended files. Inspect untracked new documentation too; add it deliberately before claiming catalog completeness.

Safe read-only checks from the repository root:

```bash
git ls-files '*.md'
python3 docs/roadmap/tools/docs-lint.py docs README.md
git diff --check
```

Expect the first command to list the tracked Markdown inventory and the linter to report zero errors. If lint fails, fix the named file/rule using the format guide; do not weaken the rule. Catalog rows must match actual files, including a new catalog row for the catalog itself. A new check result belongs in its evidence record, not as an unexplained replacement of an earlier historical pass.
