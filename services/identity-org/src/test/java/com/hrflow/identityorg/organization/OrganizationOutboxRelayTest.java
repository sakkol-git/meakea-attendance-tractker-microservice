package com.hrflow.identityorg.organization;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.identityorg.organization.service.OrganizationOutboxRelay;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
        "hrflow.auth.local-bootstrap=true",
        "hrflow.auth.dev-password=synthetic-local-password",
        "hrflow.organization.relay.enabled=true",
        "hrflow.organization.relay.initial-delay-ms=3600000",
        "logging.level.com.hrflow.identityorg.organization.service.OrganizationOutboxRelay=DEBUG",
        "spring.kafka.listener.auto-startup=false"
})
class OrganizationOutboxRelayTest {
    private static final Path KEY_DIRECTORY = temporaryKeys();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("identity-init.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
        properties.add("hrflow.organization.relay.job-user", POSTGRES::getUsername);
        properties.add("hrflow.organization.relay.job-password", POSTGRES::getPassword);
        properties.add("hrflow.auth.key-directory", KEY_DIRECTORY::toString);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationOutboxRelay relay;
    @MockitoBean KafkaTemplate<Object, Object> kafka;

    @Test
    void brokerAcknowledgmentPrecedesSentMarkerAndRetryKeepsEventId() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID holiday = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        jdbc.update("INSERT INTO identity_org.auth_tenant(id, slug) VALUES (?, ?)", tenant, "relay-" + tenant);
        jdbc.update("""
                INSERT INTO identity_org.organization_outbox
                    (id, tenant_id, topic, aggregate_id, aggregate_revision, event_type,
                     correlation_id, payload, occurred_at)
                VALUES (?, ?, 'org.holiday.v1', ?, 1, 'org.holiday.added.v1', ?,
                        ?::jsonb, now())
                """, event, tenant, holiday, UUID.randomUUID(), """
                {"holidayId":"%s","date":"2026-11-09","name":"Company Day","kind":"COMPANY"}
                """.formatted(holiday));
        assertTrue(jdbc.queryForObject("""
                SELECT count(*) FROM identity_jobs.organization_tenant_registry WHERE tenant_id = ?
                """, Integer.class, tenant) == 1);
        CountDownLatch sent = new CountDownLatch(1);
        AtomicReference<OrganizationFact> published = new AtomicReference<>();
        CompletableFuture<SendResult<Object, Object>> acknowledged = new CompletableFuture<>();
        when(kafka.send(eq("org.holiday.v1"), eq(tenant + ":" + holiday), any()))
                .thenAnswer(invocation -> {
                    published.set(invocation.getArgument(2));
                    sent.countDown();
                    return acknowledged;
                });
        try (var worker = Executors.newSingleThreadExecutor()) {
            var run = worker.submit(() -> {
                for (int attempt = 0; attempt < 10 && sent.getCount() > 0; attempt++) relay.poll();
            });
            assertTrue(sent.await(5, TimeUnit.SECONDS));
            assertNull(sentAt(event));
            assertNotNull(published.get());
            assertTrue(published.get().getEventId().equals(event.toString()));
            acknowledged.complete(null);
            run.get(5, TimeUnit.SECONDS);
        }
        assertNotNull(sentAt(event));
        relay.poll();
        verify(kafka, times(1)).send(eq("org.holiday.v1"), eq(tenant + ":" + holiday), any());

        UUID secondHoliday = UUID.randomUUID();
        UUID secondEvent = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity_org.organization_outbox
                    (id, tenant_id, topic, aggregate_id, aggregate_revision, event_type,
                     correlation_id, payload, occurred_at)
                VALUES (?, ?, 'org.holiday.v1', ?, 1, 'org.holiday.added.v1', ?,
                        ?::jsonb, now())
                """, secondEvent, tenant, secondHoliday, UUID.randomUUID(), """
                {"holidayId":"%s","date":"2026-11-10","name":"Second Day","kind":"COMPANY"}
                """.formatted(secondHoliday));
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<String> retryId = new AtomicReference<>();
        when(kafka.send(eq("org.holiday.v1"), eq(tenant + ":" + secondHoliday), any()))
                .thenAnswer(invocation -> {
                    OrganizationFact fact = invocation.getArgument(2);
                    if (retryId.get() == null) retryId.set(fact.getEventId());
                    else assertTrue(retryId.get().equals(fact.getEventId()));
                    return attempts.incrementAndGet() == 1
                            ? CompletableFuture.failedFuture(new IllegalStateException("synthetic broker failure"))
                            : CompletableFuture.completedFuture(null);
                });
        for (int i = 0; i < 10 && attempts.get() == 0; i++) relay.poll();
        assertTrue(attempts.get() == 1);
        assertNull(sentAt(secondEvent));
        for (int i = 0; i < 10 && attempts.get() == 1; i++) relay.poll();
        assertTrue(attempts.get() == 2);
        assertNotNull(sentAt(secondEvent));
        assertTrue(retryId.get().equals(secondEvent.toString()));

        UUID standbyHoliday = UUID.randomUUID();
        UUID standbyEvent = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO identity_org.organization_outbox
                    (id, tenant_id, topic, aggregate_id, aggregate_revision, event_type,
                     correlation_id, payload, occurred_at)
                VALUES (?, ?, 'org.holiday.v1', ?, 1, 'org.holiday.added.v1', ?,
                        ?::jsonb, now())
                """, standbyEvent, tenant, standbyHoliday, UUID.randomUUID(), """
                {"holidayId":"%s","date":"2026-11-11","name":"Standby Day","kind":"COMPANY"}
                """.formatted(standbyHoliday));
        when(kafka.send(eq("org.holiday.v1"), eq(tenant + ":" + standbyHoliday), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        relay.closeLeader();
        try (var blocker = DriverManager.getConnection(POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(), POSTGRES.getPassword());
                var lock = blocker.createStatement();
                var held = lock.executeQuery("SELECT pg_try_advisory_lock(73104217)")) {
            assertTrue(held.next() && held.getBoolean(1));
            relay.poll();
            assertNull(sentAt(standbyEvent));
            verify(kafka, never()).send(eq("org.holiday.v1"), eq(tenant + ":" + standbyHoliday), any());
        }
        for (int i = 0; i < 10 && sentAt(standbyEvent) == null; i++) relay.poll();
        assertNotNull(sentAt(standbyEvent));
    }

    private Timestamp sentAt(UUID event) {
        return jdbc.queryForObject("""
                SELECT published_at FROM identity_org.organization_outbox WHERE id = ?
                """, Timestamp.class, event);
    }

    private static Path temporaryKeys() {
        try {
            return Files.createTempDirectory("hrflow-relay-keys-");
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }
}
