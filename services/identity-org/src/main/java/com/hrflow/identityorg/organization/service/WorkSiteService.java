package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.audit.IdentityAuditService;
import com.hrflow.identityorg.organization.dto.WorkSiteInput;
import com.hrflow.identityorg.organization.dto.WorkSitePatch;
import com.hrflow.identityorg.organization.dto.WorkSiteRecordV2;
import com.hrflow.identityorg.organization.entity.WorkSite;
import com.hrflow.identityorg.organization.repository.ShiftRepository;
import com.hrflow.identityorg.organization.repository.WorkSiteRepository;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
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
public class WorkSiteService {
    private final WorkSiteRepository sites;
    private final ShiftRepository shifts;
    private final TenantSqlScope tenantSqlScope;
    private final CommandReceipts receipts;
    private final OrganizationFacts facts;
    private final IdentityAuditService audit;

    @Transactional(readOnly = true)
    public List<WorkSiteRecordV2> list(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return sites.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId).stream().map(this::record).toList();
    }

    @Transactional
    public WorkSiteRecordV2 create(UUID tenantId, UUID actorId, String key, WorkSiteInput request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/work-sites", key, request,
                WorkSiteRecordV2.class, () -> {
                    WorkSite saved = sites.saveAndFlush(new WorkSite(tenantId,
                        request.name().strip(), request.latitude(), request.longitude(),
                        request.radiusM(), timezone(request.timezone())));
                    audit.recordChanges(tenantId, actorId, "WORK_SITE_CREATED", "work_site", saved.getId(),
                            Map.of(), auditFields(saved));
                    return record(saved);
                });
    }

    @Transactional
    public WorkSiteRecordV2 patch(UUID tenantId, UUID actorId, UUID id, String ifMatch, WorkSitePatch request) {
        tenantSqlScope.use(tenantId);
        WorkSite site = find(tenantId, id);
        RevisionPreconditions.require(ifMatch, site.getVersion());
        var before = auditFields(site);
        String zone = request.timezone() == null ? null : timezone(request.timezone());
        if (zone != null && !zone.equals(site.getTimezone())
                && shifts.existsByTenantIdAndSiteIdAndDeletedAtIsNull(tenantId, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Existing shifts retain site timezone");
        }
        site.update(request.name() == null ? null : request.name().strip(),
                request.latitude(), request.longitude(), request.radiusM(), zone);
        WorkSite saved = sites.saveAndFlush(site);
        shifts.findByTenantIdAndSiteIdAndDeletedAtIsNull(tenantId, id)
                .forEach(shift -> facts.shift(shift, "org.shift.updated.v1"));
        audit.recordChanges(tenantId, actorId, "WORK_SITE_UPDATED", "work_site", id, before, auditFields(saved));
        return record(saved);
    }

    @Transactional
    public void delete(UUID tenantId, UUID actorId, UUID id, String ifMatch) {
        tenantSqlScope.use(tenantId);
        WorkSite site = find(tenantId, id);
        RevisionPreconditions.require(ifMatch, site.getVersion());
        if (shifts.existsByTenantIdAndSiteIdAndDeletedAtIsNull(tenantId, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work site has active shifts");
        }
        site.delete(Instant.now());
        sites.saveAndFlush(site);
        audit.record(tenantId, actorId, "WORK_SITE_DELETED", "work_site", id,
                Map.of("deletedAt", Map.of("after", site.getDeletedAt().toString())));
    }

    private Map<String, Object> auditFields(WorkSite site) {
        return Map.of("name", site.getName(), "latitude", site.getLatitude(), "longitude", site.getLongitude(),
                "radiusM", site.getRadiusM(), "timezone", site.getTimezone());
    }

    private WorkSite find(UUID tenantId, UUID id) {
        return sites.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private WorkSiteRecordV2 record(WorkSite site) {
        return new WorkSiteRecordV2(site.getId(), site.getName(), site.getLatitude(), site.getLongitude(),
                site.getRadiusM(), site.getTimezone(), site.getVersion());
    }

    static String timezone(String value) {
        try {
            return ZoneId.of(value).getId();
        } catch (DateTimeException | NullPointerException invalid) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Valid timezone required");
        }
    }

}
