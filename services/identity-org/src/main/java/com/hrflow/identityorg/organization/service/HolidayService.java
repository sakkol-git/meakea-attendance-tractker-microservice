package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.audit.IdentityAuditService;
import com.hrflow.identityorg.organization.dto.HolidayInput;
import com.hrflow.identityorg.organization.dto.HolidayPatch;
import com.hrflow.identityorg.organization.dto.HolidayRecordV2;
import com.hrflow.identityorg.organization.entity.Holiday;
import com.hrflow.identityorg.organization.repository.HolidayRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class HolidayService {
    private final HolidayRepository holidays;
    private final TenantSqlScope tenantSqlScope;
    private final CommandReceipts receipts;
    private final OrganizationFacts facts;
    private final IdentityAuditService audit;

    @Transactional(readOnly = true)
    public List<HolidayRecordV2> list(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return holidays.findByTenantIdAndDeletedAtIsNullOrderByDateAsc(tenantId).stream()
                .map(this::record).toList();
    }

    @Transactional
    public HolidayRecordV2 create(UUID tenantId, UUID actorId, String key, HolidayInput request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/holidays", key, request,
                HolidayRecordV2.class, () -> {
                    Holiday saved = holidays.saveAndFlush(new Holiday(tenantId,
                            request.date(), request.name().strip(), request.kind().name()));
                    facts.holiday(saved, "org.holiday.added.v1");
                    audit.recordChanges(tenantId, actorId, "HOLIDAY_CREATED", "holiday", saved.getId(),
                            Map.of(), auditFields(saved));
                    return record(saved);
                });
    }

    @Transactional
    public HolidayRecordV2 patch(UUID tenantId, UUID actorId, UUID id, String ifMatch, HolidayPatch request) {
        tenantSqlScope.use(tenantId);
        Holiday row = holidays.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        RevisionPreconditions.require(ifMatch, row.getVersion());
        var before = auditFields(row);
        row.update(request.name() == null ? null : request.name().strip(), request.date(),
                request.kind() == null ? null : request.kind().name());
        Holiday saved = holidays.saveAndFlush(row);
        facts.holiday(saved, "org.holiday.updated.v1");
        audit.recordChanges(tenantId, actorId, "HOLIDAY_UPDATED", "holiday", id, before, auditFields(saved));
        return record(saved);
    }

    @Transactional
    public void delete(UUID tenantId, UUID actorId, UUID id, String ifMatch) {
        tenantSqlScope.use(tenantId);
        Holiday row = holidays.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        RevisionPreconditions.require(ifMatch, row.getVersion());
        row.delete(Instant.now());
        facts.holiday(holidays.saveAndFlush(row), "org.holiday.removed.v1");
        audit.record(tenantId, actorId, "HOLIDAY_DELETED", "holiday", id,
                Map.of("deletedAt", Map.of("after", row.getDeletedAt().toString())));
    }

    private Map<String, Object> auditFields(Holiday holiday) {
        return Map.of("name", holiday.getName(), "date", holiday.getDate().toString(), "kind", holiday.getKind());
    }

    private HolidayRecordV2 record(Holiday row) {
        return new HolidayRecordV2(row.getId(), row.getName(), row.getDate(), row.getKind(), row.getVersion());
    }
}
