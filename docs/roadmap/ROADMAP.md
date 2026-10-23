# Twelve-week roadmap

## Capacity and clock

The user replied 24; this guide interprets that as 24 hours per contributor per week. Only 16.8 hours each is planned; Weeks 4 and 8 use 14.28 each. Start is Monday 2026-10-26; Week-12 release rehearsal is Friday 2027-01-15. Deadline is exactly start plus 84 days; never schedule weekend rescue work. Actual hours and velocity must be reviewed after Week 1.

## Weekly path

| Week | Theme | Stones | Minimum gate | Deployable state |
| --- | --- | --- | --- | --- |
| 01 | [Walking skeleton](01-FOUNDATION/README.md) | 8 | Nuxt and contract-driven Flutter plus critical tests/restore | Private local skeleton |
| 02 | [Identity and private host deployment](02-IDENTITY-AND-HOST/README.md) | 8 | The skeleton reaches the real host plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 03 | [Organization and admin](03-ORGANIZATION-AND-ADMIN/README.md) | 8 | Builder only after fixture pain plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 04 | [Attendance correctness](04-ATTENDANCE-CORE/README.md) | 8 | Corrections without lost history plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 05 | [Durable Kafka events and notifications](05-OUTBOX-AND-NOTIFICATIONS/README.md) | 8 | Users see durable notifications plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 06 | [Leave and concurrent balances](06-LEAVE-WORKFLOW/README.md) | 8 | Cancellation and attendance conflict plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 07 | [Materials special requests and attachments](07-REQUESTS-AND-APPROVALS/README.md) | 8 | Attachment adapter only after pain plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 08 | [Integration and accessibility](08-WEB-AND-MOBILE-INTEGRATION/README.md) | 8 | Usability without new scope plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 09 | [Reports audit and health](09-REPORTING-AUDIT-OBSERVABILITY/README.md) | 8 | Filter specification only after repetition plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 10 | [Production operations and clean-host restore](10-OPERATIONS-AND-RESTORE/README.md) | 8 | Failure modes are observable plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 11 | [Security recovery and measured load](11-HARDENING-AND-PILOT/README.md) | 8 | Pilot only behind release controls plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |
| 12 | [Freeze acceptance and demonstration](12-RELEASE-AND-DEMONSTRATION/README.md) | 8 | Tell the truth in the demonstration plus critical tests/restore | Confirmed host redeployed, safety gate controls public access |

## Critical path

```mermaid
flowchart LR
    A["W1 skeleton and mock"] --> B["W2 security and private host"]
    B --> C["W3 organization"]
    C --> D["W4 attendance"]
    D --> E["W5 reliable notifications"]
    E --> F["W6 leave"]
    F --> G["W7 requests"]
    G --> H["W8 client integration"]
    H --> I["W9 reports audit health"]
    I --> J["W10 clean restore operations"]
    J --> K["W11 hardening"]
    K --> L["W12 freeze and demo"]
```

Text version:

- Contracts/mock begin Week 1; Flutter never waits until Week 8 to start.
- Security and real-host blockers surface Week 2; public exposure waits for HTTPS/auth/firewall/secrets/restore.
- Attendance precedes outbox recovery, leave, requests and integrated clients.
- Reports and operational recovery precede hardening and final release rehearsal.

## Rolling schedule and cuts

Choose a Monday kickoff; finish release by the last Friday before the exact 84-day deadline. Each Thursday is buffer/review only; each Friday redeploys and smoke-tests the confirmed host. Failed critical work replaces next week's first stone before new scope. Never pay delay with weekends, auth bypass or omitted restore tests.

Cut UI polish, optional push/SSE/inference/localization, extra reports, and unadmitted patterns first. Keep auth/tenant, attendance, leave/approval, Kafka event reliability, deployment and restore. Requests/Nuxt/Flutter/basic reports remain Must; if actual capacity cannot fit them, declare deadline/scope conflict rather than quietly drop them.

[Capacity math](00-PREPARATION/PROJECT-BASELINE.md), [milestones](MILESTONE-MAP.md), and [checklist](GLOBAL-CHECKLIST.md).

## Final atomic capacity ledger

Step durations include first reading/tests/commit; these totals also include explicit A review/mentoring and opening-week B review. Derived from the actual BUILD step durations and owner assignments, not the older component-overhead estimate.

| Week | A planned h | B planned h | Limit each h | Steps | Gate |
| --- | --- | --- | --- | --- | --- |
| 01 | 14 | 14.25 | 16.8 | 38 | **NOT RUN** |
| 02 | 14.25 | 11 | 16.8 | 35 | **NOT RUN** |
| 03 | 14.25 | 14.25 | 16.8 | 41 | **NOT RUN** |
| 04 | 9.5 | 12.25 | 14.28 | 36 | **NOT RUN** |
| 05 | 10 | 14.25 | 16.8 | 36 | **NOT RUN** |
| 06 | 10.25 | 14 | 16.8 | 36 | **NOT RUN** |
| 07 | 13.5 | 14.25 | 16.8 | 42 | **NOT RUN** |
| 08 | 7.75 | 12.25 | 14.28 | 33 | **NOT RUN** |
| 09 | 13.25 | 14 | 16.8 | 41 | **NOT RUN** |
| 10 | 13.5 | 14 | 16.8 | 42 | **NOT RUN** |
| 11 | 9.25 | 14 | 16.8 | 34 | **NOT RUN** |
| 12 | 9.25 | 14 | 16.8 | 34 | **NOT RUN** |

Total planned A=138.75, B=162.5.5, combined 301.25 contributor-hours against 393.12 planned-capacity hours. Unallocated capacity is recovery headroom, not a hidden scope increase; the separate 30 percent reserve is still unallocated. Weeks 4/8 are lighter and each has the reduced 14.28-hour cap. These are conditional estimates, not measured velocity or a guaranteed delivery date. Review actual times every Friday.
