# Backend vertical slices

The contract contains 158 operations. Six authentication operations are implemented and 152 remain pending. Counts below describe the original workload by owning workflow, not by URL prefix alone. Each slice must have a concrete contract, persistence and permission tests, both clients wired, and a gateway test before any operation changes to implemented.

| Rank | Module and dependency | Original | Implemented | Remaining |
| --- | --- | ---: | ---: | ---: |
| 1 | Identity and organization: authentication, JWKS, tenant, employee, department, position, site, shift, holiday, audit | 44 | 6 | 38 |
| 2 | Requests and approvals: request lifecycle, delegation, material issue, drafts, workflow records, status | 39 | 0 | 39 |
| 3 | Leave: balances, policy, requests, conflict, attachments, calendar | 29 | 0 | 29 |
| 4 | Attendance: sessions, evidence, corrections, reviews, daily status | 21 | 0 | 21 |
| 5 | Notifications: inbox, preferences, devices, delivery | 13 | 0 | 13 |
| 6 | Reporting: dashboards, exports, rebuilds | 12 | 0 | 12 |
| — | Total | 158 | 6 | 152 |

The first completed slice is the six-operation password session flow: `GET /auth/csrf`, `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me`, `POST /auth/logout`, and `GET /.well-known/jwks.json`. It persists tenant-scoped users, sign-in attempts, and refresh sessions; enforces password verification, distributed throttling, CSRF, JWT validation, refresh rotation, and family revocation; and returns Problem JSON. Gateway smoke tests and real web and mobile sign-ins passed on 25 October 2026. Registration, password recovery, profile changes, and general organization CRUD remain later identity slices. No real credentials are checked into the repository.

The web client currently calls a provisional `/auth/setup` route absent from the contract. That route remains pending a product decision on provisioning and must not be counted as implemented authentication.

The next proposed backend slice is current-tenant and self-service account context: `GET /tenants/current`, `PATCH /tenants/current`, `PATCH /auth/profile`, and `POST /auth/change-password`. It must define concrete tenant/profile mutation schemas, authorization and version rules before implementation. It has not started.
