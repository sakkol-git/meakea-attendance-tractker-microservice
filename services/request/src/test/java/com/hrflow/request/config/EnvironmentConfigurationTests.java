package com.hrflow.request.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.TestConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvironmentConfigurationTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsDotEnvAsPropertiesAndHonorsCommandLineOverrides() throws Exception {
        Path envFile = temporaryDirectory.resolve(".env");
        Files.writeString(envFile, "SERVER_PORT=18199\nDB_USERNAME=test_runtime\n"
                + "DB_PASSWORD=synthetic-test-value\n");
        SpringApplication app = new SpringApplication(Probe.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setLogStartupInfo(false);
        try (var context = app.run(
                "--spring.config.import=" + envFile.toUri() + "[.properties]",
                "--spring.main.banner-mode=off", "--spring.main.web-application-type=none",
                "--SERVER_PORT=18200")) {
            var env = context.getEnvironment();
            assertEquals("request", env.getProperty("spring.application.name"));
            assertEquals("18200", env.getProperty("SERVER_PORT"));
            // Command-line arguments beat the imported file and resolve the server placeholder.
            assertEquals("18200", env.getProperty("server.port"));
            assertEquals("never", env.getProperty("server.error.include-stacktrace"));
            assertEquals("test_runtime", env.getProperty("spring.datasource.username"));
            assertEquals("synthetic-test-value", env.getProperty("spring.datasource.password"));
            assertEquals("request", env.getProperty("spring.datasource.hikari.schema"));
            assertEquals("true", env.getProperty("spring.flyway.clean-disabled"));

        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Probe {
    }
}
