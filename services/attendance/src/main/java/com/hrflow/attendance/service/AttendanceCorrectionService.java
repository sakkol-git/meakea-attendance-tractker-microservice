package com.hrflow.attendance.service;

import com.hrflow.attendance.dto.CorrectionRequest;
import com.hrflow.attendance.dto.CorrectionResponse;
import com.hrflow.attendance.entity.AttendanceCorrection;
import com.hrflow.attendance.mapper.CorrectionMapper;
import com.hrflow.attendance.repository.AttendanceCorrectionRepository;
import com.hrflow.attendance.repository.AttendanceSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class AttendanceCorrectionService {
    private final org.springframework.beans.factory.ObjectProvider<com.hrflow.attendance.daily.DailyStatusWriter> daily;

    private final AttendanceCorrectionRepository corrections;
    private final AttendanceSessionRepository sessions;
    private final CorrectionMapper mapper;
    private final com.hrflow.attendance.mapper.OriginalSessionMapper originals;
    private final CommandReceipts receipts;
    private final TenantSqlScope tenantSqlScope;
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final AttendanceScheduleResolver schedule;
    private final com.hrflow.attendance.events.SessionEventWriter sessionEvents;
    private final CorrectionAccess access;
    private final ManualAttendanceEntries manualEntries;
    private final com.hrflow.attendance.correction.CorrectionEventWriter correctionEvents;

    @Transactional
    public CorrectionResponse requestCorrection(UUID tenantId, UUID actorId, UUID employeeId,
            String idempotencyKey, CorrectionRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/attendance/corrections", idempotencyKey,
                request, CorrectionResponse.class, () -> performRequestCorrection(tenantId, employeeId, request));
    }

    private CorrectionResponse performRequestCorrection(UUID tenantId, UUID employeeId, CorrectionRequest request) {
        tenantSqlScope.lockEmployee(tenantId, employeeId);
        if (request.proposedClockIn() != null && request.proposedClockOut() != null
                && request.proposedClockOut().isBefore(request.proposedClockIn())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Proposed clock-out cannot precede proposed clock-in");
        }

        if (request.sessionId() != null) {
            sessions.findById(request.sessionId())
                    .filter(s -> s.getTenantId().equals(tenantId) && s.getEmployeeId().equals(employeeId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target session not found"));
        }

        AttendanceCorrection correction = new AttendanceCorrection(
                tenantId,
                employeeId,
                request.sessionId(),
                request.date(),
                request.proposedClockIn(),
                request.proposedClockOut(),
                request.reason()
        );

        correction.captureOriginalSessions(sessions.existsByTenantIdAndEmployeeIdAndWorkDateIsNull(tenantId, employeeId) ? null
                : json.writeValueAsString(sessions.originalSessions(tenantId, employeeId, request.date(), request.sessionId()).stream().map(originals::snapshot).toList()));
        AttendanceCorrection saved = corrections.saveAndFlush(correction);
        correctionEvents.append(saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public CorrectionResponse approveCorrection(UUID tenantId, CorrectionAccess.Caller caller, UUID correctionId, String ifMatch) {
        tenantSqlScope.use(tenantId);
        AttendanceCorrection correction = corrections.findForDecision(correctionId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Correction not found"));

        access.requireDecision(tenantId, caller, correction.getEmployeeId());

        if (!"PENDING".equals(correction.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Correction is already decided: " + correction.getStatus());
        }

        RevisionPreconditions.require(ifMatch, correction.getVersion());

        tenantSqlScope.lockEmployee(tenantId, correction.getEmployeeId());

        Instant now = Instant.now();
        correction.approve(caller.actor(), now);

        AttendanceCorrection saved = correction;

        // Apply replacement effective times if session exists without touching raw evidence
        if (saved.getSessionId() == null) {
            var manual = manualEntries.create(saved, now);
            saved.linkApprovedManualSession(manual.getId());
            sessionEvents.append(manual, "MANUAL");
        } else {
            var session = sessions.findById(saved.getSessionId())
                    .filter(row -> row.getTenantId().equals(tenantId) && row.getEmployeeId().equals(saved.getEmployeeId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target session not found"));
            var previousDate = session.getWorkDate();
            Instant effectiveIn = saved.getProposedClockIn() != null ? saved.getProposedClockIn() : session.getClockIn();
            Instant effectiveOut = saved.getProposedClockOut() != null ? saved.getProposedClockOut() : session.getClockOut();
            AttendanceScheduleResolver.Resolved replacementBasis = null;
            // Resolve references before mutating the managed entity: a query can otherwise auto-flush partial state.
            if (session.getWorkDate() == null || session.getWorkingDays() == null || !session.getWorkDate().equals(
                    effectiveIn.atZone(java.time.ZoneId.of(session.getTimezone())).toLocalDate())) {
                replacementBasis = schedule.at(tenantId, session.getEmployeeId(), effectiveIn);
            }
            session.applyCorrection(effectiveIn, effectiveOut, now);
            if (replacementBasis != null) {
                var basis = replacementBasis;
                session.correctSchedule(basis.workDate(), basis.zone().getId(), basis.shiftId(), basis.revision(),
                        basis.startsAt(), basis.endsAt(), basis.graceMinutes(), basis.workingDays());
            }
            sessions.saveAndFlush(session);
            sessionEvents.append(session, "MANUAL");
            if (previousDate != null && !previousDate.equals(session.getWorkDate())) {
                daily.ifAvailable(writer -> writer.refresh(tenantId, session.getEmployeeId(), previousDate));
            }
        }

        corrections.saveAndFlush(saved);
        correctionEvents.append(saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public CorrectionResponse rejectCorrection(UUID tenantId, CorrectionAccess.Caller caller, UUID correctionId, String ifMatch) {
        tenantSqlScope.use(tenantId);
        AttendanceCorrection correction = corrections.findForDecision(correctionId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Correction not found"));

        access.requireDecision(tenantId, caller, correction.getEmployeeId());

        if (!"PENDING".equals(correction.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Correction is already decided: " + correction.getStatus());
        }

        RevisionPreconditions.require(ifMatch, correction.getVersion());

        Instant now = Instant.now();
        correction.reject(caller.actor(), now);

        AttendanceCorrection saved = corrections.saveAndFlush(correction);
        correctionEvents.append(saved);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CorrectionResponse> listCorrections(UUID tenantId, CorrectionAccess.Caller caller, String status) {
        tenantSqlScope.use(tenantId);
        List<AttendanceCorrection> list;
        if (caller.hrAdmin()) {
            list = corrections.findByTenantIdOrderByCreatedAtDesc(tenantId);
        } else if (caller.manager()) {
            list = corrections.findCurrentDirectReports(tenantId, caller.employee());
        } else {
            list = corrections.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, caller.employee());
        }
        return list.stream().filter(row -> status == null || status.isBlank() || status.equals(row.getStatus()))
                .map(mapper::toResponse).toList();
    }

}
