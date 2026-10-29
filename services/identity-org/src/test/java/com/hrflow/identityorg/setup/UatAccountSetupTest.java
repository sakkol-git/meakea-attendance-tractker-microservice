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

class UatAccountSetupTest extends UatAccountFixture {
    @Test void explicitlyLinkedAccountLogsInWithExactEmployeeRolesAndPublishesNoSecret() {
        var tenant = tenant();
        UUID employee = employee(tenant);
        var request = request(tenant, employee);
        char[] password = "synthetic-uat-password".toCharArray();
        UUID account;
        try { account = setup.provision(request, password); }
        finally { java.util.Arrays.fill(password, '\0'); }
        var jwt = decoder.decode(sessions.login("setup-fixture", tenant.getSlug(), request.email(), "synthetic-uat-password").accessToken());
        assertThat(jwt.getSubject()).isEqualTo(account.toString());
        assertThat(jwt.getClaimAsString("employeeId")).isEqualTo(employee.toString());
        assertThat(jwt.getClaimAsString("tenantId")).isEqualTo(tenant.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("EMPLOYEE", "HR_ADMIN");
        var admin = admin();
        String fact = admin.queryForObject("SELECT payload::text FROM identity_org.organization_outbox WHERE tenant_id=? AND aggregate_id=? ORDER BY aggregate_revision DESC LIMIT 1", String.class, tenant.getId(), employee);
        assertThat(fact).contains(account.toString(), "accountRoles", "HR_ADMIN").doesNotContain("synthetic-uat-password", "$2a$");
        String audit = admin.queryForObject("SELECT diff::text FROM identity_org.audit_log WHERE tenant_id=? AND action='UAT_ACCOUNT_CREATED'", String.class, tenant.getId());
        assertThat(audit).contains("PRIVATE_UAT_SETUP", "identity_setup_runtime").doesNotContain(request.email(), "synthetic-uat-password", "$2a$");
        assertThat(runtime.queryForObject("SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname=current_user", Boolean.class)).isFalse();
    }

    @Test void foreignOrInactiveEmployeeAndUnknownRolesCannotCreateAnAccount() {
        var tenant = tenant();
        UUID employee = employee(tenant);
        var foreign = tenant();
        assertThatThrownBy(() -> setup.provision(request(foreign, employee), "synthetic-password".toCharArray())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> setup.provision(new UatAccountRequest(tenant.getSlug(), employee, "one@example.test", List.of("EMPLOYEE", "ADMIN"), "Asia/Phnom_Penh"), "synthetic-password".toCharArray())).isInstanceOf(IllegalArgumentException.class);
        admin().update("UPDATE identity_org.employee SET status='TERMINATED' WHERE tenant_id=? AND id=?", tenant.getId(), employee);
        assertThatThrownBy(() -> setup.provision(request(tenant, employee), "synthetic-password".toCharArray())).isInstanceOf(IllegalArgumentException.class);
        assertThat(admin().queryForObject("SELECT count(*) FROM identity_org.auth_user WHERE tenant_id=?", Integer.class, tenant.getId())).isZero();
    }

    @Test void concurrentSetupKeepsExactlyOneActiveEmployeeAccount() throws Exception {
        var tenant = tenant();
        UUID employee = employee(tenant);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = java.util.stream.IntStream.range(0, 2).mapToObj(index -> workers.submit(() -> {
                start.await();
                try {
                    return (Object) setup.provision(new UatAccountRequest(tenant.getSlug(), employee, "concurrent-" + index + "@example.test", List.of("EMPLOYEE"), "UTC"), "synthetic-password".toCharArray());
                } catch (IllegalArgumentException conflict) { return (Object) conflict; }
            })).toList();
            start.countDown();
            var results = new java.util.ArrayList<Object>();
            for (var future : futures) results.add(future.get(15, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(results.stream().filter(UUID.class::isInstance).count()).isEqualTo(1);
            assertThat(results.stream().filter(IllegalArgumentException.class::isInstance).count()).isEqualTo(1);
        }
        assertThat(admin().queryForObject("SELECT count(*) FROM identity_org.auth_user WHERE tenant_id=?", Integer.class, tenant.getId())).isEqualTo(1);
        assertThat(admin().queryForObject("SELECT count(*) FROM identity_org.audit_log WHERE tenant_id=? AND action='UAT_ACCOUNT_CREATED'", Integer.class, tenant.getId())).isEqualTo(1);
    }

    @Test void aDuplicateOrInterruptedSetupCannotRelinkAnExistingAccountOrLeavePartialEffects() {
        var tenant = tenant();
        UUID employee = employee(tenant);
        var request = request(tenant, employee);
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
            setup.provision(request, "synthetic-password".toCharArray());
            throw new IllegalStateException("interrupt");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(admin().queryForObject("SELECT count(*) FROM identity_org.auth_user WHERE tenant_id=?", Integer.class, tenant.getId())).isZero();
        assertThat(admin().queryForObject("SELECT count(*) FROM identity_org.organization_outbox WHERE tenant_id=? AND aggregate_id=?", Integer.class, tenant.getId(), employee)).isEqualTo(1);
        assertThat(admin().queryForObject("SELECT count(*) FROM identity_org.audit_log WHERE tenant_id=? AND action='UAT_ACCOUNT_CREATED'", Integer.class, tenant.getId())).isZero();
        UUID account = setup.provision(request, "synthetic-password".toCharArray());
        assertThatThrownBy(() -> setup.provision(new UatAccountRequest(tenant.getSlug(), employee, "another@example.test", List.of("EMPLOYEE"), "UTC"), "synthetic-password".toCharArray())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> setup.provision(request, "different-password".toCharArray())).isInstanceOf(IllegalArgumentException.class);
        assertThat(admin().queryForObject("SELECT id FROM identity_org.auth_user WHERE tenant_id=?", UUID.class, tenant.getId())).isEqualTo(account);
        assertThat(admin().queryForObject("SELECT count(*) FROM identity_org.audit_log WHERE tenant_id=? AND action='UAT_ACCOUNT_CREATED'", Integer.class, tenant.getId())).isEqualTo(1);
    }

}
