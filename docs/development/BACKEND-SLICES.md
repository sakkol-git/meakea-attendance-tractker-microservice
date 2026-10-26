# Backend vertical slices

The contract contains 158 operations. Eight authentication/profile operations are implemented and 150 remain pending. Counts below describe the original workload by owning workflow, not by URL prefix alone. Each slice must have a concrete contract, persistence and permission tests, both clients wired, and a gateway test before any operation changes to implemented.

| Rank | Module and dependency | Original | Implemented | Remaining |
| --- | --- | ---: | ---: | ---: |
| 1 | Identity and organization: authentication, JWKS, tenant, employee, department, position, site, shift, holiday, audit | 44 | 8 | 36 |
| 2 | Requests and approvals: request lifecycle, delegation, material issue, drafts, workflow records, status | 39 | 0 | 39 |
| 3 | Leave: balances, policy, requests, conflict, attachments, calendar | 29 | 0 | 29 |
| 4 | Attendance: sessions, evidence, corrections, reviews, daily status | 21 | 0 | 21 |
| 5 | Notifications: inbox, preferences, devices, delivery | 13 | 0 | 13 |
| 6 | Reporting: dashboards, exports, rebuilds | 12 | 0 | 12 |
| — | Total | 158 | 8 | 150 |

The implemented identity slice contains `GET /auth/csrf`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`, `PATCH /auth/profile`, `POST /auth/change-password`, and `GET /.well-known/jwks.json`. It persists tenant-scoped users, sign-in attempts and refresh sessions; enforces password verification, throttling, CSRF, JWT validation, refresh rotation/family revocation, self-profile updates and authenticated password changes; and returns canonical Problem JSON. Gateway smoke tests and real web/mobile sign-ins passed for the earlier session subset on 25 October 2026; newer profile/password focused tests are local provider evidence, not a repeated live smoke. Password recovery and general organization CRUD remain later identity slices. No real credentials are checked into the repository.

The web client currently calls a provisional `/auth/setup` route absent from the contract. That route remains pending a product decision on provisioning and must not be counted as implemented authentication.

The next identity decision is password recovery: `/auth/forgot-password` and `/auth/reset-password` remain pending because the current email-only, bearer-authenticated contract does not resolve duplicate email addresses across tenants or define reset-token lifetime and delivery. Resolve that security contract before implementation. Current-tenant operations remain a separate future slice.
