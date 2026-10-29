# Architecture decision records

## Active authority

[ADR 008](008-FINAL-SERVICE-AND-CLOUD-DESIGN.md) records the user's final service architecture and latest cloud deferral. [ADR 009](009-JPA-OVER-JDBC.md) selects JPA, [ADR 010](010-DUAL-JWT-VALIDATION.md) replaces the gateway-header trust clause with independent JWT validation at the gateway and every Java service, and [ADR 011](011-SPRING-RESOURCE-SERVER-JWT.md) selects Spring Security OAuth2 Resource Server instead of custom JWT filters. [ADR 013](013-DOMAIN-PATTERN-PLAN.md) owns conditional domain pattern selection, and [ADR 014](014-MAPSTRUCT-MAPPING-POLICY.md) selects MapStruct for straightforward Java mapping while retaining explicit code for complex mapping. The final architecture's inline numbered decision table is a topic summary; formal ADR file numbers in this folder are distinct.

| ADR | Record | Current status | Review verdict |
| --- | --- | --- | --- |
| 001 | [Service boundaries](001-SERVICE-AND-EVENT-BOUNDARIES.md) | Historical, restored/refined by 008 | **Adjust** |
| 002 | [Tenant isolation](002-TENANT-AND-PERSISTENCE-ISOLATION.md) | Proposed, quality refinement 007/008 | **Adjust** |
| 003 | [Event delivery](003-EVENT-DELIVERY-AND-CONTRACTS.md) | Proposed, final broker direction 008 | **Adjust** |
| 004 | [Conflict policy](004-ATTENDANCE-AND-LEAVE-CONFLICTS.md) | Proposed HR policy | **Keep** pending policy sign-off |
| 005 | [Deployment/client baseline](005-DEPLOYMENT-AND-CLIENT-BASELINE.md) | Historical, refined by 008 | **Adjust** |
| 006 | [Modular monolith](006-FINISH-LINE-MODULAR-MONOLITH.md) | Superseded by 008 | **Replace** |
| 007 | [Tenant/quality defense](007-QUALITY-AND-TENANT-DEFENSE.md) | Retained except local-dispatch/topology clauses | **Adjust** |
| 008 | [Final services/cloud deferral](008-FINAL-SERVICE-AND-CLOUD-DESIGN.md) | Accepted design, implementation not verified | **Keep** |
| 009 | [JPA over JDBC](009-JPA-OVER-JDBC.md) | Accepted persistence implementation choice | **Keep** |
| 010 | [Dual JWT validation](010-DUAL-JWT-VALIDATION.md) | Accepted boundary; custom-filter mechanism superseded by ADR 011 | **Adjust** |
| 011 | [Spring resource-server JWT](011-SPRING-RESOURCE-SERVER-JWT.md) | Accepted authentication implementation mechanism | **Keep** |
| 012 | [Organization event v1](012-ORGANIZATION-EVENT-V1.md) | Accepted provisional typed snapshot wire for three organization topics | **Keep** |
| 013 | [Domain pattern plan](013-DOMAIN-PATTERN-PLAN.md) | Accepted design guidance; conditional Strategy candidates, no automatic refactor | **Keep** |
| 014 | [MapStruct mapping policy](014-MAPSTRUCT-MAPPING-POLICY.md) | Accepted Java mapping mechanism and custom-mapper boundary | **Keep** |

## Reading rules

Keep historical records intact; this index and a superseding ADR state changed authority. Do not edit old history to imply a decision never existed. Current guides must match [Architecture](../Architecture.md) and [contracts](../REST-API-DATABASE-DESIGN.md). [Review V2](../roadmap/ARCHITECTURE-REVIEW-V2.md) contains cost, beginner adoption, capacity and impact evidence.
