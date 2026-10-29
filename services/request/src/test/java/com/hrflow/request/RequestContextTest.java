package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class RequestContextTest {
    @Container
    static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow").withUsername("hrflow").withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", database::getJdbcUrl);
        properties.add("spring.datasource.username", database::getUsername);
        properties.add("spring.datasource.password", database::getPassword);
        properties.add("spring.flyway.user", database::getUsername);
        properties.add("spring.flyway.password", database::getPassword);
    }

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired com.hrflow.request.service.RequestService requests;

    @Test
    void unknownEmployeeRelationshipCannotBeTreatedAsNoManagerOrClientChosenApprover() {
        var draft = requests.createDraftRequest("tenant-unknown", "employee-unknown", "Synthetic",
                com.hrflow.request.dto.CreateGeneralRequestDto.builder().title("Private").type("OTHER")
                        .reason("Personal").quantity(1).build());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> requests.submitRequest("tenant-unknown", "employee-unknown", draft.getId(), null, "0"))
                .isInstanceOfSatisfying(org.springframework.web.ErrorResponseException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
        assertThat(requests.getRequestDetail("tenant-unknown", "employee-unknown", java.util.Set.of("EMPLOYEE"), draft.getId()).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void startsJpaContextAgainstDisposablePostgres() {
        assertThat(entityManagerFactory).isNotNull();
    }
}
