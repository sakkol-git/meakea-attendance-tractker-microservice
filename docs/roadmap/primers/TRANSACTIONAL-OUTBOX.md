# Transactional outbox pattern

> **Note:** The Transactional Outbox pattern ensures recoverable at-least-once publication intent across system boundaries without relying on distributed two-phase commit transactions.

## Purpose and core concept

The Transactional Outbox pattern solves the dual-write problem in distributed systems and modular architectures. It ensures that whenever a database transaction modifies business state, the corresponding domain event intent is persisted atomically within the exact same database transaction.

An asynchronous worker process subsequently reads the pending outbox records and delivers them to Kafka using the approved Avro contracts.

## Problem being solved

Applications frequently need to update an internal database table and notify other components or external systems simultaneously (e.g., commit an attendance check-in and notify the employee's manager).

Attempting dual writes without an outbox creates severe failure modes:

- Database commit succeeds, but network failure prevents message publishing: The event is lost forever; downstream subscribers never learn of the state change.
- Message publishing succeeds, but the database transaction rolls back: Downstream subscribers receive a ghost event describing a state transition that never actually occurred.
- Two-phase commit (XA transactions) introduce high latency, coordinator single-points-of-failure, and lack support across modern cloud services and message brokers.

## Simple mental model

Imagine mailing an official legal document. Instead of driving across the city to hand-deliver the document immediately before signing your contract, you sign the contract and place the stamped letter into your company outbox tray in a single continuous office action.

A designated postal courier regularly empties the outbox tray and delivers the letters. Even if the courier arrives late or encounters traffic, the letter is safely stored and guaranteed to be sent.

## How it works

The pattern separates atomic intent persistence from asynchronous message dispatch:

1. Business transaction: The application begins a standard relational database transaction.
1. State mutation: The application updates domain entities (e.g., inserting into `attendance.attendance_sessions`).
1. Outbox insert: In the exact same transaction, the application inserts an event payload into `attendance.outbox_events` with status `PENDING`.
1. Atomic commit: The database transaction commits; both business state and event record become durable simultaneously.
1. Polling dispatcher: An independent worker queries `outbox_events` where `status = 'PENDING'` using pessimistic row locks.
1. Message delivery: The worker dispatches the event payload to local handler methods or external message topics.
1. Completion mark: After broker acknowledgment, the worker updates the outbox status to `PROCESSED`.

## Why HRFlow needs it

In HRFlow's final service architecture, multiple domains must react to core events without synchronous HTTP coupling or direct cross-schema database writes:

- When an employee checks in, the `attendance` package records raw evidence and inserts an `AttendanceCheckedIn` outbox event.
- The `notification` package consumes this event to update manager dashboard indicators.
- The `reporting` package consumes the event to increment daily attendance rollups.
- If the application crashes during check-in processing, no partial state or orphan notifications are ever created.

## Minimal example

Atomic persistence in SQL:

```sql
BEGIN;

INSERT INTO attendance.attendance_sessions (
    id, tenant_id, employee_id, session_date, check_in_time, status
) VALUES (
    'a1b2c3d4-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '2026-10-23',
    '2026-10-23 08:30:00+00',
    'PRESENT'
);

INSERT INTO attendance.outbox_events (
    id, tenant_id, event_type, aggregate_type, aggregate_id, payload, occurred_at
) VALUES (
    'e5f6a7b8-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'AttendanceCheckedIn',
    'AttendanceSession',
    'a1b2c3d4-0000-0000-0000-000000000001',
    '{"employeeId": "emp-101", "workDate": "2026-10-23", "status": "PRESENT"}'::jsonb,
    CURRENT_TIMESTAMP
);

COMMIT;
```

## Application-specific implementation

Spring Boot transaction boundary with outbox persistence:

```java
@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepo;
    private final OutboxEventRepository outboxRepo;

    public AttendanceService(AttendanceSessionRepository sessionRepo,
                             OutboxEventRepository outboxRepo) {
        this.sessionRepo = sessionRepo;
        this.outboxRepo = outboxRepo;
    }

    @Transactional
    public AttendanceSession recordCheckIn(String tenantId, String employeeId, Instant timestamp) {
        AttendanceSession session = AttendanceSession.create(tenantId, employeeId, timestamp);
        sessionRepo.save(session);

        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .eventType("AttendanceCheckedIn")
                .aggregateType("AttendanceSession")
                .aggregateId(session.getId().toString())
                .payload(session.toEventPayload())
                .occurredAt(timestamp)
                .status(OutboxStatus.PENDING)
                .build();
        outboxRepo.save(event);

        return session;
    }
}
```

## Common mistakes and pitfalls

Avoid these common traps when designing outbox implementations:

> **Pitfall:** Calling external network services or dispatching email directly inside the `@Transactional` method. If the email succeeds but the database transaction rolls back due to a constraint violation, an invalid email has been sent.

- Forgetting consumer deduplication: The outbox pattern provides at-least-once delivery, not exactly-once delivery. If the worker crashes after dispatching but before updating outbox status, the event will be replayed. Consumers must implement an idempotent inbox.
- Polling hotspots: Frequent `SELECT * FROM outbox_events WHERE status = 'PENDING'` queries cause table locks and performance degradation. Use partial indexes (`WHERE status = 'PENDING'`) and `SKIP LOCKED` queries.
- Unbounded outbox table growth: Processed events must be purged or archived on a scheduled cadence to prevent table bloat.

## Testing and debugging

Verification checklist:

- Unit test: Mock repositories and verify both entity and outbox event are saved within the same invocation.
- Integration test: Inject an intentional constraint failure after outbox insertion and verify that the database transaction rolls back completely, leaving zero outbox records.
- Crash simulation test: Dispatch an event, simulate worker crash before completion update, restart worker, and assert that the consumer receives the replayed event without executing duplicate business effects.

## Trade-offs and alternatives

Understanding architectural costs:

- Benefits: Guarantees eventual consistency, zero distributed locks, resilience against network hiccups, and full auditability of outgoing domain events.
- Costs: Introduces latency between database commit and message delivery; requires dedicated polling workers and inbox deduplication tables.
- Alternatives: Change Data Capture (CDC) via Debezium reads the PostgreSQL Write-Ahead Log (WAL) directly. CDC eliminates application outbox inserts but introduces complex external connector infrastructure and schema conversion pipelines.

## When not to use it

Do not use the transactional outbox pattern for:

- Synchronous request-response queries: Reads and cache lookups do not mutate business state and require immediate inline responses.
- High-frequency ephemeral data: Telemetry, IoT temperature readings, and mouse hover analytics do not warrant relational transaction overhead.
- Simple single-table mutations with no downstream subscribers: Internal UI draft saves with zero external effects.

## Related documentation

- [Database locks primer](DATABASE-LOCKS.md)
- [Idempotency primer](IDEMPOTENCY.md)
- [Multi-tenancy primer](MULTI-TENANCY.md)
- [Week 05 outbox build guide](../05-OUTBOX-AND-NOTIFICATIONS/BUILD-GUIDE.md)
- [System architecture](../../Architecture.md)
