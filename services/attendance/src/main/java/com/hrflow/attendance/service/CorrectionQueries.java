package com.hrflow.attendance.service;

import com.hrflow.attendance.dto.CorrectionListQuery;
import com.hrflow.attendance.dto.CorrectionPage;
import com.hrflow.attendance.dto.CorrectionResponse;
import com.hrflow.attendance.entity.AttendanceCorrection;
import com.hrflow.attendance.mapper.CorrectionMapper;
import com.hrflow.attendance.repository.AttendanceCorrectionRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CorrectionQueries {
    private final TenantSqlScope scope;
    private final CorrectionAccess access;
    private final AttendanceCorrectionRepository corrections;
    private final CorrectionMapper mapper;
    private final EntityManager entities;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CorrectionResponse detail(UUID tenant, CorrectionAccess.Caller caller, UUID id) {
        scope.use(tenant);
        var correction = corrections.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Correction not found"));
        if (!caller.employee().equals(correction.getEmployeeId())) access.requireDecision(tenant, caller, correction.getEmployeeId());
        return mapper.toResponse(correction);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CorrectionPage page(UUID tenant, CorrectionAccess.Caller caller, CorrectionListQuery query) {
        int offset = query.offset(); scope.use(tenant);
        var parameters = new LinkedHashMap<String, Object>(); parameters.put("tenant", tenant);
        StringBuilder where = new StringBuilder(" WHERE c.tenant_id=:tenant");
        if (!caller.hrAdmin()) {
            parameters.put("employee", caller.employee());
            if (caller.manager()) where.append("""
                     AND EXISTS (SELECT 1 FROM attendance.employee_projection e WHERE e.tenant_id=c.tenant_id
                      AND e.employee_id=c.employee_id AND e.manager_known AND e.manager_id=:employee AND e.status='ACTIVE')
                    """);
            else where.append(" AND c.employee_id=:employee");
        }
        if (query.status() != null && !query.status().isBlank()) {
            where.append(" AND c.status=:status"); parameters.put("status", query.status());
        }
        if (query.search() != null && !query.search().isBlank()) {
            where.append(" AND (lower(c.reason) LIKE :search ESCAPE '!' OR EXISTS (SELECT 1 FROM attendance.employee_projection e"
                    + " WHERE e.tenant_id=c.tenant_id AND e.employee_id=c.employee_id AND lower(e.full_name) LIKE :search ESCAPE '!'))");
            String text = query.search().strip().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
            parameters.put("search", "%" + text + "%");
        }
        if (query.minAgeDays() != null && query.minAgeDays() > 0) {
            where.append(" AND c.created_at<=:before"); parameters.put("before", clock.instant().minus(query.minAgeDays(), ChronoUnit.DAYS));
        }
        String order = "oldest".equals(query.sort()) ? " ASC" : " DESC";
        var count = entities.createNativeQuery("SELECT count(*) FROM attendance.attendance_correction c" + where, Long.class);
        var rows = entities.createNativeQuery("SELECT c.* FROM attendance.attendance_correction c" + where
                + " ORDER BY c.created_at" + order + ",c.id" + order, AttendanceCorrection.class);
        parameters.forEach((key, value) -> { count.setParameter(key, value); rows.setParameter(key, value); });
        rows.setFirstResult(offset).setMaxResults(query.pageSize());
        java.util.List<?> selected = rows.getResultList();
        return new CorrectionPage(selected.stream().map(AttendanceCorrection.class::cast).map(mapper::toResponse).toList(),
                query.pageNumber(), query.pageSize(), ((Number) count.getSingleResult()).longValue());
    }
}
