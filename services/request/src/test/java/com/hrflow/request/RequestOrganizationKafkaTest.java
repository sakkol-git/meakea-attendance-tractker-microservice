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

@SpringBootTest(properties = {"hrflow.request.employee-consumer.enabled=true", "spring.kafka.listener.auto-startup=true"})
@Testcontainers
class RequestOrganizationKafkaTest extends RequestKafkaEnvironment {
    @Autowired RequestService requests;
    @Autowired KafkaTemplate<Object, Object> kafka;

    @Test void realStartupReplayResolvesTheManagerAndLiveTerminationCannotBeUndoneByAnOlderSnapshot() throws Exception {
        var draft = requests.createDraftRequest(TENANT, EMPLOYEE, "Synthetic", CreateGeneralRequestDto.builder()
                .title("Private").type("OTHER").reason("Personal").quantity(1).build());
        var submitted = requests.submitRequest(TENANT, EMPLOYEE, draft.getId(), null, "0");
        assertThat(submitted.getSteps().getFirst().getApproverId()).isEqualTo(MANAGER);
        assertThat(requests.getInbox(TENANT, MANAGER, Set.of("MANAGER"))).hasSize(1);
        kafka.send("org.employee.v1", TENANT + ":" + MANAGER,
                RequestEmployeeReferenceTest.fact(TENANT, MANAGER, null, 2, "TERMINATED")).get();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThatThrownBy(() ->
                requests.approveRequestStep(TENANT, MANAGER, Set.of("MANAGER"), submitted.getId(), null,
                        Long.toString(submitted.getVersion()))).isInstanceOfSatisfying(ErrorResponseException.class,
                                error -> assertThat(error.getStatusCode().value()).isEqualTo(403)));
        kafka.send("org.employee.v1", TENANT + ":" + MANAGER,
                RequestEmployeeReferenceTest.fact(TENANT, MANAGER, null, 1, "ACTIVE")).get();
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThatThrownBy(() ->
                requests.getInbox(TENANT, MANAGER, Set.of("MANAGER"))).isInstanceOfSatisfying(ErrorResponseException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(403)));
        assertThat(requests.getRequestDetail(TENANT, EMPLOYEE, Set.of("EMPLOYEE"), submitted.getId()).getStatus()).isEqualTo("PENDING_APPROVAL");
    }
}
