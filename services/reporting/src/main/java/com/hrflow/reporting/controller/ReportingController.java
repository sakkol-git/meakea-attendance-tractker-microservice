package com.hrflow.reporting.controller;

import com.hrflow.reporting.dto.AttendanceFactResponse;
import com.hrflow.reporting.dto.AttendanceSummaryResponse;
import com.hrflow.reporting.dto.CreateExportRequest;
import com.hrflow.reporting.dto.DashboardKpiResponse;
import com.hrflow.reporting.dto.ExportJobListResponse;
import com.hrflow.reporting.dto.ExportJobResponse;
import com.hrflow.reporting.entity.AttendanceFactProjection;
import com.hrflow.reporting.entity.ExportJob;
import com.hrflow.reporting.mapper.ReportingMapper;
import com.hrflow.reporting.service.CsvExportService;
import com.hrflow.reporting.service.ReportingDashboardService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingDashboardService dashboardService;
    private final CsvExportService exportService;
    private final ReportingMapper reportingMapper;
    private final com.hrflow.reporting.service.PrivateExportDownload downloads;
    private final com.hrflow.reporting.service.ExportQueue exportQueue;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<DashboardKpiResponse> getDashboard(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(dashboardService.getDashboardKpis(tenantId, managerScope(jwt)));
    }

    @GetMapping("/attendance/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<AttendanceSummaryResponse> getAttendanceSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sortBy,
            @AuthenticationPrincipal Jwt jwt) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from date must not be after to date");
        }
        if (sortBy != null && !ReportingDashboardService.ALLOWED_SORTS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field: " + sortBy);
        }
        UUID tenantId = extractTenantId(jwt);
        try {
            List<AttendanceFactProjection> facts = dashboardService.getAttendanceReport(tenantId, managerScope(jwt), from, to, sortBy);
            List<AttendanceFactResponse> records = reportingMapper.toFactResponses(facts);
            return ResponseEntity.ok(new AttendanceSummaryResponse(from, to, records.size(), records));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field");
        }
    }

    @PostMapping("/exports")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ExportJobResponse> createExport(
            @Valid @RequestBody CreateExportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (request.from() != null && request.to() != null && request.from().isAfter(request.to())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from date must not be after to date");
        }
        UUID tenantId = extractTenantId(jwt);
        UUID requesterId = extractActorId(jwt);
        ExportJob job = exportQueue.create(tenantId, requesterId, request.report(), request.from(), request.to());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(reportingMapper.toResponse(job));
    }

    @GetMapping("/exports")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ExportJobListResponse> listExports(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        List<ExportJob> jobs = exportService.listExportJobs(tenantId, extractActorId(jwt));
        List<ExportJobResponse> response = jobs.stream()
                .map(reportingMapper::toResponse)
                .toList();
        return ResponseEntity.ok(new ExportJobListResponse(response, response.size()));
    }

    @GetMapping("/exports/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ExportJobResponse> getExportStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        ExportJob job = exportService.getExportJob(tenantId, id, extractActorId(jwt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Export job not found: " + id));
        return ResponseEntity.ok(reportingMapper.toResponse(job));
    }

    private static UUID managerScope(Jwt jwt) {
        var roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("HR_ADMIN") ? null : extractActorId(jwt);
    }

    @GetMapping("/exports/{id}/download")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadExport(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        var download = downloads.open(extractTenantId(jwt), extractActorId(jwt), id);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", org.springframework.http.ContentDisposition.attachment()
                        .filename(download.name()).build().toString())
                .contentLength(download.length())
                .body(new org.springframework.core.io.InputStreamResource(download.stream()));
    }

    private static UUID extractTenantId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }
        String tenantClaim = jwt.getClaimAsString("tenantId");
        if (tenantClaim == null || tenantClaim.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing tenantId claim in token");
        }
        return identityUuid(tenantClaim);
    }

    private static UUID extractActorId(Jwt jwt) {
        String empClaim = jwt.getClaimAsString("employeeId");
        if (empClaim != null && !empClaim.isBlank()) {
            return identityUuid(empClaim);
        }
        return identityUuid(jwt.getSubject());
    }

    private static UUID identityUuid(String claim) {
        try {
            return UUID.fromString(claim);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid identity claim");
        }
    }
}
