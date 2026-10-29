package com.hrflow.identityorg.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.identityorg.auth.setup.UatAccountRequest;
import com.hrflow.identityorg.auth.setup.UatAccountService;
import com.hrflow.identityorg.auth.service.AuthSessionService;
import com.hrflow.identityorg.organization.dto.EmployeeCreateRequest;
import com.hrflow.identityorg.organization.dto.NamedCreateRequest;
import com.hrflow.identityorg.organization.service.CatalogService;
import com.hrflow.identityorg.organization.service.EmployeeService;
import com.hrflow.identityorg.profile.entity.Tenant;
import com.hrflow.identityorg.profile.repository.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {"hrflow.auth.local-bootstrap=true", "spring.kafka.listener.auto-startup=false"})
@Testcontainers
abstract class UatAccountFixture {
    @TempDir static java.nio.file.Path keys;
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("identity-setup-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "identity_setup_runtime");
        p.add("spring.datasource.password", () -> "setup-test-only");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
        p.add("hrflow.auth.key-directory", () -> keys.toString());
    }
    @Autowired UatAccountService setup;
    @Autowired TenantRepository tenants;
    @Autowired CatalogService catalogs;
    @Autowired EmployeeService employees;
    @Autowired AuthSessionService sessions;
    @Autowired JwtDecoder decoder;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate runtime;

    protected Tenant tenant() { return tenants.saveAndFlush(new Tenant("setup-" + UUID.randomUUID().toString().substring(0, 8))); }
    protected UUID employee(Tenant tenant) {
        UUID actor = UUID.randomUUID();
        var department = catalogs.createDepartment(tenant.getId(), actor, UUID.randomUUID().toString(), new NamedCreateRequest("Operations", "OPS"));
        var position = catalogs.createPosition(tenant.getId(), actor, UUID.randomUUID().toString(), new NamedCreateRequest("Tester", "TEST"));
        return employees.create(tenant.getId(), actor, UUID.randomUUID().toString(), new EmployeeCreateRequest("Explicit Employee", "org@example.test", "UAT-1", department.id(), position.id(), null, null)).id();
    }
    protected UatAccountRequest request(Tenant tenant, UUID employee) { return new UatAccountRequest(tenant.getSlug(), employee, "login@example.test", List.of("EMPLOYEE", "HR_ADMIN"), "Asia/Phnom_Penh"); }
    protected JdbcTemplate admin() { return new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())); }
}
