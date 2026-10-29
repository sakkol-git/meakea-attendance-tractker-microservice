package com.hrflow.attendance.service;

import com.hrflow.attendance.dto.AttendanceSessionResponse;
import com.hrflow.attendance.dto.CheckInRequest;
import com.hrflow.attendance.dto.TodayAttendanceResponse;
import com.hrflow.attendance.entity.AttendanceEvidence;
import com.hrflow.attendance.entity.AttendanceSession;
import com.hrflow.attendance.events.SessionEventWriter;
import com.hrflow.attendance.mapper.AttendanceMapper;
import com.hrflow.attendance.repository.AttendanceEvidenceRepository;
import com.hrflow.attendance.repository.AttendanceSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceSessionRepository sessions;
    private final AttendanceEvidenceRepository evidenceRepository;
    private final AttendanceMapper mapper;
    private final CommandReceipts receipts;
    private final TenantSqlScope tenantSqlScope;
    private final AttendanceScheduleResolver schedule;
    private final SessionEventWriter sessionEvents;
    private final DailyAttendanceService daily;
    private final java.time.Clock clock;

    @Transactional
    public AttendanceSessionResponse checkIn(UUID tenantId, UUID actorId, UUID employeeId,
            String idempotencyKey, CheckInRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/attendance/check-in", idempotencyKey,
                request, AttendanceSessionResponse.class, () -> performCheckIn(tenantId, employeeId, request));
    }

    private AttendanceSessionResponse performCheckIn(UUID tenantId, UUID employeeId, CheckInRequest request) {
        tenantSqlScope.lockEmployee(tenantId, employeeId);
        Instant now = Instant.now();
        var basis = schedule.at(tenantId, employeeId, now);

        // Check if an open session already exists
        if (sessions.findByTenantIdAndEmployeeIdAndClockOutIsNull(tenantId, employeeId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Active attendance session already exists");
        }

        UUID observationId = UUID.randomUUID();
        AttendanceEvidence evidence = evidenceRepository.save(new AttendanceEvidence(
                tenantId, employeeId, observationId, now,
                request.latitude(), request.longitude(), request.accuracyMeters(), request.deviceModel()));

        AttendanceSession session;
        try {
            session = new AttendanceSession(tenantId, employeeId, now, evidence.getId());
            session.captureSchedule(basis.workDate(), basis.zone().getId(), basis.shiftId(), basis.revision(),
                    basis.startsAt(), basis.endsAt(), basis.graceMinutes(), basis.workingDays());
            session = sessions.saveAndFlush(session);
        } catch (DataIntegrityViolationException conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Simultaneous check-in detected", conflict);
        }

        sessionEvents.append(session, "TAP");
        return toResponse(session);
    }

    @Transactional
    public AttendanceSessionResponse checkOut(UUID tenantId, UUID actorId, UUID employeeId,
            String idempotencyKey, String ifMatch, CheckInRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/attendance/check-out", idempotencyKey,
                request, AttendanceSessionResponse.class, () -> performCheckOut(tenantId, employeeId, ifMatch, request));
    }

    private AttendanceSessionResponse performCheckOut(UUID tenantId, UUID employeeId,
            String ifMatch, CheckInRequest request) {
        tenantSqlScope.lockEmployee(tenantId, employeeId);
        AttendanceSession session = sessions.findByTenantIdAndEmployeeIdAndClockOutIsNull(tenantId, employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No open session to check out"));

        RevisionPreconditions.require(ifMatch, session.getVersion());
        if (session.getWorkDate() == null || session.getEffectiveSource() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Legacy session requires a verified effective schedule and source before checkout");
        }

        Instant now = Instant.now();
        UUID observationId = UUID.randomUUID();
        AttendanceEvidence evidence = evidenceRepository.save(new AttendanceEvidence(
                tenantId, employeeId, observationId, now,
                request.latitude(), request.longitude(), request.accuracyMeters(), request.deviceModel()));

        session.close(now, evidence.getId());
        AttendanceSession saved = sessions.saveAndFlush(session);

        sessionEvents.append(saved, saved.getEffectiveSource());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TodayAttendanceResponse today(UUID tenantId, UUID employeeId) {
        tenantSqlScope.use(tenantId);
        var date = schedule.at(tenantId, employeeId, clock.instant()).workDate();
        List<AttendanceSessionResponse> list = sessions.findByTenantIdAndEmployeeIdOrderByClockInDesc(tenantId, employeeId).stream()
                .filter(session -> date.equals(session.getWorkDate()))
                .map(this::toResponse)
                .toList();
        String status = list.stream()
                .filter(s -> s.clockOut() == null)
                .findFirst()
                .map(AttendanceSessionResponse::status)
                .orElse(list.isEmpty() ? null : list.getFirst().status());
        return new TodayAttendanceResponse(date.toString(), status, list);
    }

    @Transactional(readOnly = true)
    public com.hrflow.attendance.dto.DailyStatusResponse dailyStatusMe(
            UUID tenantId, UUID employeeId, java.time.LocalDate queryDate) {
        return daily.forEmployee(tenantId, employeeId, queryDate);
    }

    @Transactional(readOnly = true)
    public List<AttendanceSessionResponse> mySessions(UUID tenantId, UUID employeeId) {
        tenantSqlScope.use(tenantId);
        return sessions.findByTenantIdAndEmployeeIdOrderByClockInDesc(tenantId, employeeId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AttendanceSessionResponse toResponse(AttendanceSession session) {
        return mapper.toResponse(session);
    }
}
