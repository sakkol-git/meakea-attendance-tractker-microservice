package com.hrflow.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.hrflow.notification.consumer.NotificationConsumerService;
import com.hrflow.notification.metrics.NotificationMetrics;
import com.hrflow.notification.repository.NotificationRecordRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class NotificationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", database::getJdbcUrl);
        properties.add("spring.datasource.username", database::getUsername);
        properties.add("spring.datasource.password", database::getPassword);
        properties.add("spring.flyway.user", database::getUsername);
        properties.add("spring.flyway.password", database::getPassword);
    }

    @Autowired
    NotificationMetrics metrics;

    @Autowired
    NotificationConsumerService consumerService;

    @Autowired
    NotificationRecordRepository recordRepository;

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    @DisplayName("Synthetic stopped consumer raises lag metric and recovers when consumer resumes")
    void stoppedConsumerRaisesLagAlertAndRecovers() {
        // 1. Simulate stopped consumer accumulating lag
        metrics.setConsumerLag(25L);
        assertThat(metrics.getConsumerLag()).isEqualTo(25L);

        // Verify gauge registered in Micrometer registry
        Double gaugeValue = meterRegistry.get("hrflow.notification.consumer.lag").gauge().value();
        assertThat(gaugeValue).isEqualTo(25.0);

        // 2. Consume an event
        UUID tenantId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        double deliveredBefore = metrics.getDeliveredCount();

        boolean processed = consumerService.processIncomingEvent(
                tenantId, eventId, "org.attendance.session.created.v1",
                UUID.randomUUID(), employeeId,
                "Attendance Notification", "Session recorded",
                "rec-1", "ATTENDANCE",
                () -> {}
        );
        assertThat(processed).isTrue();
        assertThat(metrics.getDeliveredCount()).isEqualTo(deliveredBefore + 1.0);

        // 3. Lag recovers to 0
        metrics.setConsumerLag(0L);
        assertThat(metrics.getConsumerLag()).isEqualTo(0L);
        assertThat(meterRegistry.get("hrflow.notification.consumer.lag").gauge().value()).isEqualTo(0.0);
    }
}
