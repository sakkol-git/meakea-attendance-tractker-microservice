package com.hrflow.request.service;

import com.hrflow.request.dto.RequestListQuery;
import com.hrflow.request.dto.RequestPage;
import com.hrflow.request.entity.GeneralRequest;
import com.hrflow.request.entity.MaterialItem;
import com.hrflow.request.entity.RequestType;
import com.hrflow.request.mapper.GeneralRequestMapper;
import com.hrflow.request.mapper.MaterialItemMapper;
import com.hrflow.request.mapper.RequestTypeMapper;
import com.hrflow.request.organization.RequestRelationships;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Authorization and filters precede both the count and bounded owner query. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class RequestPages {
    private final EntityManager entities;
    private final TenantSqlScope scope;
    private final RequestRelationships relationships;
    private final GeneralRequestMapper requestMapper;
    private final RequestTypeMapper typeMapper;
    private final MaterialItemMapper materialMapper;
    private final Clock clock;

    public RequestPage<com.hrflow.request.dto.GeneralRequestResponse> requests(String tenant, String employee, Set<String> roles,
            boolean inbox, RequestListQuery filter) {
        filter.offset();
        scope.use(tenant);
        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("tenant", tenant);
        parameters.put("employee", employee);
        StringBuilder where = new StringBuilder(" WHERE r.tenant_id=:tenant");
        if (inbox) inbox(where, parameters, tenant, employee, roles);
        else where.append(" AND r.employee_id=:employee");
        filters(where, parameters, filter, "lower(r.title) LIKE :search ESCAPE '!' OR lower(r.type) LIKE :search ESCAPE '!' OR lower(r.employee_name) LIKE :search ESCAPE '!'");
        if (filter.status() != null && !filter.status().isBlank()) {
            where.append(" AND r.status=:status"); parameters.put("status", filter.status());
        }
        if (filter.minAgeDays() != null && filter.minAgeDays() > 0) {
            where.append(" AND r.created_at<=:before");
            parameters.put("before", clock.instant().minus(filter.minAgeDays(), ChronoUnit.DAYS));
        }
        String order = filter.newest(inbox) ? " DESC" : " ASC";
        return read("general_request", GeneralRequest.class, requestMapper::toResponse, where.toString(), parameters,
                "r.created_at" + order + ",r.id" + order, filter);
    }

    public RequestPage<com.hrflow.request.dto.RequestTypeResponse> types(String tenant, RequestListQuery filter) {
        return catalog(tenant, filter, "request_type", RequestType.class, typeMapper::toResponse);
    }

    public RequestPage<com.hrflow.request.dto.MaterialItemResponse> materials(String tenant, RequestListQuery filter) {
        return catalog(tenant, filter, "material_item", MaterialItem.class, materialMapper::toResponse);
    }

    private void inbox(StringBuilder where, Map<String, Object> parameters, String tenant, String employee, Set<String> roles) {
        boolean hr = roles.contains("HR_ADMIN") || roles.contains("ROLE_HR_ADMIN");
        boolean manager = roles.contains("MANAGER") || roles.contains("ROLE_MANAGER");
        if (!hr && !manager) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager or HR admin required");
        if (manager) relationships.current(tenant, employee);
        where.append("""
                 AND r.status='PENDING_APPROVAL' AND r.employee_id<>:employee
                 AND EXISTS (SELECT 1 FROM request.approval_step s
                  WHERE s.tenant_id=r.tenant_id AND s.request_id=r.id AND s.status='PENDING'
                   AND NOT EXISTS (SELECT 1 FROM request.approval_step earlier
                    WHERE earlier.tenant_id=s.tenant_id AND earlier.request_id=s.request_id
                     AND earlier.status='PENDING' AND earlier.step_order<s.step_order)
                   AND (
                """);
        if (hr) where.append("s.approver_role='HR_ADMIN'");
        if (hr && manager) where.append(" OR ");
        if (manager) where.append("""
                (s.approver_role='MANAGER' AND s.approver_id=:employee AND EXISTS
                 (SELECT 1 FROM request.employee_projection e WHERE e.tenant_id=r.tenant_id
                   AND e.employee_id=r.employee_id AND e.active=true AND e.manager_id=:employee))
                """);
        where.append("))");
    }

    private <T, R> RequestPage<R> catalog(String tenant, RequestListQuery filter, String table, Class<T> entity, Function<T, R> mapper) {
        filter.offset(); scope.use(tenant);
        var parameters = new LinkedHashMap<String, Object>(); parameters.put("tenant", tenant);
        var where = new StringBuilder(" WHERE r.tenant_id=:tenant AND r.active=true");
        filters(where, parameters, filter, "lower(r.name) LIKE :search ESCAPE '!' OR lower(r.code) LIKE :search ESCAPE '!'");
        return read(table, entity, mapper, where.toString(), parameters, "r.name ASC,r.id ASC", filter);
    }

    private void filters(StringBuilder where, Map<String, Object> parameters, RequestListQuery filter, String search) {
        if (filter.search() == null || filter.search().isBlank()) return;
        where.append(" AND (").append(search).append(")");
        String literal = filter.search().strip().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        parameters.put("search", "%" + literal + "%");
    }

    private <T, R> RequestPage<R> read(String table, Class<T> entity, Function<T, R> mapper, String where,
            Map<String, Object> parameters, String order, RequestListQuery filter) {
        var count = entities.createNativeQuery("SELECT count(*) FROM request." + table + " r" + where, Long.class);
        var rows = entities.createNativeQuery("SELECT r.* FROM request." + table + " r" + where + " ORDER BY " + order, entity);
        parameters.forEach((name, value) -> { count.setParameter(name, value); rows.setParameter(name, value); });
        rows.setFirstResult(filter.offset()).setMaxResults(filter.pageSize());
        java.util.List<?> result = rows.getResultList();
        return new RequestPage<>(result.stream().map(entity::cast).map(mapper).toList(), filter.pageNumber(), filter.pageSize(),
                ((Number) count.getSingleResult()).longValue());
    }
}
