package com.hrflow.leave.conflict;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
import com.hrflow.contracts.attendance.AttendanceDailyWire;
import com.hrflow.contracts.attendance.DailyLeaveReference;
import com.hrflow.leave.repository.LeaveDayRepository;
import com.hrflow.leave.repository.LeaveRequestRepository;
import com.hrflow.leave.service.LeaveOutboxWriter;
import com.hrflow.leave.service.TenantSqlScope;
import java.time.Instant;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** A committed explicit overlap requests HR review. Only an HR policy decision may later change its debit. */
@Service
@RequiredArgsConstructor
public class LeaveAttendanceReview {
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;
    private final LeaveRequestRepository requests;
    private final LeaveDayRepository days;
    private final LeaveOutboxWriter outbox;

    @Transactional
    public void apply(String key, AttendanceDailyFact fact) {
        var id = AttendanceDailyWire.validate(key, fact);
        String tenant = id.tenant().toString(), employee = id.employee().toString();
        scope.use(tenant);
        scope.lockEmployee(tenant, employee);
        if (jdbc.update("INSERT INTO leave.attendance_inbox(tenant_id,event_id) VALUES (?,?) ON CONFLICT DO NOTHING", tenant, id.event()) == 0) return;
        var prior = jdbc.queryForList("SELECT aggregate_id,revision FROM leave.attendance_day_revision WHERE tenant_id=? AND employee_id=? AND work_date=?", tenant, employee, fact.getWorkDate());
        if (!prior.isEmpty()) {
            if (!id.day().equals(prior.getFirst().get("aggregate_id"))) throw new IllegalArgumentException("Daily aggregate identity changed");
            if (((Number) prior.getFirst().get("revision")).longValue() >= fact.getAggregateVersion()) return;
        }
        for (var reference : fact.getLeaveReferences().stream().sorted(Comparator.comparing(DailyLeaveReference::getLeaveId)).toList()) {
            if (!Boolean.TRUE.equals(reference.getWorkRecorded())) continue; // Legacy unknown is never evidence of work during leave.
            var leave = requests.findForDecision(tenant, reference.getLeaveId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Referenced leave owner record is unavailable"));
            if (!employee.equals(leave.getEmployeeId()) || !reference.getPortion().equals(leave.getPortion()))
                throw new IllegalArgumentException("Attendance leave reference identity or portion mismatch");
            // Cancellation/rejection and later owner revisions win; stale facts cannot resurrect or refund them.
            if (!"APPROVED".equals(leave.getStatus()) || Math.addExact(leave.getVersion(), 1L) != reference.getRevision()) continue;
            boolean reserved = days.findByTenantIdAndRequestId(tenant, leave.getId()).stream().anyMatch(day ->
                    employee.equals(day.getEmployeeId()) && fact.getWorkDate().equals(day.getDate()) && reference.getPortion().equals(day.getPortion()));
            if (!reserved) throw new IllegalArgumentException("Attendance conflict is outside the original reserved dates");
            leave.setStatus("NEEDS_REVIEW"); leave.setVersion(Math.addExact(leave.getVersion(), 1L)); leave.setUpdatedAt(Instant.now());
            requests.saveAndFlush(leave);
            jdbc.update("""
                    INSERT INTO leave.attendance_review(tenant_id,event_id,leave_id,work_date,attendance_id,attendance_revision,approval_revision,portion)
                    VALUES (?,?,?,?,?,?,?,?)
                    """, tenant, id.event(), leave.getId(), fact.getWorkDate(), id.day(), fact.getAggregateVersion(), reference.getRevision(), reference.getPortion());
            outbox.recordEvent(tenant, "LEAVE_REQUEST", leave.getId(), "org.leave.request.review-required.v1", leave, leave.getVersion());
        }
        jdbc.update("""
                INSERT INTO leave.attendance_day_revision(tenant_id,employee_id,work_date,aggregate_id,revision) VALUES (?,?,?,?,?)
                ON CONFLICT(tenant_id,employee_id,work_date) DO UPDATE SET revision=EXCLUDED.revision
                """, tenant, employee, fact.getWorkDate(), id.day(), fact.getAggregateVersion());
    }
}
