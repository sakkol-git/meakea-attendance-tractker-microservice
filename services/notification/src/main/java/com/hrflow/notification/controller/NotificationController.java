package com.hrflow.notification.controller;

import com.hrflow.notification.dto.NoticeResponse;
import com.hrflow.notification.dto.NotificationPageResponse;
import com.hrflow.notification.dto.NotificationPreferencesResponse;
import com.hrflow.notification.dto.UnreadCountResponse;
import com.hrflow.notification.dto.UpdatePreferencesRequest;
import com.hrflow.notification.service.NotificationService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<NotificationPageResponse> listNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        return ResponseEntity.ok(service.listNotifications(tenantId, employeeId, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        return ResponseEntity.ok(service.getUnreadCount(tenantId, employeeId));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NoticeResponse> markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        return ResponseEntity.ok(service.markRead(tenantId, employeeId, id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        int marked = service.markAllRead(tenantId, employeeId);
        return ResponseEntity.ok(Map.of("success", true, "marked", marked));
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        NotificationPreferencesResponse pref = service.getPreferences(tenantId, employeeId);
        return ResponseEntity.ok()
                .eTag('"' + Long.toString(pref.version()) + '"')
                .body(pref);
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) UpdatePreferencesRequest request) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        UpdatePreferencesRequest body = request != null ? request : new UpdatePreferencesRequest(null, null, null, null, null, null);
        NotificationPreferencesResponse updated = service.updatePreferences(tenantId, employeeId, body, ifMatch);
        return ResponseEntity.ok()
                .eTag('"' + Long.toString(updated.version()) + '"')
                .body(updated);
    }

    private UUID tenantId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }
        String claim = jwt.getClaimAsString("tenantId");
        if (claim == null || claim.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing tenant claim");
        }
        return UUID.fromString(claim);
    }

    private UUID actorId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid actor subject");
        }
    }

    private UUID employeeId(Jwt jwt) {
        String claim = jwt.getClaimAsString("employeeId");
        if (claim != null && !claim.isBlank()) {
            return UUID.fromString(claim);
        }
        return actorId(jwt);
    }
}
