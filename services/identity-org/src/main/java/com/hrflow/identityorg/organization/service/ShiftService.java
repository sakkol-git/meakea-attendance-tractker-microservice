package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.audit.IdentityAuditService;
import com.hrflow.identityorg.organization.dto.ShiftInput;
import com.hrflow.identityorg.organization.dto.ShiftPatch;
import com.hrflow.identityorg.organization.dto.ShiftRecordV2;
import com.hrflow.identityorg.organization.entity.Shift;
import com.hrflow.identityorg.organization.entity.WorkSite;
import com.hrflow.identityorg.organization.repository.ShiftRepository;
import com.hrflow.identityorg.organization.repository.WorkSiteRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShiftService {
    private final ShiftRepository shifts;
    private final WorkSiteRepository sites;
    private final TenantSqlScope tenantSqlScope;
    private final CommandReceipts receipts;
    private final OrganizationFacts facts;
    private final IdentityAuditService audit;

    @Transactional(readOnly = true)
    public List<ShiftRecordV2> list(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return shifts.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId).stream()
                .map(this::record).toList();
    }

    @Transactional
    public ShiftRecordV2 create(UUID tenantId, UUID actorId, String key, ShiftInput request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/shifts", key, request,
                ShiftRecordV2.class, () -> {
                    validate(tenantId, request.siteId(), request.timezone(), request.startTime(), request.endTime());
                    Shift saved = shifts.saveAndFlush(new Shift(tenantId,
                            request.name().strip(), request.startTime(), request.endTime(),
                            request.graceMinutes() == null ? 10 : request.graceMinutes(),
                            request.workingDays(), request.siteId(), request.timezone(), request.reason()));
                    facts.shift(saved, "org.shift.defined.v1");
                    audit.recordChanges(tenantId, actorId, "SHIFT_CREATED", "shift", saved.getId(),
                            Map.of(), auditFields(saved));
                    return record(saved);
                });
    }

    @Transactional
    public ShiftRecordV2 patch(UUID tenantId, UUID actorId, UUID id, String ifMatch, ShiftPatch request) {
        tenantSqlScope.use(tenantId);
        Shift shift = shifts.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        RevisionPreconditions.require(ifMatch, shift.getVersion());
        var before = auditFields(shift);
        UUID siteId = request.siteId() == null ? shift.getSiteId() : request.siteId();
        String zone = request.timezone() == null ? shift.getTimezone() : request.timezone();
        LocalTime start = request.startTime() == null ? shift.getStartTime() : request.startTime();
        LocalTime end = request.endTime() == null ? shift.getEndTime() : request.endTime();
        validate(tenantId, siteId, zone, start, end);
        shift.update(request.name() == null ? null : request.name().strip(), request.startTime(),
                request.endTime(), request.graceMinutes(), request.workingDays(), request.siteId(),
                request.timezone());
        shift.recordReason(request.reason());
        Shift saved = shifts.saveAndFlush(shift);
        facts.shift(saved, "org.shift.updated.v1");
        audit.recordChanges(tenantId, actorId, "SHIFT_UPDATED", "shift", id, before, auditFields(saved));
        return record(saved);
    }

    private Map<String, Object> auditFields(Shift shift) {
        var fields = new LinkedHashMap<String, Object>(Map.of("name", shift.getName(),
                "startTime", shift.getStartTime().toString(), "endTime", shift.getEndTime().toString(),
                "graceMinutes", shift.getGraceMinutes(), "workingDays", shift.getWorkingDays(),
                "siteId", shift.getSiteId(), "timezone", shift.getTimezone()));
        fields.put("reason", shift.getLastChangeReason());
        return fields;
    }

    private void validate(UUID tenantId, UUID siteId, String timezone, LocalTime start, LocalTime end) {
        WorkSite site = sites.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, siteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Active work site required"));
        if (!site.getTimezone().equals(WorkSiteService.timezone(timezone)) || !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Shift must use site timezone and end after start on the same day");
        }
    }

    private ShiftRecordV2 record(Shift shift) {
        return new ShiftRecordV2(shift.getId(), shift.getName(), shift.getStartTime(),
                shift.getEndTime(), shift.getGraceMinutes(), shift.getWorkingDays(),
                shift.getSiteId(), shift.getTimezone(), shift.getVersion(), shift.getLastChangeReason());
    }
}
