package com.hrflow.reporting.consumer;

import com.hrflow.contracts.attendance.AttendanceCheckedIn;
import com.hrflow.reporting.dto.AttendanceFactEvent;
import com.hrflow.reporting.dto.LeaveFactEvent;
import com.hrflow.reporting.dto.RequestFactEvent;
import com.hrflow.reporting.entity.AttendanceFactProjection;
import com.hrflow.reporting.entity.LeaveFactProjection;
import com.hrflow.reporting.entity.ReportingInbox;
import com.hrflow.reporting.entity.RequestFactProjection;
import com.hrflow.reporting.repository.AttendanceFactProjectionRepository;
import com.hrflow.reporting.repository.LeaveFactProjectionRepository;
import com.hrflow.reporting.repository.ReportingInboxRepository;
import com.hrflow.reporting.repository.RequestFactProjectionRepository;
import com.hrflow.reporting.service.TenantSqlScope;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingProjectionConsumer {

    public static final String CONSUMER_NAME = "reporting-projection-consumer";

    private final AttendanceFactProjectionRepository attendanceRepo;
    private final LeaveFactProjectionRepository leaveRepo;
    private final RequestFactProjectionRepository requestRepo;
    private final ReportingInboxRepository inboxRepo;
    private final TenantSqlScope tenantScope;
    private final jakarta.persistence.EntityManager entities;

    /**
     * Processes an incoming AttendanceCheckedIn Avro record from Kafka.
     * Enforces that unsupported check-in events lacking aggregate revision and session facts
     * are strictly BLOCKED and never persisted with fabricated or sentinel revisions, nor acknowledged.
     */
    public boolean processAttendanceCheckedIn(String key, AttendanceCheckedIn event, Runnable onCommitAck) {
        if (event == null || event.getTenantId() == null || event.getEventId() == null) {
            throw new IllegalArgumentException("Invalid attendance event envelope: null mandatory fields");
        }

        // Invariant: Do not replace fabricated revision 1 with fabricated sentinel revision 0.
        // The event has no aggregate revision or session facts. Do not persist it into a model requiring one.
        // Keep the unsupported listener disabled and mark it BLOCKED until an accepted event/receipt schema exists.
        throw new UnsupportedOperationException(
                "AttendanceCheckedIn processing is BLOCKED: event has no aggregate revision or session facts; awaiting accepted session snapshot event schema"
        );
    }

    @Transactional
    public void processAttendanceFact(AttendanceFactEvent event) {
        tenantScope.use(event.tenantId());

        if (inboxRepo.existsByConsumerAndEvent(CONSUMER_NAME, event.tenantId(), event.eventId())) {
            return;
        }

        Optional<AttendanceFactProjection> existing =
                attendanceRepo.findForUpdate(event.tenantId(), event.sessionId());

        if (existing.isPresent()) {
            existing.get().updateFact(
                    event.employeeId(),
                    event.workDate(),
                    event.status(),
                    event.minutesLate(),
                    event.minutesWorked(),
                    event.aggregateRevision()
            );
            attendanceRepo.save(existing.get());
        } else {
            AttendanceFactProjection projection = new AttendanceFactProjection(
                    event.tenantId(),
                    event.sessionId(),
                    event.employeeId(),
                    event.workDate(),
                    event.status(),
                    event.minutesLate(),
                    event.minutesWorked(),
                    event.aggregateRevision()
            );
            attendanceRepo.save(projection);
        }

        inboxRepo.save(new ReportingInbox(
                CONSUMER_NAME,
                event.tenantId(),
                event.eventId(),
                "hrflow.attendance.facts",
                event.sessionId(),
                event.aggregateRevision()
        ));
    }

    @Transactional
    public void processLeaveFact(LeaveFactEvent event) {
        tenantScope.use(event.tenantId());

        if (inboxRepo.existsByConsumerAndEvent(CONSUMER_NAME, event.tenantId(), event.eventId())) {
            return;
        }

        Optional<LeaveFactProjection> existing =
                leaveRepo.findForUpdate(event.tenantId(), event.leaveId());

        if (existing.isPresent()) {
            existing.get().updateFact(
                    event.employeeId(),
                    event.leaveType(),
                    event.startDate(),
                    event.endDate(),
                    event.portionDays(),
                    event.status(),
                    event.aggregateRevision()
            );
            leaveRepo.save(existing.get());
        } else {
            LeaveFactProjection projection = new LeaveFactProjection(
                    event.tenantId(),
                    event.leaveId(),
                    event.employeeId(),
                    event.leaveType(),
                    event.startDate(),
                    event.endDate(),
                    event.portionDays(),
                    event.status(),
                    event.aggregateRevision()
            );
            leaveRepo.save(projection);
        }

        inboxRepo.save(new ReportingInbox(
                CONSUMER_NAME,
                event.tenantId(),
                event.eventId(),
                "hrflow.leave.facts",
                event.leaveId(),
                event.aggregateRevision()
        ));
    }

    @Transactional
    public void processRequestFact(RequestFactEvent event) {
        tenantScope.use(event.tenantId());
        entities.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .setParameter("key", "reporting.request:" + event.tenantId() + ":" + event.requestId()).getSingleResult();

        if (inboxRepo.existsByConsumerAndEvent(CONSUMER_NAME, event.tenantId(), event.eventId())) {
            return;
        }

        Optional<RequestFactProjection> existing =
                requestRepo.findForUpdate(event.tenantId(), event.requestId());

        if (existing.isPresent()) {
            if (existing.get().getCreatedAt() != null && event.aggregateRevision() > existing.get().getAggregateRevision()) {
                if (!existing.get().getRequesterId().equals(event.requesterId()) || !existing.get().getRequestType().equals(event.requestType())) {
                    throw new IllegalArgumentException("Verified request identity cannot change");
                }
                existing.get().retainCreationTime(event.createdAt());
            }
            existing.get().updateFact(
                    event.requesterId(),
                    event.requestType(),
                    event.status(),
                    event.aggregateRevision()
            );
            requestRepo.save(existing.get());
        } else {
            RequestFactProjection projection = new RequestFactProjection(
                    event.tenantId(),
                    event.requestId(),
                    event.requesterId(),
                    event.requestType(),
                    event.status(),
                    event.aggregateRevision()
            );
            projection.retainCreationTime(event.createdAt());
            requestRepo.save(projection);
        }

        inboxRepo.save(new ReportingInbox(
                CONSUMER_NAME,
                event.tenantId(),
                event.eventId(),
                "hrflow.request.facts",
                event.requestId(),
                event.aggregateRevision()
        ));
    }
}
