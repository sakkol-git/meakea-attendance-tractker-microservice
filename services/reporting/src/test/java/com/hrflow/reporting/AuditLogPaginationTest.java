package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrflow.reporting.dto.AuditLogEntry;
import com.hrflow.reporting.repository.AuditLogRepository;
import com.hrflow.reporting.service.AuditLogService;
import com.hrflow.reporting.service.TenantSqlScope;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AuditLogPaginationTest {

    private AuditLogRepository auditRepo;
    private TenantSqlScope tenantScope;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditRepo = mock(AuditLogRepository.class);
        tenantScope = mock(TenantSqlScope.class);
        auditLogService = new AuditLogService(auditRepo, tenantScope);
    }

    @Test
    @DisplayName("RPT-26: Rejects page below zero with canonical 400 Bad Request")
    void rejectsPageBelowZero() {
        UUID tenantId = UUID.randomUUID();
        assertThatThrownBy(() -> auditLogService.search(tenantId, "LEAVE", "APPROVE", -1, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Page index must not be less than zero");
                });
    }

    @Test
    @DisplayName("RPT-26: Rejects size below one with canonical 400 Bad Request")
    void rejectsSizeBelowOne() {
        UUID tenantId = UUID.randomUUID();
        assertThatThrownBy(() -> auditLogService.search(tenantId, "LEAVE", "APPROVE", 0, 0))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Page size must be at least one");
                });

        assertThatThrownBy(() -> auditLogService.search(tenantId, "LEAVE", "APPROVE", 0, -10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("RPT-26: Rejects oversized size with canonical 400 Bad Request")
    void rejectsOversizedSize() {
        UUID tenantId = UUID.randomUUID();
        assertThatThrownBy(() -> auditLogService.search(tenantId, "LEAVE", "APPROVE", 0, 101))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Page size must not exceed 100");
                });

        assertThatThrownBy(() -> auditLogService.search(tenantId, "LEAVE", "APPROVE", 0, 500))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("RPT-26: Valid maximum values and valid boundary values succeed")
    void validMaximumValuesSucceed() {
        UUID tenantId = UUID.randomUUID();
        when(auditRepo.searchLogs(eq(tenantId), eq("LEAVE"), eq("APPROVE"), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        // Maximum size: 100
        List<AuditLogEntry> resultMax = auditLogService.search(tenantId, "LEAVE", "APPROVE", 0, 100);
        assertThat(resultMax).isEmpty();
        verify(auditRepo).searchLogs(tenantId, "LEAVE", "APPROVE", PageRequest.of(0, 100));

        // Boundary size: 1
        List<AuditLogEntry> resultMin = auditLogService.search(tenantId, "LEAVE", "APPROVE", 5, 1);
        assertThat(resultMin).isEmpty();
        verify(auditRepo).searchLogs(tenantId, "LEAVE", "APPROVE", PageRequest.of(5, 1));
    }
}
