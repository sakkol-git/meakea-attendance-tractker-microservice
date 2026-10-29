package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.reporting.consumer.ReportingProjectionConsumer;
import com.hrflow.reporting.dto.AttendanceFactEvent;
import com.hrflow.reporting.dto.LeaveFactEvent;
import com.hrflow.reporting.dto.RequestFactEvent;
import com.hrflow.reporting.entity.AttendanceFactProjection;
import com.hrflow.reporting.entity.LeaveFactProjection;
import com.hrflow.reporting.entity.RequestFactProjection;
import com.hrflow.reporting.repository.AttendanceFactProjectionRepository;
import com.hrflow.reporting.repository.LeaveFactProjectionRepository;
import com.hrflow.reporting.repository.RequestFactProjectionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class AtomicProjectionRevisionOrderingTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    ReportingProjectionConsumer projectionConsumer;

    @Autowired
    AttendanceFactProjectionRepository attendanceRepo;

    @Autowired
    LeaveFactProjectionRepository leaveRepo;

    @Autowired
    RequestFactProjectionRepository requestRepo;

    @Test
    @DisplayName("Concurrent N+1 and N+2 deliveries must atomically leave the complete N+2 snapshot")
    void concurrentDeliveriesLeaveCompleteLatestSnapshot() throws Exception {
        UUID tenantId = UUID.randomUUID();
        int concurrentCases = 20;

        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            List<UUID> sessionIds = new ArrayList<>();
            List<UUID> leaveIds = new ArrayList<>();
            List<UUID> requestIds = new ArrayList<>();

            // 1. Seed initial revision 1 (N = 1) facts
            for (int i = 0; i < concurrentCases; i++) {
                UUID sessionId = UUID.randomUUID();
                sessionIds.add(sessionId);
                projectionConsumer.processAttendanceFact(new AttendanceFactEvent(
                        UUID.randomUUID(), tenantId, sessionId, UUID.randomUUID(),
                        LocalDate.of(2026, 10, 28), "PRESENT", 0, 480, 1L));

                UUID leaveId = UUID.randomUUID();
                leaveIds.add(leaveId);
                projectionConsumer.processLeaveFact(new LeaveFactEvent(
                        UUID.randomUUID(), tenantId, leaveId, UUID.randomUUID(),
                        "ANNUAL", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2),
                        new BigDecimal("2.00"), "SUBMITTED", 1L));

                UUID requestId = UUID.randomUUID();
                requestIds.add(requestId);
                projectionConsumer.processRequestFact(new RequestFactEvent(
                        UUID.randomUUID(), tenantId, requestId, UUID.randomUUID(),
                        "EQUIPMENT", "SUBMITTED", 1L));
            }

            // 2. Deliver concurrent N+1 (revision 2) and N+2 (revision 3)
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < concurrentCases; i++) {
                UUID sessionId = sessionIds.get(i);
                UUID leaveId = leaveIds.get(i);
                UUID requestId = requestIds.get(i);

                UUID empRev2 = UUID.randomUUID();
                UUID empRev3 = UUID.randomUUID();

                // Attendance N+1 and N+2
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        projectionConsumer.processAttendanceFact(new AttendanceFactEvent(
                                UUID.randomUUID(), tenantId, sessionId, empRev2,
                                LocalDate.of(2026, 10, 28), "LATE", 30, 450, 2L));
                    } catch (InterruptedException ignored) {}
                }));

                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        projectionConsumer.processAttendanceFact(new AttendanceFactEvent(
                                UUID.randomUUID(), tenantId, sessionId, empRev3,
                                LocalDate.of(2026, 10, 28), "PRESENT", 0, 500, 3L));
                    } catch (InterruptedException ignored) {}
                }));

                // Leave N+1 and N+2
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        projectionConsumer.processLeaveFact(new LeaveFactEvent(
                                UUID.randomUUID(), tenantId, leaveId, empRev2,
                                "SICK", LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 6),
                                new BigDecimal("1.00"), "CANCELLED", 2L));
                    } catch (InterruptedException ignored) {}
                }));

                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        projectionConsumer.processLeaveFact(new LeaveFactEvent(
                                UUID.randomUUID(), tenantId, leaveId, empRev3,
                                "ANNUAL", LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12),
                                new BigDecimal("3.00"), "APPROVED", 3L));
                    } catch (InterruptedException ignored) {}
                }));

                // Request N+1 and N+2
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        projectionConsumer.processRequestFact(new RequestFactEvent(
                                UUID.randomUUID(), tenantId, requestId, empRev2,
                                "HARDWARE", "REJECTED", 2L));
                    } catch (InterruptedException ignored) {}
                }));

                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        projectionConsumer.processRequestFact(new RequestFactEvent(
                                UUID.randomUUID(), tenantId, requestId, empRev3,
                                "SOFTWARE", "APPROVED", 3L));
                    } catch (InterruptedException ignored) {}
                }));
            }

            // Release all tasks at once
            startLatch.countDown();

            for (Future<?> f : futures) {
                f.get(15, TimeUnit.SECONDS);
            }

            // 3. Verify that every single projection has the complete N+2 (revision 3) snapshot
            for (int i = 0; i < concurrentCases; i++) {
                UUID sessionId = sessionIds.get(i);
                AttendanceFactProjection att = attendanceRepo.findByTenantIdAndSessionId(tenantId, sessionId).orElseThrow();
                assertThat(att.getAggregateRevision())
                        .as("Attendance %s aggregate revision must be N+2 (3L)", sessionId)
                        .isEqualTo(3L);
                assertThat(att.getStatus())
                        .as("Attendance %s status must be N+2 snapshot value", sessionId)
                        .isEqualTo("PRESENT");
                assertThat(att.getMinutesWorked())
                        .as("Attendance %s minutesWorked must be N+2 snapshot value", sessionId)
                        .isEqualTo(500);

                UUID leaveId = leaveIds.get(i);
                LeaveFactProjection leave = leaveRepo.findByTenantIdAndLeaveId(tenantId, leaveId).orElseThrow();
                assertThat(leave.getAggregateRevision())
                        .as("Leave %s aggregate revision must be N+2 (3L)", leaveId)
                        .isEqualTo(3L);
                assertThat(leave.getStatus())
                        .as("Leave %s status must be N+2 snapshot value", leaveId)
                        .isEqualTo("APPROVED");
                assertThat(leave.getLeaveType())
                        .as("Leave %s leaveType must be N+2 snapshot value", leaveId)
                        .isEqualTo("ANNUAL");

                UUID requestId = requestIds.get(i);
                RequestFactProjection req = requestRepo.findByTenantIdAndRequestId(tenantId, requestId).orElseThrow();
                assertThat(req.getAggregateRevision())
                        .as("Request %s aggregate revision must be N+2 (3L)", requestId)
                        .isEqualTo(3L);
                assertThat(req.getStatus())
                        .as("Request %s status must be N+2 snapshot value", requestId)
                        .isEqualTo("APPROVED");
                assertThat(req.getRequestType())
                        .as("Request %s requestType must be N+2 snapshot value", requestId)
                        .isEqualTo("SOFTWARE");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
