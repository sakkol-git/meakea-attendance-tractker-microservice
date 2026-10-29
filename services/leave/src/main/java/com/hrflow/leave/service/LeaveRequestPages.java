package com.hrflow.leave.service;

import com.hrflow.leave.dto.LeaveListQuery;
import com.hrflow.leave.dto.LeavePage;
import com.hrflow.leave.dto.LeaveRecordResponse;
import com.hrflow.leave.entity.LeaveRequest;
import com.hrflow.leave.mapper.LeaveRequestMapper;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveRequestPages {
    private final EntityManager entities;
    private final TenantSqlScope scope;
    private final LeaveAccess access;
    private final LeaveRequestMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public LeavePage<LeaveRecordResponse> read(String tenant, LeaveCaller caller, boolean pending, LeaveListQuery filter) {
        scope.use(tenant);
        var page = filter.request();
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("tenant", tenant);
        StringBuilder where = new StringBuilder(" WHERE r.tenant_id=:tenant");
        if (pending) {
            String manager = access.pendingScope(tenant, caller);
            where.append(manager == null ? " AND r.status IN ('PENDING','NEEDS_REVIEW')" : " AND r.status='PENDING'");
            if (manager != null) {
                where.append("""
                         AND r.employee_id<>:manager AND EXISTS
                          (SELECT 1 FROM leave.employee_projection e WHERE e.tenant_id=r.tenant_id
                           AND e.employee_id=r.employee_id AND e.manager_id=:manager AND e.active=true)
                        """);
                parameters.put("manager", manager);
            }
        } else {
            where.append(" AND r.employee_id=:employee");
            parameters.put("employee", caller.employee());
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            where.append(" AND r.status=:status");
            parameters.put("status", filter.status());
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            where.append(" AND (lower(r.title) LIKE :search ESCAPE '!' OR lower(r.type) LIKE :search ESCAPE '!'"
                    + " OR lower(r.employee_name) LIKE :search ESCAPE '!')");
            String literal = filter.search().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
            parameters.put("search", "%" + literal + "%");
        }
        if (filter.minAgeDays() != null && filter.minAgeDays() > 0) {
            where.append(" AND r.created_at<=:before");
            parameters.put("before", clock.instant().minus(filter.minAgeDays(), ChronoUnit.DAYS));
        }
        var count = entities.createNativeQuery("SELECT count(*) FROM leave.leave_request r" + where, Long.class);
        String order = filter.newest(pending) ? " DESC" : " ASC";
        var rows = entities.createNativeQuery("SELECT r.* FROM leave.leave_request r" + where
                + " ORDER BY r.created_at" + order + ",r.id" + order, LeaveRequest.class);
        parameters.forEach((name, value) -> { count.setParameter(name, value); rows.setParameter(name, value); });
        rows.setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize());
        java.util.List<?> results = rows.getResultList();
        var items = results.stream().map(LeaveRequest.class::cast).map(mapper::toResponse).toList();
        return new LeavePage<>(items, page.getPageNumber(), page.getPageSize(), ((Number) count.getSingleResult()).longValue());
    }
}
