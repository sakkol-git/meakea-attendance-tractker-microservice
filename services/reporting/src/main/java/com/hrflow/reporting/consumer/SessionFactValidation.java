package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceSessionFact;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/** Validates the full owner snapshot before any tenant data or receipt is written. */
final class SessionFactValidation {
    static final String TOPIC = "attendance.session.v1";

    private SessionFactValidation() {}

    static Identity validate(ConsumerRecord<String, AttendanceSessionFact> record) {
        var fact = record.value();
        if (fact == null || !TOPIC.equals(record.topic()) || record.partition() < 0 || record.offset() < 0
                || fact.getSchemaVersion() != 1 || fact.getAggregateVersion() < 1
                || !"AttendanceSessionSnapshot.v1".equals(fact.getType()) || fact.getOccurredAt() == null) {
            throw new IllegalArgumentException("Invalid session fact envelope");
        }
        var identity = new Identity(UUID.fromString(fact.getTenantId()), UUID.fromString(fact.getAggregateId()),
                UUID.fromString(fact.getEmployeeId()), UUID.fromString(fact.getEventId()));
        UUID.fromString(fact.getCorrelationId());
        if (!(identity.tenant() + ":" + identity.employee()).equals(record.key())
                || fact.getWorkDate() == null || fact.getClockIn() == null || fact.getTimezone() == null
                || fact.getScheduledStart() == null || fact.getScheduledEnd() == null
                || !fact.getScheduledEnd().isAfter(fact.getScheduledStart())
                || fact.getGraceMinutes() < 0 || fact.getGraceMinutes() > 120
                || fact.getSource() == null || !Set.of("TAP", "MANUAL").contains(fact.getSource())
                || !fact.getWorkDate().equals(fact.getClockIn().atZone(ZoneId.of(fact.getTimezone())).toLocalDate())
                || fact.getClockOut() != null && fact.getClockOut().isBefore(fact.getClockIn())
                || !(fact.getClockOut() == null ? "OPEN" : "COMPLETED").equals(fact.getState())) {
            throw new IllegalArgumentException("Invalid session fact routing or effective snapshot");
        }
        return identity;
    }

    record Identity(UUID tenant, UUID session, UUID employee, UUID event) {}
}
