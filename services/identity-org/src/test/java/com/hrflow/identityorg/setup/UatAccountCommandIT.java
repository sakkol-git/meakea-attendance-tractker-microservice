package com.hrflow.identityorg.setup;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the actual packaged private entry point; no fake console or bypass provisioning endpoint. */
class UatAccountCommandIT extends UatAccountFixture {
    @TempDir Path output;

    @Test void packagedInteractiveSetupUsesRuntimeCredentialsAndIssuesAnActuallyLinkedLogin() throws Exception {
        var tenant = tenant();
        UUID employee = employee(tenant);
        Path log = output.resolve("private-setup.log");
        var command = new ProcessBuilder("python3", "src/test/resources/uat-setup-terminal.py", java(),
                "-Dloader.main=com.hrflow.uat.IdentityUatAccountSetup", "-cp", jar(),
                "org.springframework.boot.loader.launch.PropertiesLauncher", "--tenant=" + tenant.getSlug(),
                "--employee=" + employee, "--email=terminal@example.test", "--roles=EMPLOYEE,MANAGER", "--time-zone=Asia/Phnom_Penh");
        command.environment().put("SPRING_DATASOURCE_URL", POSTGRES.getJdbcUrl());
        command.environment().put("SPRING_DATASOURCE_USERNAME", "identity_setup_runtime");
        command.environment().put("SPRING_DATASOURCE_PASSWORD", "setup-test-only");
        // These do not permit the CLI to enable server/bootstrap/migration behavior.
        command.environment().put("HRFLOW_AUTH_LOCAL_BOOTSTRAP", "true");
        command.environment().put("SPRING_FLYWAY_ENABLED", "true");
        var child = command.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertThat(child.waitFor(70, TimeUnit.SECONDS)).isTrue();
            String result = Files.readString(log);
            assertThat(child.exitValue()).as("Private setup diagnostic: %s", result).isZero();
            assertThat(result).contains("Created account", employee.toString()).doesNotContain("synthetic-cli-password", "Tomcat started", "generated security password");
            var jwt = decoder.decode(sessions.login("terminal-fixture", tenant.getSlug(), "terminal@example.test", "synthetic-cli-password").accessToken());
            assertThat(jwt.getClaimAsString("employeeId")).isEqualTo(employee.toString());
            assertThat(jwt.getClaimAsStringList("roles")).containsExactly("EMPLOYEE", "MANAGER");
        } finally { if (child.isAlive()) child.destroyForcibly().waitFor(); }
    }

    @Test void redirectedInputAndSecretArgumentsAreRejectedWithoutStartingAService() throws Exception {
        String employee = UUID.randomUUID().toString();
        String[] arguments = {"--tenant=explicit", "--employee=" + employee, "--email=test@example.test", "--roles=EMPLOYEE", "--time-zone=UTC"};
        String result = noninteractive(arguments);
        assertThat(result).contains("interactive terminal is required").doesNotContain("Created account", "Tomcat started");
        String secretResult = noninteractive("--password=must-never-be-printed");
        assertThat(secretResult).contains("Unsupported setup option").doesNotContain("must-never-be-printed", "Created account");
    }

    private String noninteractive(String... args) throws Exception {
        var command = new java.util.ArrayList<>(java.util.List.of(java(), "-Dloader.main=com.hrflow.uat.IdentityUatAccountSetup", "-cp", jar(), "org.springframework.boot.loader.launch.PropertiesLauncher"));
        command.addAll(java.util.List.of(args));
        var child = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            child.getOutputStream().close();
            assertThat(child.waitFor(10, TimeUnit.SECONDS)).isTrue();
            assertThat(child.exitValue()).isEqualTo(2);
            return new String(child.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } finally { if (child.isAlive()) child.destroyForcibly().waitFor(); }
    }
    private static String java() { return Path.of(System.getProperty("java.home"), "bin", "java").toString(); }
    private static String jar() { return Path.of("target", "identity-org-0.1.0-SNAPSHOT.jar").toAbsolutePath().toString(); }
}
