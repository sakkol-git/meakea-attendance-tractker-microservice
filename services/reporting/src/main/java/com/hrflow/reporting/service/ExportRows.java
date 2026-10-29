package com.hrflow.reporting.service;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Verified report sources share bounded capture; SQL identifiers are never client input. */
@Component
@RequiredArgsConstructor
public class ExportRows {
    private static final Map<String, Source> SOURCES = Map.of(
            "ATTENDANCE", new Source("""
                    SELECT employee_id,session_id,work_date,timezone,clock_in,clock_out,status,source,
                           minutes_worked,aggregate_revision,(clock_in IS NOT NULL)
                    FROM reporting.attendance_fact_projection WHERE tenant_id=:tenant
                      AND (cast(:fromDate as date) IS NULL OR work_date>=cast(:fromDate as date))
                      AND (cast(:toDate as date) IS NULL OR work_date<=cast(:toDate as date))
                    ORDER BY work_date,employee_id,session_id
                    """, List.of("employeeId", "sessionId", "workDate", "timezone", "clockIn", "clockOut",
                            "state", "source", "minutesWorked", "revision")),
            "LEAVE", new Source("""
                    SELECT employee_id,leave_id,leave_type,start_date,end_date,portion,portion_days,
                           working_dates::text,status,aggregate_revision,
                           (portion IS NOT NULL AND working_dates IS NOT NULL)
                    FROM reporting.leave_fact_projection WHERE tenant_id=:tenant
                      AND (cast(:fromDate as date) IS NULL OR end_date>=cast(:fromDate as date))
                      AND (cast(:toDate as date) IS NULL OR start_date<=cast(:toDate as date))
                    ORDER BY start_date,employee_id,leave_id
                    """, List.of("employeeId", "leaveId", "leaveType", "startDate", "endDate", "portion",
                            "chargedDays", "workingDates", "state", "revision")),
            "REQUESTS", new Source("""
                    SELECT requester_id,request_id,request_type,created_at,status,aggregate_revision,(created_at IS NOT NULL)
                    FROM reporting.request_fact_projection WHERE tenant_id=:tenant
                      AND (cast(:fromDate as date) IS NULL OR created_at IS NULL OR (created_at AT TIME ZONE 'UTC')::date>=cast(:fromDate as date))
                      AND (cast(:toDate as date) IS NULL OR created_at IS NULL OR (created_at AT TIME ZONE 'UTC')::date<=cast(:toDate as date))
                    ORDER BY created_at,requester_id,request_id
                    """, List.of("employeeId", "requestId", "requestType", "createdAt", "state", "revision")));
    private final EntityManager entityManager;
    private final org.springframework.beans.factory.ObjectProvider<com.hrflow.reporting.consumer.RequestReportBootstrap> requestReplay;

    @Transactional(propagation = Propagation.MANDATORY)
    public List<List<String>> capture(UUID tenant, String report, LocalDate from, LocalDate to, int cap) {
        if ("REQUESTS".equals(report)) {
            var replay = requestReplay.getIfAvailable();
            if (replay == null || !replay.isReady()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The request report source has not completed startup replay");
        }
        Source source = SOURCES.get(report);
        if (source == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "This report's live source is not available");
        @SuppressWarnings("unchecked")
        List<Object[]> facts = entityManager.createNativeQuery(source.sql()).setParameter("tenant", tenant)
                .setParameter("fromDate", from).setParameter("toDate", to).setMaxResults(cap + 1).getResultList();
        if (facts.size() > cap) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Export exceeds the row limit; choose a narrower date range");
        if (facts.stream().anyMatch(row -> !Boolean.TRUE.equals(row[row.length - 1]))) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The requested range contains rows without a verified owner basis");
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(source.header());
        for (Object[] fact : facts) rows.add(Arrays.stream(fact, 0, fact.length - 1).map(ExportRows::cell).toList());
        return rows;
    }

    private static String cell(Object value) {
        if (value == null) return "";
        if (value instanceof java.sql.Timestamp time) return time.toInstant().toString();
        if (value instanceof java.time.OffsetDateTime time) return time.toInstant().toString();
        return value.toString();
    }

    private record Source(String sql, List<String> header) {}
}
