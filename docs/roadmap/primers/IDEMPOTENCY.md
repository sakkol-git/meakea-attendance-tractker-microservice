# API idempotency and duplicate prevention

> **Note:** An idempotent operation can be applied multiple times without changing the result beyond the initial application. In HTTP APIs, idempotency protects mutative commands against network retries and duplicate user submissions.

## Purpose and core concept

Idempotency guarantees that executing the same operation repeatedly yields the identical business outcome as executing it once. HTTP methods like `GET`, `PUT`, and `DELETE` are naturally idempotent by specification.

However, `POST` requests create new resources and are non-idempotent by default. To make `POST` commands safe against network drops and automatic client retries, applications implement client-supplied `Idempotency-Key` headers.

## Problem being solved

Network communication is inherently unreliable. When a client submits a check-in or leave request, three distinct failure points exist:

- Request lost in transit: Server never receives the command; state does not change.
- Server crashes during processing: State may be partially mutated or rolled back.
- Response lost in transit: Server successfully processes the command and commits the state, but a network timeout prevents the HTTP 200 OK response from reaching the client.

If the client retries after a timeout, a non-idempotent server creates a duplicate check-in session or charges an employee's leave balance twice.

## Simple mental model

Consider an elevator call button. Pressing the "Floor 5" button once lights up the button and summons the elevator. Pressing the button a second or third time does not summon three separate elevators; the elevator still arrives at Floor 5 exactly once.

Non-idempotent operations are like vending machine coin slots: inserting a second coin dispenses a second snack and charges you twice.

## How it works

The standard idempotency key lifecycle operates as follows:

1. Client generates UUID: Before submitting a mutating `POST` request, the client generates a unique UUID v4 string (e.g., `Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d`).
1. Server receives key: An idempotency interceptor checks the `identity_org.idempotency_keys` table for this `(tenant_id, key)` pair.
1. First execution: If the key is not found, the server inserts a record with status `IN_PROGRESS` and proceeds with the business transaction.
1. Result caching: When the business transaction succeeds, the server caches the HTTP status code and response payload in the idempotency record, updating status to `COMPLETED`.
1. Duplicate replay: If a subsequent request arrives with the same key, the server detects the `COMPLETED` record and returns the cached response immediately without re-executing business logic.
1. Concurrent conflict: If a duplicate arrives while status is still `IN_PROGRESS`, the server returns `409 Conflict`.

## Why HRFlow needs it

In HRFlow, employee mobile and web clients operate over unreliable cellular connections:

- Attendance check-in: If an employee's mobile app experiences a signal drop while submitting a check-in, the app automatically retries. Idempotency guarantees exactly one open attendance session.
- Leave balance deductions: Duplicate approvals by a manager must never debit leave balances twice.
- Material requests: Double-clicking a submission button must not create duplicate equipment purchase orders.

## Minimal example

Idempotency table schema in PostgreSQL:

```sql
CREATE TABLE identity_org.idempotency_keys (
    tenant_id VARCHAR(64) NOT NULL,
    idempotency_key UUID NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_code INT,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_idempotency PRIMARY KEY (tenant_id, idempotency_key)
);

CREATE INDEX idx_idempotency_cleanup
ON identity_org.idempotency_keys (expires_at);
```

## Application-specific implementation

Spring MVC handler interceptor in HRFlow:

```java
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyRepository idempotencyRepo;

    public IdempotencyInterceptor(IdempotencyRepository idempotencyRepo) {
        this.idempotencyRepo = idempotencyRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String keyHeader = request.getHeader("Idempotency-Key");
        if (keyHeader == null || keyHeader.isBlank()) {
            return true;
        }

        String tenantId = TenantContext.getTenantId();
        UUID key = UUID.fromString(keyHeader);

        Optional<IdempotencyRecord> existing = idempotencyRepo.find(tenantId, key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.isCompleted()) {
                response.setStatus(record.getResponseCode());
                response.setContentType("application/json");
                response.getWriter().write(record.getResponseBody());
                return false;
            } else {
                response.sendError(HttpServletResponse.SC_CONFLICT,
                                   "Request currently in progress");
                return false;
            }
        }

        idempotencyRepo.create(tenantId, key, request.getRequestURI());
        return true;
    }
}
```

## Common mistakes and pitfalls

Watch out for these idempotency vulnerabilities:

> **Pitfall:** Ignoring payload differences. If a client reuses an idempotency key with a completely different request body, returning the cached response silently conceals a client bug. Always hash the request body and reject mismatched payloads with `422 Unprocessable Entity` or `400 Bad Request`.

- Setting infinite key expiration: Idempotency keys should expire after a bounded window (e.g., 24 to 48 hours). Retaining keys indefinitely bloats the database.
- Caching server errors: If a request fails with `500 Internal Server Error`, never cache the error as `COMPLETED`. Allow clients to retry transient server failures.
- Non-atomic key reservation: If two threads receive the same key simultaneously, both might see no record and execute concurrently. Use unique database constraints on `(tenant_id, idempotency_key)` to serialize reservation.

## Testing and debugging

Validation test plan:

- Replay verification: Send a check-in request with an `Idempotency-Key`. Capture the session ID. Re-send the exact request with the same key. Verify that the response returns 200 OK with the identical session ID and only one record exists in `attendance.attendance_sessions`.
- Payload alteration test: Send a request with a key, then send a second request with the same key but different employee ID. Verify that the server returns a 400 or 409 error.
- Concurrent submission test: Launch two threads sending identical requests with the same key simultaneously. Verify that one succeeds and one receives 409 Conflict or returns the identical success response.

## Trade-offs and alternatives

Architectural considerations:

- Benefits: Guarantees at-most-once execution of business operations; eliminates duplicate payments, bookings, and leave reservations.
- Costs: Requires client coordinate keys, consumes database storage for response caches, and adds interceptor overhead to mutating requests.
- Alternatives: Domain natural keys (e.g., unique constraint on `employee_id + work_date`). Natural keys are ideal when a natural unique identifier exists, but do not provide cached HTTP response replay.

## When not to use it

Do not require idempotency keys for:

- Safe read operations: `GET`, `HEAD`, and `OPTIONS` do not alter state and are idempotent by nature.
- Complete resource replacement: `PUT` requests that overwrite an entire resource state are inherently idempotent.
- Ephemeral ping endpoints: Health checks and metrics endpoints.

## Related documentation

- [Transactional outbox primer](TRANSACTIONAL-OUTBOX.md)
- [Database locks primer](DATABASE-LOCKS.md)
- [API and database design](../../REST-API-DATABASE-DESIGN.md)
- [Week 04 attendance core build guide](../04-ATTENDANCE-CORE/BUILD-GUIDE.md)
