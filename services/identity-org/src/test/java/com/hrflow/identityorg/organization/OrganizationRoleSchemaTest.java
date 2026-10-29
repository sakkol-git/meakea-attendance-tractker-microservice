package com.hrflow.identityorg.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hrflow.contracts.organization.OrganizationFact;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
class OrganizationRoleSchemaTest {
    static final Network NETWORK = Network.newNetwork();
    @Container static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer("confluentinc/cp-kafka:8.2.0")
            .withNetwork(NETWORK).withNetworkAliases("kafka").withListener("kafka:19092");
    @Container static final GenericContainer<?> REGISTRY = new GenericContainer<>("confluentinc/cp-schema-registry:8.2.0")
            .withNetwork(NETWORK).withExposedPorts(8081).withEnv("SCHEMA_REGISTRY_HOST_NAME", "registry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    @Test void actualRegistryAcceptsRoleAuthorityAsFullTransitiveOptionalEvolution() throws Exception {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var json = JsonMapper.builder().build();
        String url = "http://" + REGISTRY.getHost() + ":" + REGISTRY.getMappedPort(8081);
        var configured = http.send(HttpRequest.newBuilder(URI.create(url + "/config/org.employee.v1-value"))
                .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                .PUT(HttpRequest.BodyPublishers.ofString("{\"compatibility\":\"FULL_TRANSITIVE\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(configured.statusCode()).as(configured.body()).isEqualTo(200);
        String original = Files.readString(Path.of("../../libs/hrflow-contracts/src/test/resources/organization-fact.original-v1.avsc"));
        for (String schema : java.util.List.of(original, OrganizationFact.getClassSchema().toString())) {
            await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
                var registered = http.send(HttpRequest.newBuilder(URI.create(url + "/subjects/org.employee.v1-value/versions"))
                        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("schema", schema)))).build(), HttpResponse.BodyHandlers.ofString());
                assertThat(registered.statusCode()).as(registered.body()).isEqualTo(200);
            });
        }
    }
}
