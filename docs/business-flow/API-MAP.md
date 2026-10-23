# API to business workflow map

## Actual application and generated routes

[IMPL] The seven Python route objects below were inspected in-process without starting a server. Three have source handlers and four are framework documentation routes. All Java business controller mappings and gateway routes are absent. (ml/confidence-service/confidence/api.py#app; docs/business-flow/_inventory.md)

| Inventory ID | Method and route | Handler | Auth and validation | Workflow |
| --- | --- | --- | --- | --- |
| E01 | GET /health | ml/confidence-service/confidence/api.py#health | [IMPL] No auth/body; UP and modelVersion | BF-033 |
| E02 | GET /model | ml/confidence-service/confidence/api.py#info | [IMPL] No auth/body; full loaded metadata | BF-033 |
| E03 | POST /score | ml/confidence-service/confidence/api.py#score | [IMPL] Evidence validation, no trusted tenant binding | BF-032 |
| E04 | GET,HEAD /openapi.json | ml/confidence-service/confidence/api.py#app / FastAPI openapi | [IMPL] Generated technical schema route, no auth | BF-039 |
| E05 | GET,HEAD /docs | ml/confidence-service/confidence/api.py#app / FastAPI swagger_ui_html | [IMPL] Generated technical docs, no auth | BF-039 |
| E06 | GET,HEAD /docs/oauth2-redirect | ml/confidence-service/confidence/api.py#app / FastAPI swagger_ui_redirect | [IMPL] Generated technical docs helper | BF-039 |
| E07 | GET,HEAD /redoc | ml/confidence-service/confidence/api.py#app / FastAPI redoc_html | [IMPL] Generated technical docs, no auth | BF-039 |

[IMPL] Handler abbreviations above mean ml/confidence-service/confidence/api.py. Java configured health/liveness/readiness are technical management surfaces for BF-039, not business controllers. Test ProbeController mappings are not production routes. Auth checks in tests show generic denial, not JWT verification. (services/attendance/src/main/java/com/hrflow/attendance/config/SecurityConfiguration.java#SecurityConfiguration.securityFilterChain; services/attendance/src/test/java/com/hrflow/attendance/config/SecurityConfigurationTests.java#SecurityConfigurationTests.ProbeController.probe)

## Configured Java technical health surfaces

[IMPL] These surfaces are derived from each service YAML/probe configuration, with no business controller. Handler owner is Boot health/probe infrastructure; auth is the service health allowlist and request body is N/A. Runtime endpoint registration was not exercised here. (docs/business-flow/_inventory.md — Configured Java technical endpoint matrix; services/*/src/main/resources/application.yaml)

| Inventory ID | Service / private port | Configured method and route | Workflow / classification |
| --- | --- | --- | --- |
| J01H | api-gateway / 9080 | GET `/actuator/health` | [IMPL] BF-039 technical; config only |
| J01L | api-gateway / 9080 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical; config only |
| J01R | api-gateway / 9080 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical; config only |
| J02H | identity-org / 9101 | GET `/actuator/health` | [IMPL] BF-039 technical; config only |
| J02L | identity-org / 9101 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical; config only |
| J02R | identity-org / 9101 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical; config only |
| J03H | attendance / 9102 | GET `/actuator/health` | [IMPL] BF-039 technical; config only |
| J03L | attendance / 9102 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical; config only |
| J03R | attendance / 9102 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical; config only |
| J04H | leave / 9103 | GET `/actuator/health` | [IMPL] BF-039 technical; config only |
| J04L | leave / 9103 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical; config only |
| J04R | leave / 9103 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical; config only |
| J05H | request / 9104 | GET `/actuator/health` | [IMPL] BF-039 technical; config only |
| J05L | request / 9104 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical; config only |
| J05R | request / 9104 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical; config only |
| J06H | notification / 9105 | GET `/actuator/health` | [IMPL] BF-039 technical; config only |
| J06L | notification / 9105 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical; config only |
| J06R | notification / 9105 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical; config only |
| J07H | reporting / 9106 | GET `/actuator/health` | [IMPL] BF-039 technical; config only |
| J07L | reporting / 9106 | GET `/actuator/health/liveness` | [IMPL] BF-039 technical; config only |
| J07R | reporting / 9106 | GET `/actuator/health/readiness` | [IMPL] BF-039 technical; config only |

## Historical candidate route crosswalk

[DESIGN] These 112 method/path pairs are copied from historical CONTRACTS-REFERENCE headers and remain unimplemented candidate specifications. They are not endpoints found in production code, not a generated OpenAPI contract, and not newly approved scope. Supporting administrative/query mappings route to the nearest workflow context; exact permissions, required fields, transitions and adoption must be re-confirmed from the cited API block before coding. Current API conventions supersede conflicting old error/version examples. (docs/REST-API-DATABASE-DESIGN.md — Historical references; docs/reference/README.md)

| Reference | Method and historical path | Workflow context | Evidence / status |
| --- | --- | --- | --- |
| API-001 | POST `/api/v1/auth/login` | BF-001 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-001; not live |
| API-002 | POST `/api/v1/auth/refresh` | BF-002 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-002; not live |
| API-003 | POST `/api/v1/auth/logout` | BF-040 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-003; not live |
| API-004 | GET `/api/v1/auth/me` | BF-001, BF-002 (supporting account/key design; details remain historical) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-004; not live |
| API-005 | POST `/api/v1/auth/change-password` | BF-001, BF-002 (supporting account/key design; details remain historical) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-005; not live |
| API-006 | GET `/.well-known/jwks.json` | BF-001, BF-002 (supporting account/key design; details remain historical) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-006; not live |
| API-007 | POST `/api/v1/tenants` | BF-003 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-007; not live |
| API-008 | GET `/api/v1/tenants/current` | BF-003 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-008; not live |
| API-009 | PATCH `/api/v1/tenants/current` | BF-003 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-009; not live |
| API-010 | GET `/api/v1/employees` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-010; not live |
| API-011 | POST `/api/v1/employees` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-011; not live |
| API-012 | GET `/api/v1/employees/{id}` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-012; not live |
| API-013 | PATCH `/api/v1/employees/{id}` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-013; not live |
| API-014 | POST `/api/v1/employees/{id}/terminate` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-014; not live |
| API-015 | PUT `/api/v1/employees/{id}/device` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-015; not live |
| API-016 | GET `/api/v1/employees/{id}/direct-reports` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-016; not live |
| API-017 | GET `/api/v1/departments` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-017; not live |
| API-018 | POST `/api/v1/departments` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-018; not live |
| API-019 | PATCH `/api/v1/departments/{id}` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-019; not live |
| API-020 | DELETE `/api/v1/departments/{id}` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-020; not live |
| API-021 | GET `/api/v1/positions` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-021; not live |
| API-022 | POST `/api/v1/positions` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-022; not live |
| API-023 | GET `/api/v1/work-sites` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-023; not live |
| API-024 | POST `/api/v1/work-sites` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-024; not live |
| API-025 | PATCH `/api/v1/work-sites/{id}` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-025; not live |
| API-026 | DELETE `/api/v1/work-sites/{id}` | BF-004 (supporting org/device/site administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-026; not live |
| API-027 | GET `/api/v1/shifts` | BF-005 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-027; not live |
| API-028 | POST `/api/v1/shifts` | BF-005 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-028; not live |
| API-029 | PATCH `/api/v1/shifts/{id}` | BF-005 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-029; not live |
| API-030 | POST `/api/v1/employees/{id}/shifts` | BF-005 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-05.md — API-030; not live |
| API-031 | GET `/api/v1/holidays` | BF-006 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-031; not live |
| API-032 | POST `/api/v1/holidays` | BF-006 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-032; not live |
| API-033 | DELETE `/api/v1/holidays/{id}` | BF-006 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-033; not live |
| API-034 | GET `/api/v1/audit-logs` | BF-004, BF-029 (audit query design only) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-034; not live |
| API-035 | POST `/api/v1/attendance/check-in` | BF-007 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-035; not live |
| API-036 | POST `/api/v1/attendance/check-out` | BF-008 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-036; not live |
| API-037 | POST `/api/v1/attendance/evidence` | BF-009 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-037; not live |
| API-038 | GET `/api/v1/attendance/today` | BF-013 (derived attendance reads) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-038; not live |
| API-039 | GET `/api/v1/attendance/me` | BF-013 (derived attendance reads) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-039; not live |
| API-040 | GET `/api/v1/attendance/daily-status/me` | BF-013 (derived attendance reads) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-040; not live |
| API-041 | POST `/api/v1/attendance/sessions/{id}/confirm` | BF-011 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-041; not live |
| API-042 | POST `/api/v1/attendance/sessions/{id}/reject` | BF-011 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-042; not live |
| API-043 | POST `/api/v1/attendance/corrections` | BF-012 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-043; not live |
| API-044 | GET `/api/v1/attendance/corrections` | BF-012 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-044; not live |
| API-045 | POST `/api/v1/attendance/corrections/{id}/approve` | BF-012 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-045; not live |
| API-046 | POST `/api/v1/attendance/corrections/{id}/reject` | BF-012 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-046; not live |
| API-047 | GET `/api/v1/attendance/team` | BF-013 (derived attendance reads) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-047; not live |
| API-048 | GET `/api/v1/attendance/daily-status` | BF-013 (derived attendance reads) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-048; not live |
| API-049 | POST `/api/v1/attendance/manual` | BF-012 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-049; not live |
| API-050 | GET `/api/v1/attendance/sessions/{id}/evidence` | BF-009, BF-012 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-050; not live |
| API-051 | GET `/api/v1/leave/types` | BF-018 (supporting policy/type administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-051; not live |
| API-052 | GET `/api/v1/leave/balances/me` | BF-018 (supporting policy/type administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-052; not live |
| API-053 | POST `/api/v1/leave/requests` | BF-014 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-053; not live |
| API-054 | GET `/api/v1/leave/requests/me` | BF-014 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-054; not live |
| API-055 | GET `/api/v1/leave/requests/{id}` | BF-014 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-055; not live |
| API-056 | POST `/api/v1/leave/requests/{id}/cancel` | BF-017 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-056; not live |
| API-057 | GET `/api/v1/leave/requests/pending` | BF-015 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-057; not live |
| API-058 | POST `/api/v1/leave/requests/{id}/approve` | BF-015 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-058; not live |
| API-059 | POST `/api/v1/leave/requests/{id}/reject` | BF-016 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-059; not live |
| API-060 | GET `/api/v1/leave/calendar` | BF-015, BF-013 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-06.md — API-060; not live |
| API-061 | GET `/api/v1/leave/admin/types` | BF-018 (supporting policy/type administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-061; not live |
| API-062 | POST `/api/v1/leave/admin/types` | BF-018 (supporting policy/type administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-062; not live |
| API-063 | PATCH `/api/v1/leave/admin/types/{id}` | BF-018 (supporting policy/type administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-063; not live |
| API-064 | PUT `/api/v1/leave/admin/policies/{typeId}` | BF-018 (supporting policy/type administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-064; not live |
| API-065 | POST `/api/v1/leave/admin/balances/{employeeId}/adjust` | BF-018 (supporting policy/type administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-065; not live |
| API-066 | POST `/api/v1/leave/admin/year-end` | BF-019 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-066; not live |
| API-067 | GET `/api/v1/leave/admin/year-end/{id}` | BF-019 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-067; not live |
| API-068 | POST `/api/v1/leave/requests/{id}/resolve-conflict` | BF-020 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-068; not live |
| API-069 | POST `/api/v1/leave/attachments/presign` | BF-026 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-069; not live |
| API-070 | POST `/api/v1/leave/attachments/{id}/complete` | BF-026 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-070; not live |
| API-071 | GET `/api/v1/leave/attachments/{id}/download` | BF-026 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-071; not live |
| API-072 | GET `/api/v1/requests/types` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-072; not live |
| API-073 | GET `/api/v1/materials` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-073; not live |
| API-074 | POST `/api/v1/requests` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-074; not live |
| API-075 | GET `/api/v1/requests/me` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-075; not live |
| API-076 | GET `/api/v1/requests/{id}` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-076; not live |
| API-077 | PATCH `/api/v1/requests/{id}` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-077; not live |
| API-078 | POST `/api/v1/requests/{id}/submit` | BF-022 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-078; not live |
| API-079 | POST `/api/v1/requests/{id}/cancel` | BF-024 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-079; not live |
| API-080 | POST `/api/v1/requests/{id}/comments` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-080; not live |
| API-081 | POST `/api/v1/requests/{id}/attachments/presign` | BF-026 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-081; not live |
| API-082 | POST `/api/v1/requests/{id}/attachments/{attachmentId}/complete` | BF-026 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-082; not live |
| API-083 | GET `/api/v1/requests/{id}/attachments/{attachmentId}/download` | BF-026 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-083; not live |
| API-084 | GET `/api/v1/requests/inbox` | BF-023 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-084; not live |
| API-085 | POST `/api/v1/requests/{id}/approve` | BF-023 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-085; not live |
| API-086 | POST `/api/v1/requests/{id}/reject` | BF-024 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-086; not live |
| API-087 | POST `/api/v1/requests/{id}/fulfill` | BF-025 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-087; not live |
| API-088 | GET `/api/v1/materials/admin` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-088; not live |
| API-089 | POST `/api/v1/materials/admin` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-089; not live |
| API-090 | PATCH `/api/v1/materials/admin/{id}` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-07.md — API-090; not live |
| API-091 | GET `/api/v1/requests/admin/types` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-091; not live |
| API-092 | POST `/api/v1/requests/admin/types` | BF-021 (supporting type/material/comment administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-092; not live |
| API-093 | POST `/api/v1/notifications/devices` | BF-027 (supporting device/template/channel administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-093; not live |
| API-094 | DELETE `/api/v1/notifications/devices/{id}` | BF-027 (supporting device/template/channel administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-094; not live |
| API-095 | GET `/api/v1/notifications` | BF-028 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-095; not live |
| API-096 | GET `/api/v1/notifications/unread-count` | BF-028 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-096; not live |
| API-097 | POST `/api/v1/notifications/{id}/read` | BF-028 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-097; not live |
| API-098 | POST `/api/v1/notifications/read-all` | BF-028 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-098; not live |
| API-099 | GET `/api/v1/notifications/preferences` | BF-028 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-099; not live |
| API-100 | PUT `/api/v1/notifications/preferences` | BF-028 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-100; not live |
| API-101 | GET `/api/v1/notifications/stream` | BF-027 (supporting device/template/channel administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-101; not live |
| API-102 | GET `/api/v1/notifications/admin/templates/{code}` | BF-027 (supporting device/template/channel administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-102; not live |
| API-103 | PUT `/api/v1/notifications/admin/templates/{code}` | BF-027 (supporting device/template/channel administration) | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-103; not live |
| API-104 | GET `/api/v1/reports/dashboard` | BF-029 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-104; not live |
| API-105 | GET `/api/v1/reports/attendance/summary` | BF-029 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-105; not live |
| API-106 | GET `/api/v1/reports/attendance/employees/{id}` | BF-029 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-106; not live |
| API-107 | GET `/api/v1/reports/attendance/late-ranking` | BF-029 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-107; not live |
| API-108 | GET `/api/v1/reports/leave/summary` | BF-029 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-108; not live |
| API-109 | GET `/api/v1/reports/leave/balances` | BF-029 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-109; not live |
| API-110 | GET `/api/v1/reports/requests/summary` | BF-029 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-110; not live |
| API-111 | POST `/api/v1/reports/exports` | BF-030 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-111; not live |
| API-112 | GET `/api/v1/reports/exports/{id}` | BF-030 | [DESIGN] docs/reference/CONTRACTS-REFERENCE-08.md — API-112; not live |
