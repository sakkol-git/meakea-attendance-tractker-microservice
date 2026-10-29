package com.hrflow.attendance.controller;

import com.hrflow.attendance.dto.AttendanceSessionResponse;
import com.hrflow.attendance.dto.CheckInRequest;
import com.hrflow.attendance.service.AttendanceService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AttendanceController {
    private final AttendanceService service;

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceSessionResponse> checkIn(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) CheckInRequest request) {
        UUID tenantId = tenantId(jwt);
        UUID actorId = actorId(jwt);
        UUID employeeId = employeeId(jwt);
        CheckInRequest body = request != null ? request : new CheckInRequest(null, null, null, null);
        AttendanceSessionResponse session = service.checkIn(tenantId, actorId, employeeId, idempotencyKey, body);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(session)).body(session);
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceSessionResponse> checkOut(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestBody(required = false) CheckInRequest request) {
        UUID tenantId = tenantId(jwt);
        UUID actorId = actorId(jwt);
        UUID employeeId = employeeId(jwt);
        CheckInRequest body = request != null ? request : new CheckInRequest(null, null, null, null);
        AttendanceSessionResponse session = service.checkOut(tenantId, actorId, employeeId, idempotencyKey, ifMatch, body);
        return ResponseEntity.ok().eTag(etag(session)).body(session);
    }

    @GetMapping("/today")
    public ResponseEntity<com.hrflow.attendance.dto.TodayAttendanceResponse> today(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        return ResponseEntity.ok(service.today(tenantId, employeeId));
    }

    @GetMapping("/daily-status/me")
    public ResponseEntity<com.hrflow.attendance.dto.DailyStatusResponse> dailyStatusMe(
            @AuthenticationPrincipal Jwt jwt,
            @org.springframework.web.bind.annotation.RequestParam(name = "date", required = false)
            java.time.LocalDate date) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        return ResponseEntity.ok(service.dailyStatusMe(tenantId, employeeId, date));
    }

    @GetMapping("/daily-status")
    public Map<String, Object> dailyStatusAll(
            @AuthenticationPrincipal Jwt jwt,
            @org.springframework.web.bind.annotation.RequestParam(name = "date", required = false)
            java.time.LocalDate date) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        com.hrflow.attendance.dto.DailyStatusResponse me = service.dailyStatusMe(tenantId, employeeId, date);
        return Map.of("items", List.of(me), "total", 1, "page", 0, "size", 1);
    }

    @GetMapping("/me")
    public Map<String, Object> mySessions(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = tenantId(jwt);
        UUID employeeId = employeeId(jwt);
        List<AttendanceSessionResponse> items = service.mySessions(tenantId, employeeId);
        return Map.of("items", items, "total", items.size(), "page", 0, "size", items.size());
    }

    private UUID tenantId(Jwt jwt) {
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

    private String etag(AttendanceSessionResponse response) {
        return '"' + Long.toString(response.version()) + '"';
    }
}
