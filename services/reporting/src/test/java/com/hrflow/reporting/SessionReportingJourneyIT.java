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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0", "hrflow.reporting.session-consumer.enabled=true",
        "hrflow.reporting.daily-consumer.enabled=true",
        "spring.kafka.listener.auto-startup=true", "spring.kafka.listener.missing-topics-fatal=true",
        "hrflow.reporting.export.worker.enabled=true", "hrflow.reporting.export.worker.poll-ms=50"})
class SessionReportingJourneyIT extends SessionJourneyEnvironment {
    @LocalServerPort int port;

    @Test void realHttpCommandsConvergeThroughAvroKafkaIntoPrivateAutomaticCsvWithRuntimeRls() throws Exception {
        Path jar = Path.of("../attendance/target/attendance-0.1.0-SNAPSHOT.jar").toAbsolutePath().normalize();
        assertThat(jar).exists();
        int attendancePort;
        try (var socket = new ServerSocket(0)) { attendancePort = socket.getLocalPort(); }
        Path log = Files.createTempFile("hrflow-attendance-journey-", ".log");
        Process owner = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Xmx256m", "-jar", jar.toString(), "--server.port=" + attendancePort, "--management.server.port=0",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(), "--spring.datasource.username=attendance_runtime",
                "--spring.datasource.password=journey-only", "--spring.flyway.user=" + POSTGRES.getUsername(),
                "--spring.flyway.password=" + POSTGRES.getPassword(), "--hrflow.auth.jwks-uri=" + jwksUrl(),
                "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                "--hrflow.events.recovery.enabled=true", "--spring.kafka.properties.schema.registry.url=" + registryUrl(),
                "--hrflow.attendance.organization-consumer.enabled=false", "--hrflow.attendance.relay.enabled=true",
                "--hrflow.attendance.relay.job-user=attendance_job", "--hrflow.attendance.relay.job-password=journey-only",
                "--hrflow.attendance.daily-status.enabled=true", "--hrflow.attendance.daily-worker.enabled=true",
                "--hrflow.attendance.cutoff.enabled=true", "--hrflow.attendance.cutoff-consumer.enabled=true",
                "--hrflow.attendance.cutoff.poll-ms=50", "--hrflow.attendance.daily-worker.poll-ms=50",
                "--hrflow.attendance.relay.poll-ms=50").redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            String attendance = "http://127.0.0.1:" + attendancePort;
            await().atMost(Duration.ofSeconds(45)).ignoreExceptionsMatching(failure ->
                    failure instanceof java.net.ConnectException).untilAsserted(() -> {
                assertThat(owner.isAlive()).as("Attendance child; diagnostic log %s", log).isTrue();
                assertThat(HTTP.send(HttpRequest.newBuilder(URI.create(attendance + "/attendance/today"))
                        .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
            });
            UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), admin = UUID.randomUUID();
            seedOrganizationFixture(tenant, employee);
            String employeeToken = token(tenant, employee, "EMPLOYEE");
            String adminToken = token(tenant, admin, "HR_ADMIN");
            String body = "{\"latitude\":11.56,\"longitude\":104.92,\"accuracyMeters\":10.0,\"deviceModel\":\"Journey fixture\"}";
            var checkin = post(attendance + "/attendance/check-in", employeeToken, body, null);
            assertThat(checkin.statusCode()).isEqualTo(201);
            String etag = checkin.headers().firstValue("ETag").orElseThrow();
            var checkout = post(attendance + "/attendance/check-out", employeeToken, body, etag);
            assertThat(checkout.statusCode()).isEqualTo(200);
            String reporting = "http://127.0.0.1:" + port;
            UUID absentEmployee = UUID.randomUUID();
            // Keep this synthetic no-tap shift already closed whatever time the suite is executed.
            int utcHour = java.time.Instant.now().atZone(java.time.ZoneOffset.UTC).getHour();
            String fixtureZone = java.time.ZoneOffset.ofHours(Math.floorMod(12 - utcHour + 12, 24) - 12).getId();
            seedOrganizationFixture(tenant, absentEmployee, fixtureZone, "09:00");
            var fixtureAdmin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var dashboard = get(reporting + "/reports/dashboard", adminToken);
                assertThat(dashboard.statusCode()).isEqualTo(200);
                assertThat(JSON.readTree(dashboard.body()).path("totalAbsentToday").asInt()).isEqualTo(1);
                assertThat(fixtureAdmin.queryForObject("SELECT count(*) FROM attendance.attendance_evidence WHERE tenant_id=? AND employee_id=?",
                        Integer.class, tenant, absentEmployee)).isZero();
            });
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var summary = get(reporting + "/reports/attendance/summary", adminToken);
                assertThat(summary.statusCode()).isEqualTo(200);
                var records = JSON.readTree(summary.body()).path("records");
                assertThat(records.size()).isEqualTo(1);
                assertThat(records.get(0).path("aggregateRevision").asLong()).isEqualTo(2);
                assertThat(records.get(0).path("status").asText()).isEqualTo("COMPLETED");
                assertThat(records.get(0).path("clockOut").isNull()).isFalse();
            });
            var queued = post(reporting + "/reports/exports", adminToken,
                    "{\"report\":\"ATTENDANCE\",\"format\":\"CSV\"}", null);
            assertThat(queued.statusCode()).isEqualTo(202);
            String id = JSON.readTree(queued.body()).path("id").asText();
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(JSON.readTree(
                    get(reporting + "/reports/exports/" + id, adminToken).body()).path("status").asText())
                    .isEqualTo("COMPLETED"));
            var download = get(reporting + "/reports/exports/" + id + "/download", adminToken);
            assertThat(download.statusCode()).isEqualTo(200);
            assertThat(download.headers().firstValue("Cache-Control")).contains("no-store");
            assertThat(download.body()).contains(employee.toString(), "COMPLETED", "TAP");
            assertThat(download.body()).doesNotContain("Journey fixture", "latitude", storage.toString());
            assertThat(get(reporting + "/reports/exports/" + id + "/download",
                    token(tenant, UUID.randomUUID(), "HR_ADMIN")).statusCode()).isEqualTo(404);
            assertThat(get(reporting + "/reports/exports/" + id + "/download", employeeToken).statusCode()).isEqualTo(403);
            var foreign = get(reporting + "/reports/attendance/summary", token(UUID.randomUUID(), admin, "HR_ADMIN"));
            assertThat(JSON.readTree(foreign.body()).path("totalCount").asInt()).isZero();
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

    private static UUID seedOrganizationFixture(UUID tenant, UUID employee) {
        return seedOrganizationFixture(tenant, employee, "Asia/Phnom_Penh", "17:00");
    }

    private static UUID seedOrganizationFixture(UUID tenant, UUID employee, String zone, String shiftEnd) {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        UUID shift = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO attendance.shift_projection(tenant_id,shift_id,name,start_time,end_time,timezone,
                  grace_minutes,working_days,site_id,site_name,latitude,longitude,radius_m,revision)
                VALUES (?,?,'Fixture','08:00',cast(? as time),?,10,127,?,'Fixture',11.56,104.92,100,1)
                """, tenant, shift, shiftEnd, zone, UUID.randomUUID());
        String assignments = "[{\"assignmentId\":\"%s\",\"shiftId\":\"%s\",\"validFrom\":\"2000-01-01\",\"validTo\":null}]"
                .formatted(UUID.randomUUID(), shift);
        jdbc.update("""
                INSERT INTO attendance.employee_projection(tenant_id,employee_id,employee_no,full_name,status,assignments,revision)
                VALUES (?,?,'FIXTURE','Synthetic','ACTIVE',cast(? as jsonb),1)
                """, tenant, employee, assignments);
        return shift;
    }
}
