package com.hrflow.notification.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class NotificationReferenceTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl); p.add("spring.datasource.username", () -> "notification_app");
        p.add("spring.datasource.password", () -> "test-pass"); p.add("spring.flyway.user", POSTGRES::getUsername); p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired NotificationEmployeeConsumer consumer;
    @Autowired NotificationReferences references;
    @Autowired JdbcTemplate runtime;
    @Autowired TransactionTemplate transactions;
    @MockitoBean NotificationEmployeeBootstrap bootstrap;

    @Test void actualLinkedRolesAreMonotonicTenantScopedAndUnknownIsNeverHrAuthority() {
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true);
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), manager = UUID.randomUUID(), account = UUID.randomUUID();
        var fact = fact(tenant, employee, account, manager, List.of("EMPLOYEE", "HR_ADMIN"), 2, "ACTIVE");
        consumer.apply(tenant + ":" + employee, fact);
        consumer.apply(tenant + ":" + employee, fact);
        assertThat(references.employee(tenant, employee).account()).isEqualTo(account);
        assertThat(references.hr(tenant, manager)).extracting(NotificationReferences.Employee::employee).containsExactly(employee);
        assertThat(references.hr(tenant, employee)).isEmpty();
        consumer.apply(tenant + ":" + employee, fact(tenant, employee, account, manager, List.of("EMPLOYEE"), 3, "ACTIVE"));
        consumer.apply(tenant + ":" + employee, fact);
        assertThat(references.hr(tenant, manager)).isEmpty();
        UUID legacy = UUID.randomUUID();
        consumer.apply(tenant + ":" + legacy, fact(tenant, legacy, null, null, null, 1, "ACTIVE"));
        assertThatThrownBy(() -> references.employee(tenant, legacy)).isInstanceOf(org.springframework.web.ErrorResponseException.class);
        assertThatThrownBy(() -> references.employee(UUID.randomUUID(), employee)).isInstanceOf(org.springframework.web.ErrorResponseException.class);
        assertThat(runtime.queryForObject("SELECT count(*) FROM notification.employee_ref", Integer.class)).isZero();
        assertThatThrownBy(() -> consumer.apply(tenant + ":" + employee, fact(tenant, employee, account, manager, List.of("ADMIN"), 4, "ACTIVE")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(references.employee(tenant, employee).roles()).containsExactly("EMPLOYEE");
    }

    @Test void rollbackOrUnreadyReplayCannotEstablishRecipientAuthority() {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID();
        var fact = fact(tenant, employee, UUID.randomUUID(), null, List.of("EMPLOYEE"), 1, "ACTIVE");
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> { consumer.apply(tenant + ":" + employee, fact); throw new IllegalStateException("interrupt"); }))
                .isInstanceOf(IllegalStateException.class);
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(true);
        assertThatThrownBy(() -> references.employee(tenant, employee)).isInstanceOf(org.springframework.web.ErrorResponseException.class);
        consumer.apply(tenant + ":" + employee, fact);
        org.mockito.Mockito.when(bootstrap.isReady()).thenReturn(false);
        assertThatThrownBy(() -> references.employee(tenant, employee)).isInstanceOf(org.springframework.web.ErrorResponseException.class);
    }

    static OrganizationFact fact(UUID tenant, UUID employee, UUID account, UUID manager, List<String> roles, long revision, String status) {
        var snapshot = EmployeeSnapshot.newBuilder().setEmployeeId(employee.toString()).setEmployeeNo("SYNTHETIC").setFullName("Synthetic")
                .setStatus(status).setAssignments(List.of()).setManagerId(manager == null ? null : manager.toString())
                .setUserId(account == null ? null : account.toString()).setAccountRoles(roles).build();
        return new OrganizationFact(UUID.randomUUID().toString(), "org.employee.updated.v1", 1, tenant.toString(), employee.toString(), revision,
                Instant.now().toString(), UUID.randomUUID().toString(), null, snapshot);
    }
}
