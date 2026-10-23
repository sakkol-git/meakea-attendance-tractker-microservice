# Phase 00 — Preparation and technical assessment

> **Navigation:** [Documentation root](../../README.md) · [Start here](../START-HERE.md) · [Dashboard](../YOU-ARE-HERE.md) · [Next: Week 01](../01-FOUNDATION/README.md)

## Objective and phase overview

Phase 00 establishes the technical, operational, and architectural foundation before active implementation begins. It prevents wasted development hours by auditing the starting repository state, verifying host hardware capabilities, calculating realistic team capacity, and outlining an incremental alignment plan for the accepted seven-service design.

No code modifications or container pulls should begin until the preparation assessments are completed and the prerequisite blockers are acknowledged.

## Preparation deliverables and documents

The preparation phase comprises five specialized engineering documents:

| Document | Primary focus | Key outcomes |
| --- | --- | --- |
| [Repository audit](REPOSITORY-AUDIT.md) | Codebase and configuration inspection | Verified toolchain discrepancies, missing migrations, and prototype inventory. |
| [Project baseline and capacity](PROJECT-BASELINE.md) | Capacity math and velocity assumptions | Historical V1 estimates distinguished from final V2 allocations; dashboard owns capacity inputs. |
| [Hardware and hosting assessment](HARDWARE-AND-HOSTING-ASSESSMENT.md) | Physical machine and network constraints | Mac16,12 host verified, 16 GiB RAM, 5.1 GiB free disk blocker identified. |
| [Migration plan](MIGRATION-PLAN.md) | Incremental final-service alignment | Retain seven service roots; align JDBC, reactive gateway, Nuxt and Kafka without data loss. |
| [Start here](../START-HERE.md) | Daily implementation workflow | 5-minute daily routine, 30–90 minute steps, and stop rules. |

## Pre-flight verification gates

Before launching Week 01 (Walking Skeleton), the following entry gates must be evaluated:

- [ ] Repository inspection complete; build discrepancies recorded.
- [ ] Available disk space reviewed; cleanup planned to reach at least 30 GiB free.
- [ ] Java 21 JDK verified and set as default toolchain.
- [ ] Docker Desktop installed and operational.
- [ ] Weekly capacity model reviewed and agreed by Contributors A and B.
- [ ] Cloudflare Tunnel and Caddy identified as the remote ingress target.
- [ ] Backup destination storage identified and ready for initial dumps in Week 02.

## Transition to implementation

Once hardware headroom and toolchain requirements are addressed, proceed immediately to [Week 01 — Walking skeleton](../01-FOUNDATION/README.md).
