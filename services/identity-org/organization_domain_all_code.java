// ============================================================================
// ORGANIZATION DOMAIN MODULE - COMPLETE 54-FILE BUNDLE
// Package: com.hrflow.identityorg.organization
// ============================================================================


// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/controller/CatalogController.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.NamedCreateRequest;
import com.hrflow.identityorg.organization.dto.NamedRecord;
import com.hrflow.identityorg.organization.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
public class CatalogController {
    private final CatalogService catalogs;

    @GetMapping("/departments")
    public Map<String, Object> departments(@AuthenticationPrincipal Jwt jwt) {
        return page(catalogs.departments(TenantIdentity.tenantId(jwt)));
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public NamedRecord createDepartment(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody NamedCreateRequest request) {
        return catalogs.createDepartment(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
    }

    @GetMapping("/positions")
    public Map<String, Object> positions(@AuthenticationPrincipal Jwt jwt) {
        return page(catalogs.positions(TenantIdentity.tenantId(jwt)));
    }

    @PostMapping("/positions")
    @ResponseStatus(HttpStatus.CREATED)
    public NamedRecord createPosition(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody NamedCreateRequest request) {
        return catalogs.createPosition(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
    }

    private Map<String, Object> page(List<NamedRecord> rows) {
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/controller/EmployeeController.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.EmployeeCreateRequest;
import com.hrflow.identityorg.organization.dto.EmployeePageV2;
import com.hrflow.identityorg.organization.dto.EmployeePatchRequest;
import com.hrflow.identityorg.organization.dto.EmployeeRecordV2;
import com.hrflow.identityorg.organization.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
public class EmployeeController {
    private final EmployeeService employees;

    @GetMapping
    public EmployeePageV2 list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return employees.list(TenantIdentity.tenantId(jwt), page, size, search);
    }

    @PostMapping
    public ResponseEntity<EmployeeRecordV2> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeRecordV2 row = employees.create(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key,
                request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeRecordV2> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        EmployeeRecordV2 row = employees.get(TenantIdentity.tenantId(jwt), id);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeRecordV2> patch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody EmployeePatchRequest request) {
        EmployeeRecordV2 row = employees.patch(TenantIdentity.tenantId(jwt), id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    private String etag(EmployeeRecordV2 row) {
        return '"' + Long.toString(row.version()) + '"';
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/controller/HolidayController.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.HolidayInput;
import com.hrflow.identityorg.organization.dto.HolidayPatch;
import com.hrflow.identityorg.organization.dto.HolidayRecordV2;
import com.hrflow.identityorg.organization.service.HolidayService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {
    private final HolidayService holidays;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt) {
        var rows = holidays.list(TenantIdentity.tenantId(jwt));
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<HolidayRecordV2> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody HolidayInput request) {
        var row = holidays.create(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        holidays.delete(TenantIdentity.tenantId(jwt), id, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<HolidayRecordV2> patch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody HolidayPatch request) {
        var row = holidays.patch(TenantIdentity.tenantId(jwt), id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    private String etag(HolidayRecordV2 row) { return '"' + Long.toString(row.version()) + '"'; }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/controller/OrganizationProblems.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.controller;

import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {CatalogController.class, EmployeeController.class})
public class OrganizationProblems {
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflict(DataIntegrityViolationException ignored) {
        return problem(HttpStatus.CONFLICT, "CONFLICT", "The organization record conflicts with current data.");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> stale(ObjectOptimisticLockingFailureException ignored) {
        return problem(HttpStatus.PRECONDITION_FAILED, "STALE_VERSION", "Reload the record and retry.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(URI.create("urn:hrflow:problem:" + code.toLowerCase().replace('_', '-')));
        body.setTitle(code);
        body.setProperty("code", code);
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(body);
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/controller/ShiftAssignmentController.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.ShiftAssignmentClose;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentInput;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentRecordV2;
import com.hrflow.identityorg.organization.service.ShiftAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ShiftAssignmentController {
    private final ShiftAssignmentService assignments;

    @GetMapping("/shift-assignments")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt) {
        return page(assignments.list(TenantIdentity.tenantId(jwt)));
    }

    @GetMapping("/employees/{id}/shifts")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> forEmployee(@AuthenticationPrincipal Jwt jwt,
            Authentication authentication, @PathVariable UUID id) {
        boolean admin = authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_OWNER".equals(authority.getAuthority()) || "ROLE_HR_ADMIN".equals(authority.getAuthority()));
        if (!admin && !id.toString().equals(jwt.getClaimAsString("employeeId"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return page(assignments.listForEmployee(TenantIdentity.tenantId(jwt), id));
    }

    @PostMapping("/employees/{id}/shifts")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<ShiftAssignmentRecordV2> assign(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody ShiftAssignmentInput request) {
        var row = assignments.assign(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), id, key, request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @PostMapping("/shift-assignments/{id}/close")
    @PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
    public ResponseEntity<ShiftAssignmentRecordV2> close(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ShiftAssignmentClose request) {
        var row = assignments.close(TenantIdentity.tenantId(jwt), id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    private Map<String, Object> page(List<ShiftAssignmentRecordV2> rows) {
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }

    private String etag(ShiftAssignmentRecordV2 row) { return '"' + Long.toString(row.version()) + '"'; }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/controller/ShiftController.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.ShiftInput;
import com.hrflow.identityorg.organization.dto.ShiftPatch;
import com.hrflow.identityorg.organization.dto.ShiftRecordV2;
import com.hrflow.identityorg.organization.service.ShiftService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
public class ShiftController {
    private final ShiftService shifts;

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt) {
        var rows = shifts.list(TenantIdentity.tenantId(jwt));
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }

    @PostMapping
    public ResponseEntity<ShiftRecordV2> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody ShiftInput request) {
        var row = shifts.create(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ShiftRecordV2> patch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ShiftPatch request) {
        var row = shifts.patch(TenantIdentity.tenantId(jwt), id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    private String etag(ShiftRecordV2 row) { return '"' + Long.toString(row.version()) + '"'; }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/controller/TenantIdentity.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.exception.AuthFailedException;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

final class TenantIdentity {
    private TenantIdentity() {}

    static UUID tenantId(Jwt jwt) {
        if (jwt == null) throw AuthFailedException.invalid("Invalid JWT");
        try {
            return UUID.fromString(jwt.getClaimAsString("tenantId"));
        } catch (RuntimeException invalid) {
            throw AuthFailedException.invalid("Invalid tenant claim");
        }
    }

    static UUID actorId(Jwt jwt) {
        if (jwt == null) throw AuthFailedException.invalid("Invalid JWT");
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException invalid) {
            throw AuthFailedException.invalid("Invalid subject claim");
        }
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/controller/WorkSiteController.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.controller;

import com.hrflow.identityorg.organization.dto.WorkSiteInput;
import com.hrflow.identityorg.organization.dto.WorkSitePatch;
import com.hrflow.identityorg.organization.dto.WorkSiteRecordV2;
import com.hrflow.identityorg.organization.service.WorkSiteService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/work-sites")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'HR_ADMIN')")
public class WorkSiteController {
    private final WorkSiteService sites;

    @GetMapping
    public Map<String, Object> list(@AuthenticationPrincipal Jwt jwt) {
        var rows = sites.list(TenantIdentity.tenantId(jwt));
        return Map.of("items", rows, "page", 0, "size", rows.size(), "total", rows.size());
    }

    @PostMapping
    public ResponseEntity<WorkSiteRecordV2> create(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody WorkSiteInput request) {
        var row = sites.create(TenantIdentity.tenantId(jwt), TenantIdentity.actorId(jwt), key, request);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(row)).body(row);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkSiteRecordV2> patch(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody WorkSitePatch request) {
        var row = sites.patch(TenantIdentity.tenantId(jwt), id, ifMatch, request);
        return ResponseEntity.ok().eTag(etag(row)).body(row);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        sites.delete(TenantIdentity.tenantId(jwt), id, ifMatch);
        return ResponseEntity.noContent().build();
    }

    private String etag(WorkSiteRecordV2 row) { return '"' + Long.toString(row.version()) + '"'; }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/EmployeeCreateRequest.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.time.LocalDate;

public record EmployeeCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 64) String employeeNo,
        @NotNull UUID departmentId,
        @NotNull UUID positionId,
        UUID managerId,
        LocalDate hireDate) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/EmployeePageV2.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import java.util.List;

public record EmployeePageV2(List<EmployeeRecordV2> items, int page, int size, long total) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/EmployeePatchRequest.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** PATCH needs to distinguish an omitted manager from an explicit null. */
@Getter
@Setter
public class EmployeePatchRequest {
    @Size(min = 2, max = 120)
    private String name;
    @Email
    @Size(max = 254)
    private String email;
    @Size(min = 1, max = 64)
    private String employeeNo;
    private UUID departmentId;
    private UUID positionId;
    private UUID managerId;
    private LocalDate hireDate;
    @Setter(lombok.AccessLevel.NONE)
    private boolean managerIdPresent;

    public void setManagerId(UUID managerId) {
        this.managerId = managerId;
        this.managerIdPresent = true;
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/EmployeeRecordV2.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import java.util.UUID;
import java.time.LocalDate;

public record EmployeeRecordV2(
        UUID id, String name, String email, String employeeNo,
        UUID departmentId, UUID positionId, UUID managerId,
        String department, String position, String status, LocalDate hireDate, long version) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/HolidayInput.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayInput(@NotBlank @Size(min = 2, max = 120) String name,
        @NotNull LocalDate date, @NotNull Kind kind) {
    public enum Kind { PUBLIC, COMPANY }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/HolidayPatch.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayPatch(@Size(min = 2, max = 120) String name,
        LocalDate date, HolidayInput.Kind kind) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/HolidayRecordV2.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import java.time.LocalDate;
import java.util.UUID;

public record HolidayRecordV2(UUID id, String name, LocalDate date, String kind, long version) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/NamedCreateRequest.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NamedCreateRequest(@NotBlank @Size(max = 120) String name,
                                 @Size(max = 64) String code) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/NamedRecord.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import java.util.UUID;

public record NamedRecord(UUID id, String name, String code, long version) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/ShiftAssignmentClose.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ShiftAssignmentClose(@NotNull LocalDate validTo) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/ShiftAssignmentInput.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ShiftAssignmentInput(@NotNull UUID shiftId, @NotNull LocalDate validFrom,
        LocalDate validTo) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/ShiftAssignmentRecordV2.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record ShiftAssignmentRecordV2(UUID id, UUID employeeId, UUID shiftId,
        LocalDate validFrom, LocalDate validTo, Map<String, Object> shiftSnapshot, long version) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/ShiftInput.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftInput(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Min(0) @Max(120) Integer graceMinutes,
        @NotNull @Min(1) @Max(127) Integer workingDays,
        @NotNull UUID siteId,
        @NotBlank String timezone,
        @Size(max = 2000) String reason) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/ShiftPatch.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftPatch(
        @Size(min = 2, max = 120) String name,
        LocalTime startTime,
        LocalTime endTime,
        @Min(0) @Max(120) Integer graceMinutes,
        @Min(1) @Max(127) Integer workingDays,
        UUID siteId,
        String timezone,
        @Size(max = 2000) String reason) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/ShiftRecordV2.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import java.time.LocalTime;
import java.util.UUID;

public record ShiftRecordV2(UUID id, String name, LocalTime startTime, LocalTime endTime,
        int graceMinutes, int workingDays, UUID siteId, String timezone, long version,
        String lastChangeReason) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/WorkSiteInput.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record WorkSiteInput(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @NotNull @Min(1) @Max(10000) Integer radiusM,
        @NotBlank String timezone) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/WorkSitePatch.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record WorkSitePatch(
        @Size(min = 2, max = 120) String name,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Min(1) @Max(10000) Integer radiusM,
        String timezone) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/dto/WorkSiteRecordV2.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkSiteRecordV2(UUID id, String name, BigDecimal latitude, BigDecimal longitude,
        int radiusM, String timezone, long version) {}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/entity/Department.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "department", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID tenantId;
    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @Version
    private long version;

    public Department(UUID tenantId, String code, String name) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/entity/Employee.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID tenantId;
    @Column(nullable = false)
    private String employeeCode;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private UUID departmentId;
    @Column(nullable = false)
    private UUID positionId;
    private UUID managerId;
    private LocalDate hireDate;
    @Column(nullable = false)
    private String status;
    @Version
    private long version;

    public Employee(UUID tenantId, String employeeCode, String name, String email,
            UUID departmentId, UUID positionId, UUID managerId, LocalDate hireDate) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.employeeCode = employeeCode;
        this.name = name;
        this.email = email;
        this.departmentId = departmentId;
        this.positionId = positionId;
        this.managerId = managerId;
        this.hireDate = hireDate;
        this.status = "ACTIVE";
    }

    public void update(String name, String email, String employeeCode,
            UUID departmentId, UUID positionId, UUID managerId, boolean managerIdPresent, LocalDate hireDate) {
        if (name != null) this.name = name;
        if (email != null) this.email = email;
        if (employeeCode != null) this.employeeCode = employeeCode;
        if (departmentId != null) this.departmentId = departmentId;
        if (positionId != null) this.positionId = positionId;
        if (managerIdPresent) this.managerId = managerId;
        if (hireDate != null) this.hireDate = hireDate;
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/entity/EmployeeShift.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "employee_shift", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeShift {
    @Id private UUID id;
    private UUID tenantId;
    private UUID employeeId;
    private UUID shiftId;
    private LocalDate validFrom;
    private LocalDate validTo;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> shiftSnapshot;
    @Version private long version;

    public EmployeeShift(UUID tenantId, UUID employeeId, UUID shiftId, LocalDate validFrom,
            LocalDate validTo, Map<String, Object> shiftSnapshot) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.employeeId = employeeId;
        this.shiftId = shiftId;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.shiftSnapshot = Map.copyOf(shiftSnapshot);
    }

    public void close(LocalDate date) { this.validTo = date; }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/entity/Holiday.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "holiday", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holiday {
    @Id private UUID id;
    private UUID tenantId;
    private LocalDate date;
    private String name;
    private String kind;
    private Instant deletedAt;
    @Version private long version;

    public Holiday(UUID tenantId, LocalDate date, String name, String kind) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.date = date;
        this.name = name;
        this.kind = kind;
    }

    public void delete(Instant when) { this.deletedAt = when; }

    public void update(String name, LocalDate date, String kind) {
        if (name != null) this.name = name;
        if (date != null) this.date = date;
        if (kind != null) this.kind = kind;
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/entity/Position.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "position", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID tenantId;
    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @Version
    private long version;

    public Position(UUID tenantId, String code, String name) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/entity/Shift.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shift", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shift {
    @Id private UUID id;
    private UUID tenantId;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private int graceMinutes;
    private int workingDays;
    private UUID siteId;
    private String timezone;
    private Instant deletedAt;
    private String lastChangeReason;
    @Version private long version;

    public Shift(UUID tenantId, String name, LocalTime startTime, LocalTime endTime,
            int graceMinutes, int workingDays, UUID siteId, String timezone, String reason) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        update(name, startTime, endTime, graceMinutes, workingDays, siteId, timezone);
        this.lastChangeReason = reason;
    }

    public void update(String name, LocalTime startTime, LocalTime endTime,
            Integer graceMinutes, Integer workingDays, UUID siteId, String timezone) {
        if (name != null) this.name = name;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (graceMinutes != null) this.graceMinutes = graceMinutes;
        if (workingDays != null) this.workingDays = workingDays;
        if (siteId != null) this.siteId = siteId;
        if (timezone != null) this.timezone = timezone;
    }

    public void recordReason(String reason) {
        if (reason != null) this.lastChangeReason = reason;
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/entity/WorkSite.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_site", schema = "identity_org")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkSite {
    @Id private UUID id;
    private UUID tenantId;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    @Column(name = "radius_m") private int radiusM;
    private String timezone;
    private Instant deletedAt;
    @Version private long version;

    public WorkSite(UUID tenantId, String name, BigDecimal latitude, BigDecimal longitude,
            int radiusM, String timezone) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        update(name, latitude, longitude, radiusM, timezone);
    }

    public void update(String name, BigDecimal latitude, BigDecimal longitude, Integer radiusM, String timezone) {
        if (name != null) this.name = name;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
        if (radiusM != null) this.radiusM = radiusM;
        if (timezone != null) this.timezone = timezone;
    }

    public void delete(Instant when) { this.deletedAt = when; }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/repository/DepartmentRepository.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Department;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByTenantIdAndId(UUID tenantId, UUID id);
    List<Department> findByTenantIdAndIdIn(UUID tenantId, Set<UUID> ids);
    List<Department> findByTenantIdOrderByNameAsc(UUID tenantId);
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/repository/EmployeeRepository.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Employee;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByTenantIdAndId(UUID tenantId, UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select employee from Employee employee where employee.tenantId = :tenantId and employee.id = :id")
    Optional<Employee> lockByTenantIdAndId(@Param("tenantId") UUID tenantId, @Param("id") UUID id);
    Page<Employee> findByTenantId(UUID tenantId, Pageable page);
    @Query("""
            select employee from Employee employee
            where employee.tenantId = :tenantId and
                (lower(employee.name) like concat('%', :search, '%')
                or lower(employee.email) like concat('%', :search, '%')
                or lower(employee.employeeCode) like concat('%', :search, '%'))
            """)
    Page<Employee> search(@Param("tenantId") UUID tenantId,
                          @Param("search") String search, Pageable page);
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/repository/EmployeeShiftRepository.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.EmployeeShift;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, UUID> {
    List<EmployeeShift> findByTenantIdAndEmployeeIdOrderByValidFromDesc(UUID tenantId, UUID employeeId);
    List<EmployeeShift> findByTenantIdOrderByValidFromDesc(UUID tenantId);
    Optional<EmployeeShift> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("""
            select row from EmployeeShift row
            where row.tenantId = :tenant and row.employeeId = :employee
              and row.validFrom <= :end and (row.validTo is null or row.validTo >= :start)
            """)
    List<EmployeeShift> overlaps(@Param("tenant") UUID tenant, @Param("employee") UUID employee,
            @Param("start") LocalDate start, @Param("end") LocalDate end);
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/repository/HolidayRepository.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Holiday;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, UUID> {
    List<Holiday> findByTenantIdAndDeletedAtIsNullOrderByDateAsc(UUID tenantId);
    Optional<Holiday> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/repository/PositionRepository.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Position;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    Optional<Position> findByTenantIdAndId(UUID tenantId, UUID id);
    List<Position> findByTenantIdAndIdIn(UUID tenantId, Set<UUID> ids);
    List<Position> findByTenantIdOrderByNameAsc(UUID tenantId);
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/repository/ShiftRepository.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.Shift;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    List<Shift> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);
    Optional<Shift> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);
    boolean existsByTenantIdAndSiteIdAndDeletedAtIsNull(UUID tenantId, UUID siteId);
    List<Shift> findByTenantIdAndSiteIdAndDeletedAtIsNull(UUID tenantId, UUID siteId);
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/repository/WorkSiteRepository.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.repository;

import com.hrflow.identityorg.organization.entity.WorkSite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkSiteRepository extends JpaRepository<WorkSite, UUID> {
    List<WorkSite> findByTenantIdAndDeletedAtIsNullOrderByNameAsc(UUID tenantId);
    Optional<WorkSite> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/CatalogService.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.dto.NamedCreateRequest;
import com.hrflow.identityorg.organization.dto.NamedRecord;
import com.hrflow.identityorg.organization.entity.Department;
import com.hrflow.identityorg.organization.entity.Position;
import com.hrflow.identityorg.organization.repository.DepartmentRepository;
import com.hrflow.identityorg.organization.repository.PositionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final CommandReceipts receipts;
    private final TenantSqlScope tenantSqlScope;

    @Transactional(readOnly = true)
    public List<NamedRecord> departments(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return departments.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(row -> new NamedRecord(row.getId(), row.getName(), row.getCode(), row.getVersion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NamedRecord> positions(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return positions.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(row -> new NamedRecord(row.getId(), row.getName(), row.getCode(), row.getVersion()))
                .toList();
    }

    @Transactional
    public NamedRecord createDepartment(UUID tenantId, UUID actorId, String key, NamedCreateRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/departments", key, request, NamedRecord.class, () -> {
            Department row = departments.saveAndFlush(new Department(tenantId, code(request), request.name().strip()));
            return new NamedRecord(row.getId(), row.getName(), row.getCode(), row.getVersion());
        });
    }

    @Transactional
    public NamedRecord createPosition(UUID tenantId, UUID actorId, String key, NamedCreateRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/positions", key, request, NamedRecord.class, () -> {
            Position row = positions.saveAndFlush(new Position(tenantId, code(request), request.name().strip()));
            return new NamedRecord(row.getId(), row.getName(), row.getCode(), row.getVersion());
        });
    }

    private String code(NamedCreateRequest request) {
        return request.code() == null || request.code().isBlank()
                ? UUID.randomUUID().toString() : request.code().strip();
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/CommandReceipts.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class CommandReceipts {
    private final EntityManager entityManager;
    private final JsonMapper json;

    /** Reserve and settle a command in the same transaction as its owner-row mutation. */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> T execute(UUID tenantId, UUID actorId, String path, String keyText,
            Object request, Class<T> responseType, Supplier<T> action) {
        UUID key = key(keyText);
        String hash = hash(json.writeValueAsString(request));
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO identity_org.command_receipt
                    (tenant_id, actor_id, method, path, command_key, request_hash)
                VALUES (:tenant, :actor, 'POST', :path, :key, :hash)
                ON CONFLICT DO NOTHING
                """)
                .setParameter("tenant", tenantId).setParameter("actor", actorId)
                .setParameter("path", path).setParameter("key", key)
                .setParameter("hash", hash).executeUpdate();
        if (inserted == 0) {
            Object[] stored = (Object[]) entityManager.createNativeQuery("""
                    SELECT request_hash, response_body FROM identity_org.command_receipt
                    WHERE tenant_id = :tenant AND actor_id = :actor AND method = 'POST'
                      AND path = :path AND command_key = :key FOR UPDATE
                    """)
                    .setParameter("tenant", tenantId).setParameter("actor", actorId)
                    .setParameter("path", path).setParameter("key", key).getSingleResult();
            if (!hash.equals(stored[0]) || stored[1] == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key has different content");
            }
            return json.readValue((String) stored[1], responseType);
        }
        T response = action.get();
        entityManager.createNativeQuery("""
                UPDATE identity_org.command_receipt SET response_body = :body
                WHERE tenant_id = :tenant AND actor_id = :actor AND method = 'POST'
                  AND path = :path AND command_key = :key
                """)
                .setParameter("body", json.writeValueAsString(response))
                .setParameter("tenant", tenantId).setParameter("actor", actorId)
                .setParameter("path", path).setParameter("key", key).executeUpdate();
        return response;
    }

    private UUID key(String text) {
        try {
            UUID key = UUID.fromString(text);
            if (key.version() == 4) return key;
        } catch (RuntimeException ignored) {
            // Missing and invalid command keys share the same required-header response.
        }
        throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "UUID v4 Idempotency-Key required");
    }

    private String hash(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/EmployeeService.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.dto.EmployeeCreateRequest;
import com.hrflow.identityorg.organization.dto.EmployeePageV2;
import com.hrflow.identityorg.organization.dto.EmployeePatchRequest;
import com.hrflow.identityorg.organization.dto.EmployeeRecordV2;
import com.hrflow.identityorg.organization.entity.Employee;
import com.hrflow.identityorg.organization.repository.DepartmentRepository;
import com.hrflow.identityorg.organization.repository.EmployeeRepository;
import com.hrflow.identityorg.organization.repository.PositionRepository;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final CommandReceipts receipts;
    private final TenantSqlScope tenantSqlScope;
    private final OrganizationFacts facts;

    @Transactional(readOnly = true)
    public EmployeePageV2 list(UUID tenantId, int page, int size, String search) {
        tenantSqlScope.use(tenantId);
        if (page < 0 || size < 1 || size > 100) throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
        if (search != null && search.length() > 120) throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
        String term = search == null || search.isBlank() ? null : search.strip().toLowerCase(Locale.ROOT);
        PageRequest paging = PageRequest.of(page, size);
        Page<Employee> rows = term == null ? employees.findByTenantId(tenantId, paging)
                : employees.search(tenantId, term, paging);
        var content = rows.getContent();
        Set<UUID> departmentIds = content.stream().map(Employee::getDepartmentId).collect(Collectors.toSet());
        Set<UUID> positionIds = content.stream().map(Employee::getPositionId).collect(Collectors.toSet());
        Map<UUID, String> departmentNames = departmentIds.isEmpty() ? Map.of()
                : departments.findByTenantIdAndIdIn(tenantId, departmentIds).stream()
                        .collect(Collectors.toMap(row -> row.getId(), row -> row.getName()));
        Map<UUID, String> positionNames = positionIds.isEmpty() ? Map.of()
                : positions.findByTenantIdAndIdIn(tenantId, positionIds).stream()
                        .collect(Collectors.toMap(row -> row.getId(), row -> row.getName()));
        return new EmployeePageV2(content.stream().map(row -> record(row,
                departmentNames.get(row.getDepartmentId()), positionNames.get(row.getPositionId())))
                .toList(), page, size, rows.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EmployeeRecordV2 get(UUID tenantId, UUID id) {
        tenantSqlScope.use(tenantId);
        return record(find(tenantId, id));
    }

    @Transactional
    public EmployeeRecordV2 create(UUID tenantId, UUID actorId, String key, EmployeeCreateRequest request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/employees", key, request,
                EmployeeRecordV2.class, () -> createNew(tenantId, request));
    }

    private EmployeeRecordV2 createNew(UUID tenantId, EmployeeCreateRequest request) {
        validateReferences(tenantId, request.departmentId(), request.positionId(), request.managerId());
        Employee row = new Employee(tenantId, request.employeeNo().strip(), request.name().strip(),
                request.email().strip().toLowerCase(Locale.ROOT), request.departmentId(),
                request.positionId(), request.managerId(), request.hireDate());
        Employee saved = employees.saveAndFlush(row);
        facts.employee(saved, "org.employee.created.v1");
        return record(saved);
    }

    @Transactional
    public EmployeeRecordV2 patch(UUID tenantId, UUID id, String ifMatch, EmployeePatchRequest request) {
        tenantSqlScope.use(tenantId);
        Employee row = find(tenantId, id);
        RevisionPreconditions.require(ifMatch, row.getVersion());
        UUID departmentId = request.getDepartmentId() == null ? row.getDepartmentId() : request.getDepartmentId();
        UUID positionId = request.getPositionId() == null ? row.getPositionId() : request.getPositionId();
        UUID managerId = request.isManagerIdPresent() ? request.getManagerId() : row.getManagerId();
        validateReferences(tenantId, departmentId, positionId, managerId);
        row.update(strip(request.getName()), normalizeEmail(request.getEmail()), strip(request.getEmployeeNo()),
                request.getDepartmentId(), request.getPositionId(), managerId,
                request.isManagerIdPresent(), request.getHireDate());
        Employee saved = employees.saveAndFlush(row);
        facts.employee(saved, "org.employee.updated.v1");
        return record(saved);
    }

    private Employee find(UUID tenantId, UUID id) {
        return employees.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND));
    }

    private void validateReferences(UUID tenantId, UUID departmentId, UUID positionId, UUID managerId) {
        if (departments.findByTenantIdAndId(tenantId, departmentId).isEmpty()
                || positions.findByTenantIdAndId(tenantId, positionId).isEmpty()
                || managerId != null && employees.findByTenantIdAndId(tenantId, managerId).isEmpty()) {
            throw problem(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private EmployeeRecordV2 record(Employee row) {
        String department = departments.findByTenantIdAndId(row.getTenantId(), row.getDepartmentId())
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND)).getName();
        String position = positions.findByTenantIdAndId(row.getTenantId(), row.getPositionId())
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND)).getName();
        return record(row, department, position);
    }

    private EmployeeRecordV2 record(Employee row, String department, String position) {
        if (department == null || position == null) throw problem(HttpStatus.NOT_FOUND);
        return new EmployeeRecordV2(row.getId(), row.getName(), row.getEmail(), row.getEmployeeCode(),
                row.getDepartmentId(), row.getPositionId(), row.getManagerId(),
                department, position, row.getStatus(), row.getHireDate(), row.getVersion());
    }

    private String strip(String value) {
        return value == null ? null : value.strip();
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException problem(HttpStatus status) {
        return new ResponseStatusException(status);
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/HolidayService.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.dto.HolidayInput;
import com.hrflow.identityorg.organization.dto.HolidayPatch;
import com.hrflow.identityorg.organization.dto.HolidayRecordV2;
import com.hrflow.identityorg.organization.entity.Holiday;
import com.hrflow.identityorg.organization.repository.HolidayRepository;
import java.time.Instant;
import java.util.List;
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
                    return record(saved);
                });
    }

    @Transactional
    public HolidayRecordV2 patch(UUID tenantId, UUID id, String ifMatch, HolidayPatch request) {
        tenantSqlScope.use(tenantId);
        Holiday row = holidays.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        RevisionPreconditions.require(ifMatch, row.getVersion());
        row.update(request.name() == null ? null : request.name().strip(), request.date(),
                request.kind() == null ? null : request.kind().name());
        Holiday saved = holidays.saveAndFlush(row);
        facts.holiday(saved, "org.holiday.updated.v1");
        return record(saved);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id, String ifMatch) {
        tenantSqlScope.use(tenantId);
        Holiday row = holidays.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        RevisionPreconditions.require(ifMatch, row.getVersion());
        row.delete(Instant.now());
        facts.holiday(holidays.saveAndFlush(row), "org.holiday.removed.v1");
    }

    private HolidayRecordV2 record(Holiday row) {
        return new HolidayRecordV2(row.getId(), row.getName(), row.getDate(), row.getKind(), row.getVersion());
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/OrganizationFactMapper.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.contracts.organization.OrganizationFact;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Only allowlisted owner snapshots cross the outbox-to-Avro boundary. */
@Component
@RequiredArgsConstructor
public class OrganizationFactMapper {
    private static final Map<String, Set<String>> TYPES = Map.of(
            "org.employee.v1", Set.of("org.employee.created.v1", "org.employee.updated.v1",
                    "org.employee.terminated.v1"),
            "org.shift.v1", Set.of("org.shift.defined.v1", "org.shift.updated.v1",
                    "org.shift.removed.v1"),
            "org.holiday.v1", Set.of("org.holiday.added.v1", "org.holiday.updated.v1",
                    "org.holiday.removed.v1"));

    private final JsonMapper json;

    public OrganizationFact map(OutboxEntry row) {
        if (!TYPES.getOrDefault(row.topic(), Set.of()).contains(row.type())
                || row.revision() < 1) {
            throw new IllegalArgumentException("Unknown organization topic/type or revision");
        }
        Object snapshot = switch (row.topic()) {
            case "org.employee.v1" -> employee(row.payload(), row.aggregateId());
            case "org.shift.v1" -> shift(row.payload(), row.aggregateId());
            case "org.holiday.v1" -> holiday(row.payload(), row.aggregateId());
            default -> throw new IllegalArgumentException("Unknown organization topic");
        };
        return new OrganizationFact(row.id().toString(), row.type(), 1,
                row.tenantId().toString(), row.aggregateId().toString(), row.revision(),
                row.occurredAt().toString(), row.correlationId().toString(),
                row.causationId() == null ? null : row.causationId().toString(), snapshot);
    }

    private Object employee(String payload, UUID aggregateId) {
        var value = json.readValue(payload, OrganizationSnapshots.Employee.class);
        matchId(value.employeeId(), aggregateId);
        return value.avro();
    }

    private Object shift(String payload, UUID aggregateId) {
        var value = json.readValue(payload, OrganizationSnapshots.Shift.class);
        matchId(value.shiftId(), aggregateId);
        return value.avro();
    }

    private Object holiday(String payload, UUID aggregateId) {
        var value = json.readValue(payload, OrganizationSnapshots.Holiday.class);
        matchId(value.holidayId(), aggregateId);
        return value.avro();
    }

    private void matchId(String value, UUID aggregateId) {
        if (value == null || !aggregateId.equals(UUID.fromString(value))) {
            throw new IllegalArgumentException("Payload aggregate does not match outbox key");
        }
    }

    public record OutboxEntry(UUID id, UUID tenantId, String topic, UUID aggregateId,
            long revision, String type, UUID correlationId, UUID causationId,
            String payload, Instant occurredAt) {}
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/OrganizationFacts.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.entity.Employee;
import com.hrflow.identityorg.organization.entity.Holiday;
import com.hrflow.identityorg.organization.entity.Shift;
import com.hrflow.identityorg.organization.entity.WorkSite;
import com.hrflow.identityorg.organization.repository.DepartmentRepository;
import com.hrflow.identityorg.organization.repository.EmployeeShiftRepository;
import com.hrflow.identityorg.organization.repository.PositionRepository;
import com.hrflow.identityorg.organization.repository.WorkSiteRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrganizationFacts {
    private final OrganizationOutbox outbox;
    private final DepartmentRepository departments;
    private final PositionRepository positions;
    private final EmployeeShiftRepository assignments;
    private final WorkSiteRepository sites;

    @Transactional(propagation = Propagation.MANDATORY)
    public void employee(Employee employee, String type) {
        var department = departments.findByTenantIdAndId(employee.getTenantId(), employee.getDepartmentId())
                .orElseThrow();
        var position = positions.findByTenantIdAndId(employee.getTenantId(), employee.getPositionId())
                .orElseThrow();
        var currentAssignments = assignments.findByTenantIdAndEmployeeIdOrderByValidFromDesc(
                employee.getTenantId(), employee.getId()).stream()
                .map(assignment -> new OrganizationSnapshots.Assignment(
                        assignment.getId().toString(), assignment.getShiftId().toString(),
                        assignment.getValidFrom().toString(),
                        assignment.getValidTo() == null ? null : assignment.getValidTo().toString()))
                .toList();
        var payload = new OrganizationSnapshots.Employee(employee.getId().toString(), null,
                employee.getEmployeeCode(), employee.getName(), employee.getEmail(),
                employee.getDepartmentId().toString(), department.getName(),
                employee.getPositionId().toString(), position.getName(),
                employee.getManagerId() == null ? null : employee.getManagerId().toString(),
                employee.getHireDate() == null ? null : employee.getHireDate().toString(),
                null, employee.getStatus(), null, null, currentAssignments, null);
        outbox.append(employee.getTenantId(), "org.employee.v1", employee.getId(), type, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void shift(Shift shift, String type) {
        WorkSite site = sites.findByTenantIdAndIdAndDeletedAtIsNull(shift.getTenantId(), shift.getSiteId())
                .orElseThrow();
        var sitePayload = new OrganizationSnapshots.Site(site.getId().toString(), site.getName(),
                site.getLatitude(), site.getLongitude(), BigDecimal.valueOf(site.getRadiusM()),
                site.getTimezone(), null, null);
        var payload = new OrganizationSnapshots.Shift(shift.getId().toString(), shift.getName(),
                shift.getStartTime().toString(), shift.getEndTime().toString(), shift.getTimezone(),
                BigDecimal.valueOf(shift.getGraceMinutes()), BigDecimal.valueOf(shift.getWorkingDays()),
                sitePayload, shift.getDeletedAt() == null ? null : shift.getDeletedAt().toString());
        outbox.append(shift.getTenantId(), "org.shift.v1", shift.getId(), type, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void holiday(Holiday holiday, String type) {
        var payload = new OrganizationSnapshots.Holiday(holiday.getId().toString(),
                holiday.getDate().toString(), holiday.getName(), holiday.getKind(),
                holiday.getDeletedAt() == null ? null : holiday.getDeletedAt().toString());
        outbox.append(holiday.getTenantId(), "org.holiday.v1", holiday.getId(), type, payload);
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/OrganizationOutbox.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OrganizationOutbox {
    private final EntityManager entityManager;
    private final JsonMapper json;

    /** Append the owner fact in the same transaction as the aggregate change. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(UUID tenantId, String topic, UUID aggregateId, String type,
            OrganizationSnapshots.Snapshot payload) {
        Number revision = (Number) entityManager.createNativeQuery("""
                INSERT INTO identity_org.organization_revision(tenant_id, topic, aggregate_id, revision)
                VALUES (:tenant, :topic, :aggregate, 1)
                ON CONFLICT (tenant_id, topic, aggregate_id)
                DO UPDATE SET revision = identity_org.organization_revision.revision + 1
                RETURNING revision
                """)
                .setParameter("tenant", tenantId).setParameter("topic", topic)
                .setParameter("aggregate", aggregateId).getSingleResult();
        entityManager.createNativeQuery("""
                INSERT INTO identity_org.organization_outbox
                    (id, tenant_id, topic, aggregate_id, aggregate_revision, event_type,
                     correlation_id, payload, occurred_at)
                VALUES (:id, :tenant, :topic, :aggregate, :revision, :type,
                        :correlation, cast(:payload as jsonb), :occurred)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("tenant", tenantId).setParameter("topic", topic)
                .setParameter("aggregate", aggregateId).setParameter("revision", revision.longValue())
                .setParameter("type", type).setParameter("correlation", UUID.randomUUID())
                .setParameter("payload", json.writeValueAsString(payload))
                .setParameter("occurred", Instant.now()).executeUpdate();
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/OrganizationOutboxRelay.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.service.OrganizationFactMapper.OutboxEntry;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** One active relay; the dedicated job connection owns leadership and UUID-only discovery. */
@Slf4j
@Component
@ConditionalOnProperty(name = "hrflow.organization.relay.enabled", havingValue = "true")
public class OrganizationOutboxRelay {
    private static final int LEADER_LOCK = 73104217;
    private final String jdbcUrl;
    private final String jobUser;
    private final String jobPassword;
    private final TransactionTemplate transactions;
    private final TenantSqlScope scope;
    private final EntityManager entityManager;
    private final OrganizationFactMapper mapper;
    private final KafkaTemplate<Object, Object> kafka;
    private Connection leader;
    private UUID lastTenant;

    public OrganizationOutboxRelay(@Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${hrflow.organization.relay.job-user:}") String jobUser,
            @Value("${hrflow.organization.relay.job-password:}") String jobPassword,
            TransactionTemplate transactions, TenantSqlScope scope, EntityManager entityManager,
            OrganizationFactMapper mapper, KafkaTemplate<Object, Object> kafka) {
        if (jobUser.isBlank() || jobPassword.isBlank()) {
            throw new IllegalStateException("Organization relay requires a private job database role");
        }
        this.jdbcUrl = jdbcUrl;
        this.jobUser = jobUser;
        this.jobPassword = jobPassword;
        this.transactions = transactions;
        this.scope = scope;
        this.entityManager = entityManager;
        this.mapper = mapper;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${hrflow.organization.relay.poll-ms:250}",
            initialDelayString = "${hrflow.organization.relay.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            if (!lead()) {
                log.debug("Organization relay standby");
                return;
            }
            UUID tenant = nextTenant();
            if (tenant == null) {
                log.debug("Organization relay has no registered tenant");
                return;
            }
            transactions.executeWithoutResult(status -> publishOne(tenant));
        } catch (Exception error) {
            log.warn("Organization relay stopped this poll; pending fact remains retryable", error);
            closeLeader();
        }
    }

    private boolean lead() throws SQLException {
        if (leader != null && leader.isValid(2)) return true;
        closeLeader();
        Connection candidate = DriverManager.getConnection(jdbcUrl, jobUser, jobPassword);
        try (var query = candidate.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            query.setInt(1, LEADER_LOCK);
            try (var rows = query.executeQuery()) {
                rows.next();
                if (!rows.getBoolean(1)) {
                    candidate.close();
                    return false;
                }
            }
        } catch (SQLException error) {
            candidate.close();
            throw error;
        }
        leader = candidate;
        return true;
    }

    private UUID nextTenant() throws SQLException {
        UUID next = findTenantAfter(lastTenant);
        if (next == null) next = findTenantAfter(null);
        lastTenant = next;
        return next;
    }

    private UUID findTenantAfter(UUID cursor) throws SQLException {
        String sql = """
                SELECT tenant_id FROM identity_jobs.organization_tenant_registry
                WHERE lifecycle = 'ACTIVE' AND (?::uuid IS NULL OR tenant_id > ?::uuid)
                ORDER BY tenant_id LIMIT 1
                """;
        try (PreparedStatement query = leader.prepareStatement(sql)) {
            query.setObject(1, cursor);
            query.setObject(2, cursor);
            try (ResultSet rows = query.executeQuery()) {
                return rows.next() ? rows.getObject(1, UUID.class) : null;
            }
        }
    }

    private void publishOne(UUID tenant) {
        scope.use(tenant);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT id, tenant_id, topic, aggregate_id, aggregate_revision, event_type,
                       correlation_id, causation_id, payload::text, occurred_at
                FROM identity_org.organization_outbox current_fact
                WHERE tenant_id = :tenant AND published_at IS NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM identity_org.organization_outbox earlier
                    WHERE earlier.tenant_id = current_fact.tenant_id
                      AND earlier.topic = current_fact.topic
                      AND earlier.aggregate_id = current_fact.aggregate_id
                      AND earlier.aggregate_revision < current_fact.aggregate_revision
                      AND earlier.published_at IS NULL)
                ORDER BY occurred_at, id LIMIT 1 FOR UPDATE OF current_fact SKIP LOCKED
                """).setParameter("tenant", tenant).getResultList();
        if (rows.isEmpty()) {
            log.debug("Organization relay found no pending row for registered tenant");
            return;
        }
        Object[] value = rows.getFirst();
        OutboxEntry fact = new OutboxEntry((UUID) value[0], (UUID) value[1], (String) value[2],
                (UUID) value[3], ((Number) value[4]).longValue(), (String) value[5],
                (UUID) value[6], (UUID) value[7], (String) value[8],
                instant(value[9]));
        try {
            kafka.send(fact.topic(), fact.tenantId() + ":" + fact.aggregateId(), mapper.map(fact))
                    .get(3, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new IllegalStateException("Broker did not acknowledge organization fact", error);
        }
        entityManager.createNativeQuery("""
                UPDATE identity_org.organization_outbox
                SET published_at = now(), delivery_attempts = delivery_attempts + 1
                WHERE tenant_id = :tenant AND id = :id AND published_at IS NULL
                """).setParameter("tenant", tenant).setParameter("id", fact.id()).executeUpdate();
    }

    private Instant instant(Object value) {
        if (value instanceof OffsetDateTime dateTime) return dateTime.toInstant();
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof Instant result) return result;
        throw new IllegalArgumentException("Outbox timestamp has an unsupported type");
    }

    @PreDestroy
    public synchronized void closeLeader() {
        if (leader == null) return;
        try {
            leader.close();
        } catch (SQLException ignored) {
            log.warn("Organization relay leader connection failed to close");
        } finally {
            leader = null;
        }
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/OrganizationSnapshots.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.contracts.organization.AssignmentSnapshot;
import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.ShiftSnapshot;
import com.hrflow.contracts.organization.SiteSnapshot;
import java.math.BigDecimal;
import java.util.List;

/** Typed JSON persisted in the owner outbox; Avro is created at publication. */
public final class OrganizationSnapshots {
    private OrganizationSnapshots() {}

    public sealed interface Snapshot permits Employee, Shift, Holiday {}

    public record Assignment(String assignmentId, String shiftId, String validFrom, String validTo) {
        AssignmentSnapshot avro() {
            return new AssignmentSnapshot(required(assignmentId), required(shiftId),
                    required(validFrom), validTo);
        }
    }

    public record Employee(String employeeId, String userId, String employeeNo, String fullName,
            String email, String departmentId, String departmentName, String positionId,
            String position, String managerId, String hireDate, String terminatedOn,
            String status, String deviceId, String locale, List<Assignment> assignments,
            String deletedAt) implements Snapshot {
        EmployeeSnapshot avro() {
            if (assignments == null) throw new IllegalArgumentException("Assignments required");
            return new EmployeeSnapshot(required(employeeId), userId, required(employeeNo),
                    required(fullName), email, departmentId, departmentName, positionId,
                    position, managerId, hireDate, terminatedOn, required(status), deviceId,
                    locale, assignments.stream().map(Assignment::avro).toList(), deletedAt);
        }
    }

    public record Site(String siteId, String name, BigDecimal latitude, BigDecimal longitude,
            BigDecimal radiusM, String timezone, String wifiSsid, String wifiBssid) {
        SiteSnapshot avro() {
            return new SiteSnapshot(required(siteId), required(name), finite(latitude),
                    finite(longitude), finite(radiusM), required(timezone), wifiSsid, wifiBssid);
        }
    }

    public record Shift(String shiftId, String name, String startTime, String endTime,
            String timezone, BigDecimal graceMinutes, BigDecimal workingDays,
            Site site, String deletedAt) implements Snapshot {
        ShiftSnapshot avro() {
            if (site == null) throw new IllegalArgumentException("Site required");
            return new ShiftSnapshot(required(shiftId), required(name), required(startTime),
                    required(endTime), required(timezone), integer(graceMinutes),
                    integer(workingDays), site.avro(), deletedAt);
        }
    }

    public record Holiday(String holidayId, String date, String name, String kind,
            String deletedAt) implements Snapshot {
        HolidaySnapshot avro() {
            return new HolidaySnapshot(required(holidayId), required(date), required(name),
                    required(kind), deletedAt);
        }
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Required organization field missing");
        return value;
    }

    private static int integer(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Required organization integer missing");
        try {
            return value.intValueExact();
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("Invalid organization integer", error);
        }
    }

    private static double finite(BigDecimal value) {
        if (value == null || !Double.isFinite(value.doubleValue())) {
            throw new IllegalArgumentException("Invalid organization number");
        }
        return value.doubleValue();
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/RevisionPreconditions.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** All owner updates compare the opaque quoted version sent by the client. */
final class RevisionPreconditions {
    private RevisionPreconditions() {}

    static void require(String ifMatch, long version) {
        if (ifMatch == null) throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED);
        if (!ifMatch.equals('"' + Long.toString(version) + '"')) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED);
        }
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/ShiftAssignmentService.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.dto.ShiftAssignmentClose;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentInput;
import com.hrflow.identityorg.organization.dto.ShiftAssignmentRecordV2;
import com.hrflow.identityorg.organization.entity.Employee;
import com.hrflow.identityorg.organization.entity.EmployeeShift;
import com.hrflow.identityorg.organization.entity.Shift;
import com.hrflow.identityorg.organization.entity.WorkSite;
import com.hrflow.identityorg.organization.repository.EmployeeRepository;
import com.hrflow.identityorg.organization.repository.EmployeeShiftRepository;
import com.hrflow.identityorg.organization.repository.ShiftRepository;
import com.hrflow.identityorg.organization.repository.WorkSiteRepository;
import java.time.LocalDate;
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
public class ShiftAssignmentService {
    private final EmployeeRepository employees;
    private final ShiftRepository shifts;
    private final WorkSiteRepository sites;
    private final EmployeeShiftRepository assignments;
    private final TenantSqlScope tenantSqlScope;
    private final CommandReceipts receipts;
    private final OrganizationFacts facts;

    @Transactional(readOnly = true)
    public List<ShiftAssignmentRecordV2> list(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return assignments.findByTenantIdOrderByValidFromDesc(tenantId).stream().map(this::record).toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftAssignmentRecordV2> listForEmployee(UUID tenantId, UUID employeeId) {
        tenantSqlScope.use(tenantId);
        employees.findByTenantIdAndId(tenantId, employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return assignments.findByTenantIdAndEmployeeIdOrderByValidFromDesc(tenantId, employeeId)
                .stream().map(this::record).toList();
    }

    @Transactional
    public ShiftAssignmentRecordV2 assign(UUID tenantId, UUID actorId, UUID employeeId,
            String key, ShiftAssignmentInput request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/employees/" + employeeId + "/shifts", key,
                request, ShiftAssignmentRecordV2.class, () -> assignNew(tenantId, employeeId, request));
    }

    private ShiftAssignmentRecordV2 assignNew(UUID tenantId, UUID employeeId, ShiftAssignmentInput request) {
        Employee employee = employees.lockByTenantIdAndId(tenantId, employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is not active");
        }
        if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid effective period");
        }
        Shift shift = shifts.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, request.shiftId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Active shift required"));
        WorkSite site = sites.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, shift.getSiteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Active work site required"));
        LocalDate end = request.validTo() == null ? LocalDate.of(9999, 12, 31) : request.validTo();
        if (!assignments.overlaps(tenantId, employeeId, request.validFrom(), end).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shift assignment overlaps an existing period");
        }
        Map<String, Object> snapshot = Map.of(
                "name", shift.getName(), "startTime", shift.getStartTime().toString(),
                "endTime", shift.getEndTime().toString(), "graceMinutes", shift.getGraceMinutes(),
                "workingDays", shift.getWorkingDays(), "timezone", shift.getTimezone(),
                "site", Map.of("id", site.getId().toString(), "name", site.getName(),
                        "latitude", site.getLatitude(), "longitude", site.getLongitude(),
                        "radiusM", site.getRadiusM(), "timezone", site.getTimezone()));
        EmployeeShift saved = assignments.saveAndFlush(new EmployeeShift(tenantId, employeeId,
                shift.getId(), request.validFrom(), request.validTo(), snapshot));
        facts.employee(employee, "org.employee.updated.v1");
        return record(saved);
    }

    @Transactional
    public ShiftAssignmentRecordV2 close(UUID tenantId, UUID id, String ifMatch,
            ShiftAssignmentClose request) {
        tenantSqlScope.use(tenantId);
        EmployeeShift row = assignments.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Employee employee = employees.lockByTenantIdAndId(tenantId, row.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        RevisionPreconditions.require(ifMatch, row.getVersion());
        if (request.validTo().isBefore(row.getValidFrom())
                || row.getValidTo() != null && request.validTo().isAfter(row.getValidTo())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid close date");
        }
        row.close(request.validTo());
        EmployeeShift saved = assignments.saveAndFlush(row);
        facts.employee(employee, "org.employee.updated.v1");
        return record(saved);
    }

    private ShiftAssignmentRecordV2 record(EmployeeShift row) {
        return new ShiftAssignmentRecordV2(row.getId(), row.getEmployeeId(), row.getShiftId(),
                row.getValidFrom(), row.getValidTo(), row.getShiftSnapshot(), row.getVersion());
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/ShiftService.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import com.hrflow.identityorg.organization.dto.ShiftInput;
import com.hrflow.identityorg.organization.dto.ShiftPatch;
import com.hrflow.identityorg.organization.dto.ShiftRecordV2;
import com.hrflow.identityorg.organization.entity.Shift;
import com.hrflow.identityorg.organization.entity.WorkSite;
import com.hrflow.identityorg.organization.repository.ShiftRepository;
import com.hrflow.identityorg.organization.repository.WorkSiteRepository;
import java.time.LocalTime;
import java.util.List;
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
                    return record(saved);
                });
    }

    @Transactional
    public ShiftRecordV2 patch(UUID tenantId, UUID id, String ifMatch, ShiftPatch request) {
        tenantSqlScope.use(tenantId);
        Shift shift = shifts.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        RevisionPreconditions.require(ifMatch, shift.getVersion());
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
        return record(saved);
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

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/TenantSqlScope.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Binds the organization tenant to the current database transaction only. */
@Component
@RequiredArgsConstructor
public class TenantSqlScope {
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void use(UUID tenantId) {
        entityManager.createNativeQuery("SELECT set_config('app.tenant_id', :tenant, true)")
                .setParameter("tenant", tenantId.toString()).getSingleResult();
    }
}

// ----------------------------------------------------------------------------
// FILE: com/hrflow/identityorg/organization/service/WorkSiteService.java
// ----------------------------------------------------------------------------

package com.hrflow.identityorg.organization.service;

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

    @Transactional(readOnly = true)
    public List<WorkSiteRecordV2> list(UUID tenantId) {
        tenantSqlScope.use(tenantId);
        return sites.findByTenantIdAndDeletedAtIsNullOrderByNameAsc(tenantId).stream().map(this::record).toList();
    }

    @Transactional
    public WorkSiteRecordV2 create(UUID tenantId, UUID actorId, String key, WorkSiteInput request) {
        tenantSqlScope.use(tenantId);
        return receipts.execute(tenantId, actorId, "/work-sites", key, request,
                WorkSiteRecordV2.class, () -> record(sites.saveAndFlush(new WorkSite(tenantId,
                        request.name().strip(), request.latitude(), request.longitude(),
                        request.radiusM(), timezone(request.timezone())))));
    }

    @Transactional
    public WorkSiteRecordV2 patch(UUID tenantId, UUID id, String ifMatch, WorkSitePatch request) {
        tenantSqlScope.use(tenantId);
        WorkSite site = find(tenantId, id);
        RevisionPreconditions.require(ifMatch, site.getVersion());
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
        return record(saved);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id, String ifMatch) {
        tenantSqlScope.use(tenantId);
        WorkSite site = find(tenantId, id);
        RevisionPreconditions.require(ifMatch, site.getVersion());
        if (shifts.existsByTenantIdAndSiteIdAndDeletedAtIsNull(tenantId, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Work site has active shifts");
        }
        site.delete(Instant.now());
        sites.saveAndFlush(site);
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
