# HRFlow execution plan

## Current authoritative plan

The finish-line guide replaces the previous service-by-service roadmap. One modular monolith and React/Flutter clients are built through a deployed walking skeleton, then domain workflows. The twelve-week deadline is start plus 84 days; start date is unknown.

- [Start here](roadmap/START-HERE.md).
- [Twelve-week path](roadmap/ROADMAP.md).
- [Capacity baseline](roadmap/00-PREPARATION/PROJECT-BASELINE.md).
- [Milestone evidence](roadmap/MILESTONE-MAP.md).
- [Requirement checklist](roadmap/GLOBAL-CHECKLIST.md).
- [Risk register](roadmap/RISK-REGISTER.md).

## Must versus optional

Must: auth/tenant/resource security, organization, attendance/status/corrections, leave and general requests, in-app notifications, audit, basic CSV, React, Flutter and self-hosted tested recovery. Optional: push, SSE, inference, localization and nonessential refactors. Kafka runtime, ML, Kubernetes, Terraform, cloud databases and tracing stacks are deferred.

Each week has five stones, three build days, a buffer day and gate/deploy day. Core security/correctness/restore failures block advancement. Real-host private deployment begins Week 2; safe public exposure waits for prerequisites. Clean restore repeats Weeks 10 and 12.

## Preserved earlier plan

Earlier scope details, acceptance targets and domain requirements are preserved in [historical references](reference/README.md); old AWS/EKS/React-versus-Nuxt assumptions are superseded by [current decisions](roadmap/DECISION-REGISTER.md). Nothing in the new guide claims the product is built.
