package com.hrflow.request.controller;

import com.hrflow.request.dto.RequestListQuery;
import com.hrflow.request.service.RequestPages;
import com.hrflow.request.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class RequestListsController {
    private final RequestService requests;
    private final RequestPages pages;

    @GetMapping("/requests/me")
    public ResponseEntity<?> own(@AuthenticationPrincipal Jwt jwt, RequestListQuery query) {
        var tenant = RequestController.tenantId(jwt);
        var employee = RequestController.employeeId(jwt);
        return ResponseEntity.ok(query.page() == null ? requests.getMyRequests(tenant, employee)
                : pages.requests(tenant, employee, RequestController.roles(jwt), false, query));
    }

    @GetMapping("/requests/inbox")
    public ResponseEntity<?> inbox(@AuthenticationPrincipal Jwt jwt, RequestListQuery query) {
        var tenant = RequestController.tenantId(jwt);
        var employee = RequestController.employeeId(jwt);
        var roles = RequestController.roles(jwt);
        return ResponseEntity.ok(query.page() == null ? requests.getInbox(tenant, employee, roles)
                : pages.requests(tenant, employee, roles, true, query));
    }

    @GetMapping("/requests/types")
    public ResponseEntity<?> types(@AuthenticationPrincipal Jwt jwt, RequestListQuery query) {
        var tenant = RequestController.tenantId(jwt);
        return ResponseEntity.ok(query.page() == null ? requests.listTypes(tenant) : pages.types(tenant, query));
    }

    @GetMapping("/materials")
    public ResponseEntity<?> materials(@AuthenticationPrincipal Jwt jwt, RequestListQuery query) {
        var tenant = RequestController.tenantId(jwt);
        return ResponseEntity.ok(query.page() == null ? requests.listMaterials(tenant) : pages.materials(tenant, query));
    }
}
