# Reactive gateway boundary

## Ten-minute concept

Spring Cloud Gateway has reactive and MVC variants; the final design chooses the reactive one. Reactor processes asynchronous results without blocking its event-loop threads.

## HRFlow use

Keep JPA persistence/password hashing in business services, not gateway filters. The gateway validates tokens, strips caller identity headers and routes to private services; services still enforce role/resource checks.

## Small proposed example

```yaml
# Proposed route shape; compatibility spike verifies exact version/property binding first.
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: identity
              uri: http://identity-org:8101
              predicates:
                - Path=/api/v1/auth/**
```

## Common mistake

> **Pitfall:** MVC exception advice does not automatically handle reactive filter errors. Test the actual gateway error writer and never block an event-loop thread with a database call.

## Check understanding

- [ ] Explain the boundary in two sentences.
- [ ] Name one failing test before returning to the step.

[Final architecture](../../Architecture.md), [standards](../ENGINEERING-STANDARDS.md), and [dashboard](../YOU-ARE-HERE.md).
