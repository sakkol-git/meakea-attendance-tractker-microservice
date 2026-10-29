package com.hrflow.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.notification.consumer.NotificationConsumerService;
import com.hrflow.notification.service.NotificationService;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.ErrorResponseException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class RestrictedNotificationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "notification_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired NotificationConsumerService consumer;
    @Autowired NotificationService notices;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
    @Autowired com.hrflow.notification.metrics.NotificationMetrics metrics;

    @Test void concurrentPreferenceInitializationReturnsOneSharedDefaultRevision() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        var results = preferenceRace(() -> notices.getPreferences(tenant, employee));
        assertThat(results).extracting(com.hrflow.notification.dto.NotificationPreferencesResponse::version).containsExactly(0L, 0L);
    }

    @Test void simultaneousPreferenceEditsReturnSuccessAndStaleInsteadOfAnInternalError() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        notices.getPreferences(tenant, employee);
        var update = new com.hrflow.notification.dto.UpdatePreferencesRequest(false, null, null, null, null, null);
        var results = preferenceRace(() -> {
            try { notices.updatePreferences(tenant, employee, update, "\"0\""); return 200; }
            catch (ErrorResponseException rejected) { return rejected.getStatusCode().value(); }
        });
        assertThat(results).containsExactlyInAnyOrder(200, 412);
        assertThat(notices.getPreferences(tenant, employee).version()).isEqualTo(1);
    }

    private <T> java.util.List<T> preferenceRace(java.util.function.Supplier<T> operation) throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        var admin = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        try (var barrier = java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            barrier.setAutoCommit(false);
            barrier.createStatement().execute("LOCK TABLE notification.notification_preference IN SHARE MODE");
            var outcomes = new java.util.ArrayList<java.util.concurrent.Future<T>>();
            for (int i = 0; i < 2; i++) outcomes.add(executor.submit(() -> { start.await(); return operation.get(); }));
            start.countDown();
            try {
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).until(() -> admin.queryForObject(
                        "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'", Integer.class) >= 2);
            } finally { barrier.commit(); }
            return java.util.List.of(outcomes.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS), outcomes.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    @Test void preferenceMutationRequiresTheActualRevisionAndCannotAcceptMalformedQuotes() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        notices.getPreferences(tenant, employee);
        var update = new com.hrflow.notification.dto.UpdatePreferencesRequest(false, null, null, null, null, null);
        assertThatThrownBy(() -> notices.updatePreferences(tenant, employee, update, null))
                .isInstanceOfSatisfying(ErrorResponseException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(428));
        assertThatThrownBy(() -> notices.updatePreferences(tenant, employee, update, "\"0\"0\""))
                .isInstanceOfSatisfying(ErrorResponseException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(412));
        assertThat(notices.getPreferences(tenant, employee).version()).isZero();
        assertThat(notices.updatePreferences(tenant, employee, update, "\"0\"").email()).isFalse();
        assertThatThrownBy(() -> notices.updatePreferences(tenant, employee, update, "\"0\""))
                .isInstanceOfSatisfying(ErrorResponseException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(412));
    }

    @Test void simultaneousRedeliveryProducesOnePrivateNoticeAndTwoCommittedAcknowledgments() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), event = UUID.randomUUID(), aggregate = UUID.randomUUID();
        var acknowledgments = new java.util.concurrent.atomic.AtomicInteger();
        var start = new java.util.concurrent.CountDownLatch(1);
        var admin = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        try (var barrier = java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            barrier.setAutoCommit(false);
            barrier.createStatement().execute("LOCK TABLE notification.notification_inbox IN SHARE MODE");
            var outcomes = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 2; i++) outcomes.add(executor.submit(() -> {
                start.await();
                return consumer.processIncomingEvent(tenant, event, "synthetic-owner-case", aggregate, employee,
                        "Synthetic", "Private", aggregate.toString(), "REQUEST", acknowledgments::incrementAndGet);
            }));
            start.countDown();
            try {
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(5)).until(() -> admin.queryForObject(
                        "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'", Integer.class) >= 2);
            } finally { barrier.commit(); }
            assertThat(java.util.List.of(outcomes.get(0).get(10, java.util.concurrent.TimeUnit.SECONDS),
                    outcomes.get(1).get(10, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(acknowledgments.get()).isEqualTo(2);
        assertThat(notices.getUnreadCount(tenant, employee).count()).isEqualTo(1);
    }

    @Test void nestedDeliveryCannotAcknowledgeOrCountSuccessBeforeTheActualOuterCommit() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), aggregate = UUID.randomUUID();
        var ack = new AtomicBoolean();
        double before = metrics.getDeliveredCount();
        transactions.executeWithoutResult(tx -> {
            consumer.processIncomingEvent(tenant, UUID.randomUUID(), "synthetic-owner-case", aggregate, employee,
                    "Synthetic", "Private", aggregate.toString(), "REQUEST", () -> ack.set(true));
            tx.setRollbackOnly();
        });
        assertThat(ack).isFalse();
        assertThat(metrics.getDeliveredCount()).isEqualTo(before);
        assertThat(notices.getUnreadCount(tenant, employee).count()).isZero();
        transactions.executeWithoutResult(tx -> {
            consumer.processIncomingEvent(tenant, UUID.randomUUID(), "synthetic-owner-case", aggregate, employee,
                    "Synthetic", "Private", aggregate.toString(), "REQUEST", () -> ack.set(true));
            assertThat(ack).isFalse();
        });
        assertThat(ack).isTrue();
        assertThat(metrics.getDeliveredCount()).isEqualTo(before + 1);
    }

    @Test void actualRuntimeCanCommitAndReadOnlyItsTenantRecipientWithoutBypassRls() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), other = UUID.randomUUID(), record = UUID.randomUUID();
        var ack = new AtomicBoolean();
        assertThat(consumer.processIncomingEvent(tenant, UUID.randomUUID(), "synthetic-owner-case", record, employee,
                "Synthetic", "Private message", record.toString(), "REQUEST", () -> ack.set(true))).isTrue();
        assertThat(ack).isTrue();
        assertThat(notices.getUnreadCount(tenant, employee).count()).isEqualTo(1);
        var page = notices.listNotifications(tenant, employee, 0, 20);
        assertThat(page.items()).hasSize(1);
        UUID id = UUID.fromString(page.items().getFirst().id());
        assertThatThrownBy(() -> notices.markRead(tenant, other, id)).isInstanceOfSatisfying(ErrorResponseException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
        assertThat(notices.listNotifications(UUID.randomUUID(), employee, 0, 20).items()).isEmpty();
        notices.markRead(tenant, employee, id);
        assertThat(notices.getUnreadCount(tenant, employee).count()).isZero();
        assertThat(notices.getPreferences(tenant, employee).version()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification.notification_record", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname=current_user", Boolean.class)).isFalse();
    }
}
