# Developer onboarding and daily execution guide

> **Navigation:** [Documentation root](../README.md) · [Source of truth](../SOURCE-OF-TRUTH.md) · [Current dashboard](YOU-ARE-HERE.md) · [Roadmap](ROADMAP.md)

## Ten-minute orientation

Welcome to the HRFlow engineering roadmap. This documentation is an executable, build-along playbook designed to guide developers from an unbuilt scaffolding repository to a fully tested, production-ready modular monolith.

Crucial operational realities to understand before writing code:

- Work is unbuilt: Scaffolding exists in `services/` and `web/`, but core HR business features are planned, not implemented.
- Evidence-based status: Implementation steps are marked **Not run** until you execute the step, run its verification tests, and record authentic test outputs.
- Proposed paths: Commands referencing files that do not exist yet (such as `services/hrflow-app` or `contracts/openapi.yaml`) indicate targets to create during that designated step.

## The five daily files

Every developer must keep these five central files bookmarked during daily implementation:

1. [You are here](YOU-ARE-HERE.md): Your primary active dashboard indicating the current week, next step ID, last verified green milestone tag, and active blockers.
1. [Current build guide](01-FOUNDATION/BUILD-GUIDE.md): The step-by-step manual for the active week, specifying exact prerequisites, implementation tasks, and test commands.
1. [Current gate](01-FOUNDATION/VERIFY-AND-GATE.md): The acceptance test matrix, regression checks, and exit criteria required before closing the week.
1. [Stuck protocol](STUCK-PROTOCOL.md): The time-boxed 45-minute and 90-minute escalation procedures for recovering from unexpected blockers.
1. [Global checklist](GLOBAL-CHECKLIST.md): The complete functional requirements traceability matrix mapping business features to verified test evidence.

## The daily five-minute routine

To ensure continuous, sustainable velocity and avoid fatigue, follow this three-phase daily routine:

- Morning kickoff (2 minutes): Open [You are here](YOU-ARE-HERE.md) to inspect today's assigned step. Check prerequisites in the active build guide. Confirm that you have energy (rested, steady, or tired) and start a 45-minute timer before writing code.
- Focused execution (30 to 90 minutes): Work on one single, bounded implementation step. Introduce no more than two new concepts per day. Run the step's specific check command.
- Evening shutdown (3 minutes): Commit only intended files with a conventional commit message. If completing a stepping stone, tag the commit. Record safe output evidence in the dashboard, update tomorrow's first step, and close your laptop.

## Weekly cadence and operational boundaries

The twelve-week curriculum enforces strict weekly boundaries to guarantee code quality and prevent burnout:

- Monday through Wednesday: Active implementation across Stones 1, 2, and 3 (three 30–90 minute steps per day).
- Thursday: Review and buffer in Stone 4. Zero new feature scope may be introduced. Time is dedicated to test hardening, diagnostic investigations, and refactoring admitted patterns.
- Friday: Stone 5 gate execution, private host deployment, and automated scratch disaster recovery testing.
- Saturday and Sunday: Mandatory rest days. Never pay for missed weekday tasks with weekend work.

## Escalation and stop rules

If an implementation step encounters technical difficulties:

- 45-minute limit: If you make zero observable progress for 45 minutes, stop coding and consult the active week's `TROUBLESHOOTING.md` and [Stuck protocol](STUCK-PROTOCOL.md).
- 90-minute cut rule: If a task remains blocked after 90 minutes, apply the documented stone cut in the phase README. Never skip multi-tenant security or backup verification.
- Critical gate failures: If a Friday gate fails, record **FAIL**. The failed check becomes Monday's first action in the following week, cutting non-essential stretch features.

## Preparation and baseline references

- [Source of truth map](../SOURCE-OF-TRUTH.md)
- [System architecture](../Architecture.md)
- [Twelve-week roadmap](ROADMAP.md)
- [Repository audit](00-PREPARATION/REPOSITORY-AUDIT.md)
- [Project capacity baseline](00-PREPARATION/PROJECT-BASELINE.md)
- [Git safety protocol](GIT-SAFETY.md)
- [Decision register](DECISION-REGISTER.md)

## Senior quality reference

- [Engineering Standards](ENGINEERING-STANDARDS.md).
- [Code Review Checklist](CODE-REVIEW-CHECKLIST.md).
- [Test Strategy](TEST-STRATEGY.md).
- [Security Baseline](SECURITY-BASELINE.md).
- [Api Guidelines](API-GUIDELINES.md).
- [Daily Commands](DAILY-COMMANDS.md).
- [Golden Slice](GOLDEN-SLICE.md).
- [Operational runbooks](runbooks/README.md).

## V2 partial-upgrade checkpoint

Open [V2 progress](PROGRESS-V2.md) before executing this roadmap. The final seven-service architecture supersedes old C/React instructions; cloud execution is deferred. Weeks 1–2 are atomic drafts with known validation gaps, Weeks 3–12 remain unconverted, and the requested linter extensions are not implemented yet. [Interim changelog](CHANGELOG-V2.md), [ID map](STEP-ID-MAP.md), [validation log](V2-VALIDATION-LOG.md), and [remaining architecture references](V2-REMAINING-REFERENCES.md) make the unfinished work explicit. No application work is marked complete.
