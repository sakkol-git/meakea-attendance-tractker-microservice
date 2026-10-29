package com.hrflow.identityorg.audit;

import com.hrflow.identityorg.organization.service.TenantSqlScope;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityAuditService {
    private final AuditRepository records;
    private final AuditMapper mapper;
    private final TenantSqlScope scope;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID tenant, UUID actor, String action, String entity,
            UUID id, Map<String, Object> changes) {
        scope.use(tenant);
        records.save(new IdentityAudit(tenant, actor, action, entity, id, changes, clock.instant()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordChanges(UUID tenant, UUID actor, String action, String entity, UUID id,
            Map<String, Object> before, Map<String, Object> after) {
        var changes = new LinkedHashMap<String, Object>();
        after.forEach((field, value) -> {
            if (!Objects.equals(before.get(field), value)) {
                var change = new LinkedHashMap<String, Object>();
                if (before.containsKey(field)) change.put("before", before.get(field));
                change.put("after", value);
                changes.put(field, change);
            }
        });
        if (!changes.isEmpty()) record(tenant, actor, action, entity, id, changes);
    }

    @Transactional(readOnly = true)
    public AuditPage search(UUID tenant, AuditQuery query) {
        scope.use(tenant);
        // Scope is unconditional; optional filters can only narrow this tenant's rows.
        Specification<IdentityAudit> filters = (root, ignored, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenant));
            if (query.actorId() != null) predicates.add(cb.equal(root.get("actorId"), query.actorId()));
            if (query.entity() != null) predicates.add(cb.equal(root.get("entity"), query.entity()));
            if (query.actor() != null && !query.actor().isBlank()) {
                predicates.add(cb.like(root.get("actorId").cast(String.class), literalContains(query.actor()), '\\'));
            }
            if (query.action() != null && !query.action().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("action")), literalContains(query.action()), '\\'));
            }
            if (query.target() != null && !query.target().isBlank()) {
                predicates.add(cb.like(root.get("entityId").cast(String.class), literalContains(query.target()), '\\'));
            }
            if (query.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("at"),
                    query.from().atStartOfDay(ZoneOffset.UTC).toInstant()));
            if (query.to() != null) predicates.add(cb.lessThan(root.get("at"),
                    query.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var page = records.findAll(filters, PageRequest.of(query.page(), query.size(),
                Sort.by(Sort.Direction.DESC, "at", "id")));
        return new AuditPage(page.getContent().stream().map(mapper::toEntry).toList(),
                query.page(), query.size(), page.getTotalElements());
    }

    private static String literalContains(String text) {
        return "%" + text.strip().toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
