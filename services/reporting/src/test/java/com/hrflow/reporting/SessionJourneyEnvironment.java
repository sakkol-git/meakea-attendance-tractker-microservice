package com.hrflow.reporting;

import com.hrflow.contracts.attendance.AttendanceSessionFact;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/** Real disposable transports; the JWKS fixture signs only synthetic test identities. */
@Testcontainers
abstract class SessionJourneyEnvironment {
    static final Network NETWORK = Network.newNetwork();
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("session-journey-schema.sql");
    @Container static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:8.2.0")
            .withNetwork(NETWORK).withNetworkAliases("kafka").withListener("kafka:19092");
    @Container static final GenericContainer<?> REGISTRY = new GenericContainer<>("confluentinc/cp-schema-registry:8.2.0")
            .withNetwork(NETWORK).withExposedPorts(8081).dependsOn(KAFKA)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    static HttpServer jwks;
    static RSAKey key;
    static Path storage;
    static final UUID TEAM_TENANT = UUID.randomUUID(), TEAM_MANAGER = UUID.randomUUID(), TEAM_EMPLOYEE = UUID.randomUUID();

    static String registryUrl() { return "http://" + REGISTRY.getHost() + ":" + REGISTRY.getMappedPort(8081); }
    static String jwksUrl() { return "http://127.0.0.1:" + jwks.getAddress().getPort() + "/jwks"; }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) throws Exception {
        key = new RSAKeyGenerator(2048).keyID("journey-fixture").generate();
        jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwks.createContext("/jwks", exchange -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var stream = exchange.getResponseBody()) { stream.write(body); }
        });
        jwks.start();
        storage = Files.createTempDirectory("hrflow-private-journey-");
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic("attendance.session.v1", 1, (short) 1), new NewTopic("attendance.correction.v1", 1, (short) 1),
                    new NewTopic("org.employee.v1", 1, (short) 1),
                    new NewTopic("attendance.daily-status.v1", 1, (short) 1),
                    new NewTopic("time.shift-cutoff.v1", 1, (short) 1),
                    new NewTopic("leave.request.v1", 1, (short) 1),
                    new NewTopic("request.workflow.v1", 1, (short) 1))).all().get();
        }
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            var recovery = new java.util.ArrayList<NewTopic>();
            for (String source : com.hrflow.events.SourceRecords.TOPICS)
                for (String suffix : List.of(".retry-0", ".retry-1", ".retry-2", ".dlq")) recovery.add(new NewTopic(source + suffix, 1, (short) 1));
            admin.createTopics(recovery).all().get();
        }
        p.add("hrflow.events.recovery.enabled", () -> "true");
        var registration = HTTP.send(HttpRequest.newBuilder(URI.create(registryUrl()
                        + "/subjects/attendance.session.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(
                        Map.of("schema", AttendanceSessionFact.getClassSchema().toString())))).build(),
                HttpResponse.BodyHandlers.ofString());
        if (registration.statusCode() != 200) throw new IllegalStateException("Test Avro registration failed");
        var organizationRegistration = HTTP.send(HttpRequest.newBuilder(URI.create(registryUrl()
                        + "/subjects/org.employee.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("schema",
                        com.hrflow.contracts.organization.OrganizationFact.getClassSchema().toString())))).build(),
                HttpResponse.BodyHandlers.ofString());
        if (organizationRegistration.statusCode() != 200) throw new IllegalStateException("Organization schema registration failed");
        var leaveRegistration = HTTP.send(HttpRequest.newBuilder(URI.create(registryUrl()
                        + "/subjects/leave.request.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("schema",
                        com.hrflow.contracts.leave.LeaveRequestFact.getClassSchema().toString())))).build(),
                HttpResponse.BodyHandlers.ofString());
        if (leaveRegistration.statusCode() != 200) throw new IllegalStateException("Leave schema registration failed");
        var requestRegistration = HTTP.send(HttpRequest.newBuilder(URI.create(registryUrl()
                        + "/subjects/request.workflow.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("schema",
                        com.hrflow.contracts.request.RequestWorkflowFact.getClassSchema().toString())))).build(),
                HttpResponse.BodyHandlers.ofString());
        if (requestRegistration.statusCode() != 200) throw new IllegalStateException("Request schema registration failed");
        var dailyRegistration = HTTP.send(HttpRequest.newBuilder(URI.create(registryUrl()
                        + "/subjects/attendance.daily-status.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("schema",
                        com.hrflow.contracts.attendance.AttendanceDailyFact.getClassSchema().toString())))).build(),
                HttpResponse.BodyHandlers.ofString());
        if (dailyRegistration.statusCode() != 200) throw new IllegalStateException("Daily schema registration failed");
        var correctionRegistration = HTTP.send(HttpRequest.newBuilder(URI.create(registryUrl() + "/subjects/attendance.correction.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("schema", com.hrflow.contracts.attendance.AttendanceCorrectionFact.getClassSchema().toString())))).build(), HttpResponse.BodyHandlers.ofString());
        if (correctionRegistration.statusCode() != 200) throw new IllegalStateException("Correction schema registration failed");
        var cutoffRegistration = HTTP.send(HttpRequest.newBuilder(URI.create(registryUrl()
                        + "/subjects/time.shift-cutoff.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(Map.of("schema",
                        com.hrflow.contracts.attendance.ShiftCutoffFact.getClassSchema().toString())))).build(),
                HttpResponse.BodyHandlers.ofString());
        if (cutoffRegistration.statusCode() != 200) throw new IllegalStateException("Cutoff schema registration failed");
        try (var producer = new org.apache.kafka.clients.producer.KafkaProducer<String, com.hrflow.contracts.organization.OrganizationFact>(
                Map.of("bootstrap.servers", KAFKA.getBootstrapServers(), "key.serializer", org.apache.kafka.common.serialization.StringSerializer.class,
                        "value.serializer", io.confluent.kafka.serializers.KafkaAvroSerializer.class,
                        "schema.registry.url", registryUrl(), "auto.register.schemas", false, "acks", "all"))) {
            for (var fact : List.of(ManagerReportScopeTest.employee(TEAM_TENANT, TEAM_MANAGER, null, 1),
                    ManagerReportScopeTest.employee(TEAM_TENANT, TEAM_EMPLOYEE, TEAM_MANAGER, 1))) {
                producer.send(new org.apache.kafka.clients.producer.ProducerRecord<>("org.employee.v1",
                        fact.getTenantId() + ":" + fact.getAggregateId(), fact)).get();
            }
        }
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "reporting_runtime");
        p.add("spring.datasource.password", () -> "journey-only");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        p.add("spring.kafka.properties.schema.registry.url", SessionJourneyEnvironment::registryUrl);
        p.add("hrflow.auth.jwks-uri", SessionJourneyEnvironment::jwksUrl);
        p.add("hrflow.reporting.export.storage-dir", storage::toString);
        p.add("hrflow.reporting.export.worker.job-user", () -> "reporting_job");
        p.add("hrflow.reporting.export.worker.job-password", () -> "journey-only");
    }

    static String token(UUID tenant, UUID employee, String role) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer("hrflow-local").subject(employee.toString())
                .audience("hrflow-clients").issueTime(java.util.Date.from(now.minusSeconds(1)))
                .expirationTime(java.util.Date.from(now.plusSeconds(300))).claim("tenantId", tenant.toString())
                .claim("employeeId", employee.toString()).claim("roles", List.of(role)).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT)
                .keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    @AfterAll static void cleanupEnvironment() throws Exception {
        if (jwks != null) jwks.stop(0);
        if (storage != null) {
            try (var files = Files.list(storage)) { for (var file : files.toList()) Files.deleteIfExists(file); }
            Files.deleteIfExists(storage);
        }
    }
}
