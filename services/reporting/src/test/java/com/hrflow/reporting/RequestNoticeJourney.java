package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/** Disposable real notification process; explicit synthetic role bindings, never an onboarding substitute. */
final class RequestNoticeJourney implements AutoCloseable {
    final UUID hr = UUID.randomUUID();
    final int port;
    private final Process process;

    RequestNoticeJourney() throws Exception {
        var jar = Path.of("../notification/target/notification-0.1.0-SNAPSHOT.jar").toAbsolutePath().normalize();
        assertThat(jar).exists();
        try (var producer = new KafkaProducer<String, OrganizationFact>(Map.of(
                "bootstrap.servers", SessionJourneyEnvironment.KAFKA.getBootstrapServers(),
                "key.serializer", org.apache.kafka.common.serialization.StringSerializer.class,
                "value.serializer", io.confluent.kafka.serializers.KafkaAvroSerializer.class,
                "schema.registry.url", SessionJourneyEnvironment.registryUrl(), "auto.register.schemas", false))) {
            for (var fact : List.of(linked(SessionJourneyEnvironment.TEAM_EMPLOYEE, SessionJourneyEnvironment.TEAM_MANAGER, List.of("EMPLOYEE")),
                    linked(SessionJourneyEnvironment.TEAM_MANAGER, null, List.of("EMPLOYEE", "MANAGER")),
                    linked(hr, null, List.of("EMPLOYEE", "HR_ADMIN")))) {
                producer.send(new ProducerRecord<>("org.employee.v1", fact.getTenantId() + ":" + fact.getAggregateId(), fact)).get();
            }
        }
        int managementPort;
        try (var socket = new ServerSocket(0); var management = new ServerSocket(0)) {
            port = socket.getLocalPort(); managementPort = management.getLocalPort();
        }
        var log = Files.createTempFile("hrflow-request-notification-journey-", ".log");
        process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(), "-Xmx256m", "-jar", jar.toString(),
                "--server.port=" + port, "--management.server.port=" + managementPort,
                "--spring.datasource.url=" + SessionJourneyEnvironment.POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=notification_runtime", "--spring.datasource.password=journey-only",
                "--spring.flyway.user=" + SessionJourneyEnvironment.POSTGRES.getUsername(),
                "--spring.flyway.password=" + SessionJourneyEnvironment.POSTGRES.getPassword(),
                "--hrflow.auth.jwks-uri=" + SessionJourneyEnvironment.jwksUrl(),
                "--spring.kafka.bootstrap-servers=" + SessionJourneyEnvironment.KAFKA.getBootstrapServers(),
                "--hrflow.events.recovery.enabled=true", "--spring.kafka.properties.schema.registry.url=" + SessionJourneyEnvironment.registryUrl(),
                "--hrflow.notification.domain-consumer.enabled=true", "--spring.kafka.listener.auto-startup=true")
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            await().atMost(Duration.ofSeconds(45)).ignoreExceptionsMatching(error -> error instanceof java.net.ConnectException)
                    .untilAsserted(() -> {
                        assertThat(process.isAlive()).as("Notification child log %s", log).isTrue();
                        assertThat(SessionJourneyEnvironment.HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + managementPort + "/actuator/health/readiness"))
                                .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
                    });
        } catch (Exception | AssertionError error) { close(); throw error; }
    }

    void expect(String jwt, String requestId, String title) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var response = SessionJourneyEnvironment.HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/notifications"))
                    .header("Authorization", "Bearer " + jwt).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            var items = SessionJourneyEnvironment.JSON.readTree(response.body()).path("items");
            assertThat(items).anySatisfy(item -> {
                assertThat(item.path("kind").asText()).isEqualTo("request");
                assertThat(item.path("recordId").asText()).isEqualTo(requestId);
                assertThat(item.path("title").asText()).isEqualTo(title);
                assertThat(item.path("body").asText()).doesNotContain("Sensitive");
            });
        });
    }

    private OrganizationFact linked(UUID employee, UUID manager, List<String> roles) {
        var fact = ManagerReportScopeTest.employee(SessionJourneyEnvironment.TEAM_TENANT, employee, manager, 2);
        var snapshot = (EmployeeSnapshot) fact.getPayload();
        snapshot.setUserId(employee.toString()); snapshot.setAccountRoles(roles);
        return fact;
    }
    @Override public void close() throws Exception {
        process.destroy();
        if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly();
    }
}
