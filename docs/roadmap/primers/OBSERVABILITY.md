# Health checks, metrics, and structured logging

> **Note:** Observability provides continuous insight into system health, performance bottlenecks, and runtime errors through structured logs, operational metrics, and standardized health check probes.

## Purpose and core concept

Observability enables operators to infer the internal health state of a software system based on its external telemetry outputs.

Modern production observability relies on three complementary pillars:

- Health check probes: Liveness and readiness endpoints indicating whether the application process is running and able to accept traffic.
- Operational metrics: Numeric counters, gauges, and timers tracking request rates, error percentages, JVM memory consumption, and database pool utilization.
- Structured logs: Machine-readable JSON log events enriched with context (tenant ID, user ID, request ID) without exposing sensitive credentials or employee data.

## Problem being solved

Operating production software without structured observability creates severe blind spots:

- Silent failures: Background workers crash quietly while the web server stays up, causing unhandled outbox queues to accumulate for days unnoticed.
- Needle in a haystack debugging: Unstructured plaintext console logs make it nearly impossible to filter errors belonging to a specific tenant or user session.
- Secret leakage: Naively logging HTTP request and response bodies accidentally exposes plaintext passwords, JWT tokens, and medical certificate records to log files.

## Simple mental model

Observability is like the instrument dashboard and warning lights in an automobile.

The check-engine light is a health probe; the speedometer and fuel gauge are metrics; and the detailed maintenance service diagnostic log retrieved by a mechanic is structured logging.

## How it works

The observability architecture in HRFlow combines Spring Boot Actuator with lightweight monitoring:

1. Health inspection: Spring Boot Actuator periodically verifies database connectivity, disk space, and outbox worker liveness at `/actuator/health`.
1. Metric collection: Micrometer records HTTP request duration histograms, active database connections, and outbox queue size.
1. Private scraping: Uptime Kuma polls the private health endpoint every 60 seconds from within the internal Docker network.
1. Context-enriched logging: Mapped Diagnostic Context (MDC) automatically binds `tenantId`, `userId`, and `requestId` to every log event.
1. Sanitized logging: Logback encoders format logs as single-line JSON objects, masking sensitive authorization headers and password fields.

## Why HRFlow needs it

In HRFlow's self-hosted service environment:

- Low-overhead monitoring: Uptime Kuma and Actuator provide immediate alerting without requiring heavy, resource-intensive monitoring stacks (like Prometheus, Grafana, and Elasticsearch).
- Multi-tenant diagnostic filtering: When an HR administrator reports a failure, operators can filter logs by `tenant_id` to isolate relevant errors without viewing other tenants' logs.
- Audit trail integrity: Tamper-evident operational logging satisfies legal compliance for employee record modifications and leave approvals.

## Minimal example

Spring Boot Actuator configuration in `application.yaml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
  metrics:
    tags:
      application: attendance
```

Actuator health response:

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "free": 5368709120,
        "threshold": 10485760
      }
    }
  }
}
```

## Application-specific implementation

Mapped Diagnostic Context (MDC) filter in Java:

```java
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        response.setHeader("X-Request-ID", requestId);

        try {
            MDC.put("requestId", requestId);
            MDC.put("uri", request.getRequestURI());
            MDC.put("method", request.getMethod());

            String tenantId = extractTenantIfPresent(request);
            if (tenantId != null) {
                MDC.put("tenantId", tenantId);
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String extractTenantIfPresent(HttpServletRequest request) {
        // Safe extraction from verified token without throwing if unauthenticated
        return null;
    }
}
```

## Common mistakes and pitfalls

Avoid these dangerous logging mistakes:

> **Pitfall:** Logging full HTTP request headers or raw request bodies. Headers contain `Authorization: Bearer <jwt>` and request bodies contain plaintext passwords during login. Always sanitize or explicitly whitelist logged fields.

- Unbounded log file growth: Writing gigabytes of container logs without Docker log rotation fills the host hard disk and crashes the server. Always configure Docker logging limits (`max-size: 50m`, `max-file: 3`).
- Blocking health checks: A health check endpoint that executes complex, slow queries (such as counting millions of rows) causes health probes to time out during heavy load, triggering false restarts. Keep health checks lightweight (e.g., `SELECT 1`).
- Exposing actuator endpoints to the public internet: Never route sensitive actuator endpoints (`/actuator/env`, `/actuator/heapdump`, `/actuator/beans`) through public reverse proxies.

## Testing and debugging

Verification checks:

- Health probe test: Run `curl -s http://localhost:8080/actuator/health` and verify that the response returns `{"status":"UP"}` with zero authentication required on localhost.
- Metric inspection test: Run `curl -s http://localhost:8080/actuator/metrics/http.server.requests` to inspect request latency distributions.
- Log sanitization test: Execute a login request with an intentionally incorrect password and inspect the log output to confirm that the password is masked or omitted.

## Trade-offs and alternatives

Observability architectures:

- Actuator + Uptime Kuma (HRFlow choice): Sub-100MB memory footprint, zero complex maintenance, immediate uptime alerts; lacks distributed microservice trace visualization.
- Full LGTM stack (Loki, Grafana, Tempo, Mimir): Industrial-scale metrics and distributed tracing; requires 8+ GiB of RAM, completely exceeding self-host budgets.
- Cloud SaaS (Datadog, New Relic): Powerful insights; high per-host recurring costs and violates data sovereignty requirements.

## When not to use it

Do not use high-cardinality metric tags for:

- Individual user or employee IDs: Adding unique user IDs as Prometheus metric tags creates high-cardinality explosions that exhaust server memory. Use structured logs for user-level debugging.
- Full payload debug logging in production: High-frequency debug logging degrades disk I/O performance; retain `INFO` and `WARN` levels in production.

## Related documentation

- [Reverse proxy and HTTPS primer](REVERSE-PROXY-HTTPS.md)
- [Docker Compose primer](DOCKER-COMPOSE.md)
- [Week 09 reporting and audit build guide](../09-REPORTING-AUDIT-OBSERVABILITY/BUILD-GUIDE.md)
- [Operations host runbook](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md)
