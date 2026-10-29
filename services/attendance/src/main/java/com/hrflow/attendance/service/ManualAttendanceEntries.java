package com.hrflow.attendance.service;

import com.hrflow.attendance.entity.AttendanceCorrection;
import com.hrflow.attendance.entity.AttendanceSession;
import com.hrflow.attendance.repository.AttendanceSessionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Creates approved manual evidence without manufacturing a tap/location observation. */
@Service
@RequiredArgsConstructor
public class ManualAttendanceEntries {
    private final AttendanceScheduleResolver schedule;
    private final AttendanceSessionRepository sessions;
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.MANDATORY)
    public AttendanceSession create(AttendanceCorrection correction, Instant approvedAt) {
        Instant in = correction.getProposedClockIn(), out = correction.getProposedClockOut();
        if (in == null || out != null && out.isBefore(in)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A manual entry requires valid clock-in/out times");
        }
        UUID tenant = correction.getTenantId(), employee = correction.getEmployeeId();
        var basis = schedule.at(tenant, employee, in);
        if (!basis.workDate().equals(correction.getDate())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Correction date must match the assigned-zone work date");
        }
        boolean overlap = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM attendance.attendance_session
                  WHERE tenant_id=? AND employee_id=?
                  AND clock_in<COALESCE(cast(? as timestamptz),'infinity'::timestamptz)
                  AND COALESCE(clock_out,'infinity'::timestamptz)>cast(? as timestamptz))
                """, Boolean.class, tenant, employee, out == null ? null : java.sql.Timestamp.from(out),
                java.sql.Timestamp.from(in)));
        if (overlap) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "An attendance interval already exists; correct the existing session instead");
        var session = new AttendanceSession(tenant, employee, in, null);
        session.captureSchedule(basis.workDate(), basis.zone().getId(), basis.shiftId(), basis.revision(),
                basis.startsAt(), basis.endsAt(), basis.graceMinutes(), basis.workingDays());
        session.applyCorrection(in, out, approvedAt);
        return sessions.saveAndFlush(session);
    }
}
