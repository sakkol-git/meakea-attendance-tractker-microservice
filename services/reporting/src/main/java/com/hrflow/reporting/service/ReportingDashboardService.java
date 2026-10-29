package com.hrflow.reporting.service;

import com.hrflow.reporting.dto.DashboardKpiResponse;
import com.hrflow.reporting.repository.AttendanceFactProjectionRepository;
import com.hrflow.reporting.repository.LeaveFactProjectionRepository;
import com.hrflow.reporting.repository.RequestFactProjectionRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportingDashboardService {

    private final AttendanceFactProjectionRepository attendanceRepo;
    private final LeaveFactProjectionRepository leaveRepo;
    private final RequestFactProjectionRepository requestRepo;
    private final TenantSqlScope tenantScope;
    private final ReportAccess access;
    private final com.hrflow.reporting.repository.DailyReportRepository daily;

    @Transactional(readOnly = true)
    public DashboardKpiResponse getDashboardKpis(UUID tenantId) {
        return getDashboardKpis(tenantId, null);
    }

    @Transactional(readOnly = true)
    public DashboardKpiResponse getDashboardKpis(UUID tenantId, UUID manager) {
        tenantScope.use(tenantId);
        access.requireKnownManager(tenantId, manager);
        LocalDate today = LocalDate.now();
        var counts = daily.today(tenantId, manager, java.time.Instant.now());
        long present = counts.present(), late = counts.late(), absent = counts.absent();
        long leaves = leaveRepo.countScopedActive(tenantId, manager, today);
        long pendingRequests = requestRepo.countScopedPending(tenantId, manager);

        Map<String, Object> summary = new HashMap<>();
        summary.put("date", today.toString());
        summary.put("totalAttendance", present + late);
        summary.put("reviewRequiredCount", counts.review());
        summary.put("provisionalCount", counts.provisional());
        summary.put("attendanceDateBasis", "employee-local");

        return new DashboardKpiResponse(present, late, absent, leaves, pendingRequests, summary);
    }

    public static final java.util.Set<String> ALLOWED_SORTS =
            java.util.Set.of("workDate", "status", "minutesLate", "minutesWorked");

    @Transactional(readOnly = true)
    public java.util.List<com.hrflow.reporting.entity.AttendanceFactProjection> getAttendanceReport(
            UUID tenantId,
            LocalDate start,
            LocalDate end,
            String sortBy
    ) {
        return getAttendanceReport(tenantId, null, start, end, sortBy);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.hrflow.reporting.entity.AttendanceFactProjection> getAttendanceReport(
            UUID tenantId, UUID manager, LocalDate start, LocalDate end, String sortBy) {
        tenantScope.use(tenantId);
        access.requireKnownManager(tenantId, manager);
        if (sortBy != null && !ALLOWED_SORTS.contains(sortBy)) {
            throw new IllegalArgumentException("Unknown or disallowed sort field: " + sortBy);
        }
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                sortBy != null ? sortBy : "workDate"
        ).descending();
        return attendanceRepo.findScoped(tenantId, manager, start, end, sort.and(
                org.springframework.data.domain.Sort.by("id")));
    }
}
