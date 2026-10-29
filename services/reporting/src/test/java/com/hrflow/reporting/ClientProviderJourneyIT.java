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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

/** Actual consumer repositories against packaged direct providers; explicitly no gateway/login acceptance. */
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "HRFLOW_VERIFY_CLIENT_PROVIDERS", matches = "true")
@SpringBootTest(properties = {"spring.kafka.listener.auto-startup=false", "management.server.port=0"})
class ClientProviderJourneyIT extends SessionJourneyEnvironment {
    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS) Path files;

    @Test void webAndNativeConsumersUseTheActualPrivateProvidersAndCannotSkipRequiredCases() throws Exception {
        var children = new ArrayList<Process>();
        try {
            int requestPort = port(), notificationPort = port(), attendancePort = port();
            children.add(provider("request", requestPort, true));
            children.add(provider("notification", notificationPort, false));
            children.add(provider("attendance", attendancePort, false));
            for (int port : List.of(requestPort, notificationPort, attendancePort)) {
                await().atMost(Duration.ofSeconds(60)).ignoreExceptionsMatching(error -> error instanceof java.net.ConnectException)
                        .untilAsserted(() -> {
                            assertThat(children).allMatch(Process::isAlive);
                            // Actual authenticated request, never a guessed process-start delay.
                            String path = port == requestPort ? "/requests/types?page=0&size=25" : port == attendancePort ? "/attendance/corrections" : "/notifications";
                            var response = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                                    .header("Authorization", "Bearer " + token(TEAM_TENANT, TEAM_EMPLOYEE, "EMPLOYEE")).GET().build(), HttpResponse.BodyHandlers.ofString());
                            assertThat(response.statusCode()).isEqualTo(200);
                        });
            }
            // Wait for organization replay separately: a reachable HTTP endpoint does not prove relationship readiness.
            var admin = new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(admin.queryForObject(
                    "SELECT count(*) FROM request.employee_projection WHERE tenant_id=?", Integer.class, TEAM_TENANT.toString())).isEqualTo(2));
            admin.update("INSERT INTO request.material_item(id,tenant_id,code,name,category,active) VALUES (?,?,'FIXTURE-MATERIAL','Synthetic fixture material','EQUIPMENT',true)", UUID.randomUUID().toString(), TEAM_TENANT.toString());
            ClientProviderFixtures.attendance(admin, TEAM_TENANT, TEAM_EMPLOYEE, TEAM_MANAGER);
            Map<String, String> environment = new LinkedHashMap<>();
            environment.put("HRFLOW_REQUEST_BASE_URL", "http://127.0.0.1:" + requestPort);
            environment.put("HRFLOW_NOTIFICATION_BASE_URL", "http://127.0.0.1:" + notificationPort);
            environment.put("HRFLOW_ATTENDANCE_BASE_URL", "http://127.0.0.1:" + attendancePort);
            environment.put("HRFLOW_EMPLOYEE_TOKEN", token(TEAM_TENANT, TEAM_EMPLOYEE, "EMPLOYEE"));
            environment.put("HRFLOW_MANAGER_TOKEN", token(TEAM_TENANT, TEAM_MANAGER, "MANAGER"));
            environment.put("HRFLOW_HR_TOKEN", token(TEAM_TENANT, UUID.randomUUID(), "HR_ADMIN"));
            environment.put("HRFLOW_OWNER_TOKEN", token(TEAM_TENANT, UUID.randomUUID(), "OWNER"));
            environment.put("HRFLOW_FOREIGN_TOKEN", token(UUID.randomUUID(), UUID.randomUUID(), "EMPLOYEE"));
            Path root = Path.of("../..").toAbsolutePath().normalize();
            runClient(root.resolve("web"), List.of("./node_modules/.bin/vitest", "run", "--config", "tests/live-provider.config.ts"), environment, "web");
            runClient(root.resolve("mobile"), List.of(flutter(root), "test", "test/live_provider_test.dart", "--reporter", "expanded"), environment, "mobile");
        } finally {
            for (Process child : children) {
                child.destroy();
                if (!child.waitFor(10, TimeUnit.SECONDS)) child.destroyForcibly().waitFor();
            }
        }
    }

    private Process provider(String service, int port, boolean organization) throws Exception {
        Path jar = Path.of("../" + service + "/target/" + service + "-0.1.0-SNAPSHOT.jar").toAbsolutePath().normalize();
        assertThat(jar).exists();
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin/java").toString(), "-Xmx192m", "-jar", jar.toString(),
                "--server.port=" + port, "--management.server.port=0", "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + service + "_runtime", "--spring.datasource.password=journey-only",
                "--spring.flyway.user=" + POSTGRES.getUsername(), "--spring.flyway.password=" + POSTGRES.getPassword(),
                "--hrflow.auth.jwks-uri=" + jwksUrl(), "--spring.kafka.bootstrap-servers=" + KAFKA.getBootstrapServers(),
                "--hrflow.events.recovery.enabled=true", "--spring.kafka.properties.schema.registry.url=" + registryUrl(), "--attachment.storage.dir=" + files.resolve("attachments")));
        if (organization) command.add("--hrflow.request.employee-consumer.enabled=true");
        else command.add("--spring.kafka.listener.auto-startup=false");
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(files.resolve(service + ".log").toFile()).start();
    }

    private void runClient(Path cwd, List<String> command, Map<String, String> environment, String name) throws Exception {
        Path log = files.resolve(name + ".log");
        var builder = new ProcessBuilder(command).directory(cwd.toFile());
        builder.environment().putAll(environment);
        var child = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertThat(child.waitFor(120, TimeUnit.SECONDS)).as("%s consumer timed out; log %s", name, log).isTrue();
            String output = Files.readString(log);
            assertThat(output).as("Credentials must never be printed by the integration harness")
                    .doesNotContain(environment.get("HRFLOW_EMPLOYEE_TOKEN"), environment.get("HRFLOW_HR_TOKEN"));
            assertThat(child.exitValue()).as("%s consumer failure; inspect private log %s", name, log).isZero();
            if (name.equals("web")) assertThat(output).contains("2 passed").doesNotContain("skipped");
            else assertThat(output).contains("+2:", "All tests passed").doesNotContain("Skipped:");
        } finally { if (child.isAlive()) child.destroyForcibly().waitFor(); }
    }
    private static String flutter(Path root) {
        var local = root.resolve("mobile/.fvm/flutter_sdk/bin/flutter");
        return Files.isRegularFile(local) ? local.toString() : "flutter";
    }
    private static int port() throws Exception { try (var socket = new ServerSocket(0)) { return socket.getLocalPort(); } }
}
