package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0", "hrflow.reporting.request-consumer.enabled=true",
        "hrflow.reporting.export.worker.enabled=true", "hrflow.reporting.export.worker.poll-ms=50",
        "spring.kafka.listener.auto-startup=true"})
class RequestReportingJourneyIT extends SessionJourneyEnvironment {
    @LocalServerPort int port;

    @Test void signedRequestCommandsConvergeThroughRealKafkaAndAnAutomaticallyCompletedPrivateCsv() throws Exception {
        Path jar = Path.of("../request/target/request-0.1.0-SNAPSHOT.jar").toAbsolutePath().normalize();
        assertThat(jar).exists();
        int ownerPort, managementPort;
        try (var socket = new ServerSocket(0); var management = new ServerSocket(0)) {
            ownerPort = socket.getLocalPort();
            managementPort = management.getLocalPort();
        }
        Path log = Files.createTempFile("hrflow-request-journey-", ".log");
        Process owner = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(), "-Xmx256m", "-jar", jar.toString(),
                "--server.port=" + ownerPort, "--management.server.port=" + managementPort, "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=request_runtime", "--spring.datasource.password=journey-only",
                "--spring.flyway.user=" + POSTGRES.getUsername(), "--spring.flyway.password=" + POSTGRES.getPassword(),
                "--hrflow.auth.jwks-uri=" + jwksUrl(), "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                "--hrflow.events.recovery.enabled=true", "--spring.kafka.properties.schema.registry.url=" + registryUrl(), "--hrflow.request.employee-consumer.enabled=true",
                "--hrflow.request.relay.enabled=true", "--hrflow.request.relay.job-user=request_job",
                "--hrflow.request.relay.job-password=journey-only", "--hrflow.request.relay.poll-ms=50")
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try (var notifications = new RequestNoticeJourney()) {
            String base = "http://127.0.0.1:" + ownerPort + "/requests";
            await().atMost(Duration.ofSeconds(45)).ignoreExceptionsMatching(error -> error instanceof java.net.ConnectException)
                    .untilAsserted(() -> {
                        assertThat(owner.isAlive()).as("Request child diagnostic log %s", log).isTrue();
                        assertThat(HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + managementPort + "/actuator/health/readiness"))
                                .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
                    });
            String employee = token(TEAM_TENANT, TEAM_EMPLOYEE, "EMPLOYEE"), manager = token(TEAM_TENANT, TEAM_MANAGER, "MANAGER");
            String admin = token(TEAM_TENANT, notifications.hr, "HR_ADMIN");
            var draft = post(base, employee, "{\"type\":\"OTHER\",\"title\":\"Sensitive title\",\"reason\":\"Sensitive reason\",\"quantity\":1}", null);
            assertThat(draft.statusCode()).isEqualTo(201);
            String id = JSON.readTree(draft.body()).path("id").asText();
            var submitted = post(base + "/" + id + "/submit", employee, "[]", draft.headers().firstValue("ETag").orElseThrow());
            assertThat(submitted.statusCode()).isEqualTo(200);
            notifications.expect(manager, id, "Request approval needed");
            String revision = submitted.headers().firstValue("ETag").orElseThrow();
            assertThat(post(base + "/" + id + "/approve", admin, "{}", revision).statusCode()).isEqualTo(403);
            assertThat(get(base + "/" + id, token(TEAM_TENANT, UUID.randomUUID(), "MANAGER")).statusCode()).isEqualTo(403);
            var managerDecision = post(base + "/" + id + "/approve", manager, "{}", revision);
            assertThat(managerDecision.statusCode()).isEqualTo(200);
            notifications.expect(admin, id, "Request approval needed");
            var hrDecision = post(base + "/" + id + "/approve", admin, "{}", managerDecision.headers().firstValue("ETag").orElseThrow());
            assertThat(hrDecision.statusCode()).isEqualTo(200);
            notifications.expect(employee, id, "Request approved");
            String reporting = "http://127.0.0.1:" + port;
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var dashboard = get(reporting + "/reports/dashboard", admin);
                assertThat(dashboard.statusCode()).isEqualTo(200);
                // Wait for this particular committed owner revision, rather than accepting an initially empty dashboard.
                var projection = new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
                assertThat(projection.queryForObject("SELECT status FROM reporting.request_fact_projection WHERE tenant_id=? AND request_id=?",
                        String.class, TEAM_TENANT, UUID.fromString(id))).isEqualTo("APPROVED");
            });
            var queued = post(reporting + "/reports/exports", admin, "{\"report\":\"REQUESTS\",\"format\":\"CSV\"}", null);
            assertThat(queued.statusCode()).isEqualTo(202);
            String export = JSON.readTree(queued.body()).path("id").asText();
            String download = reporting + "/reports/exports/" + export;
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                    assertThat(JSON.readTree(get(download, admin).body()).path("status").asText()).isEqualTo("COMPLETED"));
            var file = get(download + "/download", admin);
            assertThat(file.statusCode()).isEqualTo(200);
            assertThat(file.body()).contains("createdAt", TEAM_EMPLOYEE.toString(), id, "APPROVED").doesNotContain("Sensitive", storage.toString());
            assertThat(get(download + "/download", token(TEAM_TENANT, UUID.randomUUID(), "HR_ADMIN")).statusCode()).isEqualTo(404);
            assertThat(get(download + "/download", employee).statusCode()).isEqualTo(403);
        } finally {
            owner.destroy();
            if (!owner.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) owner.destroyForcibly();
        }
    }

    private static HttpResponse<String> get(String url, String token) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static HttpResponse<String> post(String url, String token, String body, String etag) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString());
        if (etag != null) request.header("If-Match", etag);
        return HTTP.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
