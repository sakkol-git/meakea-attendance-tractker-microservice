# Apache Kafka fundamentals and delivery boundaries

> **Decision:** ADR 008 retains Kafka KRaft, Avro and Schema Registry in the final service architecture. AWS/Kubernetes deployment is deferred; Kafka runtime is not deferred.

## Purpose and core concept

Apache Kafka is a distributed, append-only event streaming platform designed for high-throughput, fault-tolerant message streaming and storage.

Kafka models data streams as immutable, partitioned, replicated logs called topics. Producers append events to topic partitions, while consumer groups read independently at their own recorded offsets.

## Problem being solved by distributed event brokers

In large-scale microservice architectures, distributed message brokers solve critical decoupling challenges:

- Asynchronous message buffering: Brokers absorb massive traffic spikes (millions of events per second) without overwhelming downstream processing services.
- Independent consumer velocity: Multiple independent consumer groups read from the same topic at their own pace without impacting producer throughput.
- Event replayability: Because events are retained on disk for days or months, new downstream services can replay historical events from the beginning of the log.

## Simple mental model

Imagine an automated printing press producing an endless paper tape of stock trades. The press stamps each trade at the end of the tape and marks it with an incrementing number (the offset).

Different departments (accounting, compliance, analytics) each have their own reader machine. Accounting might read near the very end of the tape, while compliance pauses to investigate an anomaly and catches up later. The printing press never stops or slows down for any reader.

## How it works

Key Kafka architectural components:

1. Topics and partitions: A topic is divided into one or more partitions distributed across broker nodes. Partitions are the unit of parallelism.
1. Partition keys: Producers attach a message key (e.g., `tenant_id + employee_id`). Kafka hashes the key to assign the event deterministically to a specific partition, preserving partition append order; application retries and competing outbox publishers still require aggregate-revision handling.
1. Consumer groups: Multiple instances of a service join a consumer group. Kafka divides partitions among active consumers so each partition is processed by exactly one consumer per group.
1. Offset tracking: Consumers periodically commit their latest processed offset back to Kafka's internal `__consumer_offsets` topic.
1. Compaction: For stateful topics, log compaction retains only the latest message payload for each message key, discarding older overwritten versions.

## Current cost and failure boundary

The previous ADR 006 deferral is superseded. One local broker is possible for development but has no broker failover or host high availability. Hardware sizing must be measured; neither a fixed RAM number nor an assumed cluster size proves this workload fits. Low free disk and an unavailable Docker daemon remain blockers.

A PostgreSQL outbox is retained with Kafka: business state/audit/intent commit together, publisher acknowledgment precedes marking sent, and consumers commit effect/inbox before offsets. This provides retryable at-least-once delivery, not a cross-database distributed transaction. Kafka retention does not replace tested DB/file backup.

## Minimal example

Illustrative Avro shape; inspect the actual file before changing it (`libs/hrflow-contracts/events/attendance-checked-in.v1.avsc`):

```json
{
  "type": "record",
  "name": "AttendanceCheckedIn",
  "namespace": "com.hrflow.contracts.events",
  "fields": [
    {"name": "eventId", "type": "string"},
    {"name": "tenantId", "type": "string"},
    {"name": "employeeId", "type": "string"},
    {"name": "sessionDate", "type": "string"},
    {"name": "checkInTime", "type": "string"},
    {"name": "workDate", "type": "string"}
  ]
}
```

## Application-specific implementation

Illustrative consumer shape, not a discovered implemented class:

```java
@Component
public class AttendanceEventConsumer {

    private final AttendanceProjectionService projectionService;

    public AttendanceEventConsumer(AttendanceProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @KafkaListener(topics = "hrflow.attendance.checked-in.v1", groupId = "notification-service")
    public void handleCheckIn(ConsumerRecord<String, AttendanceCheckedIn> record,
                              Acknowledgment acknowledgment) {
        AttendanceCheckedIn event = record.value();
        projectionService.updatePresence(event.getTenantId(), event.getEmployeeId());
        acknowledgment.acknowledge();
    }
}
```

The projection service must commit its effect and inbox before acknowledgment; the small example omits that implementation and is not a production consumer. ADR 008 supersedes the previous local-listener replacement.

## Common mistakes and pitfalls

Common pitfalls when operating Kafka:

> **Pitfall:** Assuming Kafka provides distributed transactions with relational databases out of the box. Writing to a database and publishing to Kafka without a Transactional Outbox creates the dual-write problem.

- Unbalanced partitions: Failing to specify a partition key sends messages in round-robin fashion, destroying order guarantees for dependent employee state transitions.
- Consumer lag blindness: Monitoring only broker process uptime while ignoring consumer group lag allows message backlogs to grow undetected for hours.
- Committing offsets before business effects: If a consumer commits its offset before its database transaction completes, a crash causes permanent message loss. Always commit offsets after state is durable.

## Testing and debugging

Verification methods:

- Embedded Kafka vs Testcontainers: Integration tests must use real Kafka through Testcontainers; the following image is an old example, not a verified current pin: `Testcontainers` running `confluentinc/cp-kafka:7.6.0` rather than lightweight embedded mocks.
- Partition distribution verification: Producing 10 messages with identical tenant keys confirms they land on the identical partition index.
- Poison pill handling: Publishing an invalid or malformed schema payload verifies that consumers route dead-letter messages to a designated `.DLT` error topic.

## Delivery tests before acceptance

- Producer stops after broker acknowledgment but before mark-sent: replay can duplicate publication.
- Consumer stops after DB commit but before offset: receipt suppresses a duplicate effect.
- Same tenant/aggregate revision cannot be applied twice or older state overwrite newer state.
- Retention gap requires owner snapshot bootstrap; do not silently rebuild from an incomplete log.
- Poison messages produce an observable dead-letter record and an authorized replay procedure.

## Related documentation

- [Transactional outbox primer](TRANSACTIONAL-OUTBOX.md)
- [Idempotency primer](IDEMPOTENCY.md)
- [Current architecture decision](../../adr/008-FINAL-SERVICE-AND-CLOUD-DESIGN.md)
- [System architecture](../../Architecture.md)

## One active owner relay

The approved design uses a PostgreSQL advisory lock, a named application-level lock, on one dedicated relay connection per owner service. Standbys do not publish; loss of that connection stops the active relay until leadership is reacquired. SKIP LOCKED bounds batches but cannot alone guarantee ordering across multiple publishers. Inbox/revision checks still protect against duplicate/stale delivery.
