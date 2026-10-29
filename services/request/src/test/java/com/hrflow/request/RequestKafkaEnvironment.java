package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.request.dto.CreateGeneralRequestDto;
import com.hrflow.request.service.RequestService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.ErrorResponseException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
abstract class RequestKafkaEnvironment {
    static final Network NETWORK = Network.newNetwork();
    static final String TENANT = UUID.randomUUID().toString(), EMPLOYEE = UUID.randomUUID().toString(), MANAGER = UUID.randomUUID().toString();
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("request-live-schema.sql");
    @Container static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:8.2.0")
            .withNetwork(NETWORK).withNetworkAliases("kafka").withListener("kafka:19092");
    @Container static final GenericContainer<?> REGISTRY = new GenericContainer<>("confluentinc/cp-schema-registry:8.2.0")
            .withNetwork(NETWORK).withExposedPorts(8081).dependsOn(KAFKA)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "registry").withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
    static String registry() { return "http://" + REGISTRY.getHost() + ":" + REGISTRY.getMappedPort(8081); }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic("org.employee.v1", 2, (short) 1).configs(Map.of("cleanup.policy", "compact")))).all().get();
        }
        var registered = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(registry() + "/subjects/org.employee.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(
                        Map.of("schema", OrganizationFact.getClassSchema().toString())))).build(), HttpResponse.BodyHandlers.ofString());
        if (registered.statusCode() != 200) throw new IllegalStateException("Organization schema registration failed");
        try (var producer = new KafkaProducer<String, OrganizationFact>(Map.of("bootstrap.servers", KAFKA.getBootstrapServers(),
                "key.serializer", org.apache.kafka.common.serialization.StringSerializer.class,
                "value.serializer", io.confluent.kafka.serializers.KafkaAvroSerializer.class, "schema.registry.url", registry(),
                "auto.register.schemas", false, "acks", "all"))) {
            for (var fact : List.of(RequestEmployeeReferenceTest.fact(TENANT, MANAGER, null, 1, "ACTIVE"),
                    RequestEmployeeReferenceTest.fact(TENANT, EMPLOYEE, MANAGER, 1, "ACTIVE"))) {
                producer.send(new ProducerRecord<>("org.employee.v1", fact.getTenantId() + ":" + fact.getAggregateId(), fact)).get();
            }
        }
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic("request.workflow.v1", 1, (short) 1))).all().get();
        }
        var workflow = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(registry() + "/subjects/request.workflow.v1-value/versions"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.builder().build().writeValueAsString(
                        Map.of("schema", com.hrflow.contracts.request.RequestWorkflowFact.getClassSchema().toString())))).build(), HttpResponse.BodyHandlers.ofString());
        if (workflow.statusCode() != 200) throw new IllegalStateException("Request schema registration failed");
        p.add("hrflow.request.relay.job-user", () -> "request_job");
        p.add("hrflow.request.relay.job-password", () -> "job-test-pass");
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "request_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        p.add("spring.kafka.properties.schema.registry.url", RequestKafkaEnvironment::registry);
    }
}
