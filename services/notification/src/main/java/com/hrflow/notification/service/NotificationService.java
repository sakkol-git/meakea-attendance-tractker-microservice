package com.hrflow.notification.service;

import com.hrflow.notification.dto.NoticeResponse;
import com.hrflow.notification.dto.NotificationPageResponse;
import com.hrflow.notification.dto.NotificationPreferencesResponse;
import com.hrflow.notification.dto.UnreadCountResponse;
import com.hrflow.notification.dto.UpdatePreferencesRequest;
import com.hrflow.notification.entity.NotificationPreference;
import com.hrflow.notification.entity.NotificationPreferenceId;
import com.hrflow.notification.entity.NotificationRecord;
import com.hrflow.notification.mapper.NoticeMapper;
import com.hrflow.notification.mapper.PreferenceMapper;
import com.hrflow.notification.repository.NotificationPreferenceRepository;
import com.hrflow.notification.repository.NotificationRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRecordRepository recordRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NoticeMapper noticeMapper;
    private final PreferenceMapper preferenceMapper;
    private final TenantSqlScope scope;

    @Transactional(readOnly = true)
    public NotificationPageResponse listNotifications(UUID tenantId, UUID employeeId, int page, int size) {
        scope.use(tenantId);
        Page<NotificationRecord> result = recordRepository.findByTenantIdAndRecipientIdOrderByCreatedAtDesc(
                tenantId, employeeId, PageRequest.of(page, size)
        );
        List<NoticeResponse> items = result.getContent().stream()
                .map(noticeMapper::toResponse)
                .toList();
        return new NotificationPageResponse(items, page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID tenantId, UUID employeeId) {
        scope.use(tenantId);
        long count = recordRepository.countByTenantIdAndRecipientIdAndReadFalse(tenantId, employeeId);
        return new UnreadCountResponse(count, count);
    }

    public NoticeResponse markRead(UUID tenantId, UUID employeeId, UUID id) {
        scope.use(tenantId);
        NotificationRecord record = recordRepository.findByIdAndTenantIdAndRecipientId(id, tenantId, employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: " + id));
        record.markRead();
        NotificationRecord saved = recordRepository.save(record);
        return noticeMapper.toResponse(saved);
    }

    public int markAllRead(UUID tenantId, UUID employeeId) {
        scope.use(tenantId);
        return recordRepository.markAllReadForRecipient(tenantId, employeeId);
    }

    public NotificationPreferencesResponse getPreferences(UUID tenantId, UUID employeeId) {
        scope.use(tenantId);
        NotificationPreference pref = findOrCreatePreference(tenantId, employeeId);
        return preferenceMapper.toResponse(pref);
    }

    public NotificationPreferencesResponse updatePreferences(UUID tenantId,
                                                             UUID employeeId,
                                                             UpdatePreferencesRequest request,
                                                             String ifMatch) {
        scope.use(tenantId);
        NotificationPreference pref = findOrCreatePreference(tenantId, employeeId);

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "If-Match header is required");
        }
        String expected = Long.toString(pref.getVersion());
        if (!ifMatch.trim().equals(expected) && !ifMatch.trim().equals("\"" + expected + "\"")) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Preference revision does not match");
        }

        pref.setEmail(request.resolvedEmail(pref.isEmail()));
        pref.setInApp(request.resolvedInApp(pref.isInApp()));
        pref.setReminders(request.resolvedReminders(pref.isReminders()));
        pref.setDigestFrequency(request.resolvedDigest(pref.getDigestFrequency()));
        pref.setUpdatedAt(Instant.now());

        NotificationPreference saved = preferenceRepository.saveAndFlush(pref);
        return preferenceMapper.toResponse(saved);
    }

    public NotificationPreference findOrCreatePreference(UUID tenantId, UUID employeeId) {
        scope.use(tenantId);
        scope.lockPreferences(tenantId, employeeId);
        NotificationPreferenceId id = new NotificationPreferenceId(tenantId, employeeId);
        return preferenceRepository.findById(id).orElseGet(() -> {
            NotificationPreference created = new NotificationPreference(tenantId, employeeId);
            return preferenceRepository.save(created);
        });
    }
}
