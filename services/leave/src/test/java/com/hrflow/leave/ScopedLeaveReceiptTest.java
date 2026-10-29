package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hrflow.leave.service.CommandReceipts;
import com.hrflow.leave.service.TenantSqlScope;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class ScopedLeaveReceiptTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", () -> "leave_app");
        p.add("spring.datasource.password", () -> "test-pass");
        p.add("spring.flyway.user", POSTGRES::getUsername);
        p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired CommandReceipts receipts;
    @Autowired TenantSqlScope scope;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    @Test void realRestrictedRuntimeScopesReplaysAndRollsBackAnInterruptedCommand() {
        String tenant = UUID.randomUUID().toString(), actor = UUID.randomUUID().toString(), key = UUID.randomUUID().toString();
        assertThat(receipts.execute(tenant, actor, "/leave/requests", key, "body", String.class, () -> "original")).isEqualTo("original");
        assertThat(receipts.execute(tenant, actor, "/leave/requests", key, "body", String.class, () -> { throw new AssertionError("repeated"); })).isEqualTo("original");
        assertThat(receipts.execute(tenant, actor, "/leave/requests/other/cancel", key, "body", String.class, () -> "other-path")).isEqualTo("other-path");
        assertThat(receipts.execute(tenant, "another-account", "/leave/requests", key, "body", String.class, () -> "other-account")).isEqualTo("other-account");
        assertThat(receipts.execute("another-tenant", actor, "/leave/requests", key, "body", String.class, () -> "other-tenant")).isEqualTo("other-tenant");
        assertThatThrownBy(() -> receipts.execute(tenant, actor, "/leave/requests", key, "changed", String.class, () -> "bad"))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
        String interrupted = UUID.randomUUID().toString();
        assertThatThrownBy(() -> receipts.execute(tenant, actor, "/leave/requests", interrupted, "body", String.class,
                () -> { throw new IllegalStateException("interrupted"); })).isInstanceOf(IllegalStateException.class);
        assertThat(receipts.execute(tenant, actor, "/leave/requests", interrupted, "body", String.class, () -> "recovered")).isEqualTo("recovered");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.command_receipt_v2", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT rolsuper OR rolbypassrls FROM pg_roles WHERE rolname=current_user", Boolean.class)).isFalse();
    }

    @Test void historicalOutcomeWithoutActorOrPathRequiresReconciliationWithoutReexecution() {
        String tenant = UUID.randomUUID().toString(), key = UUID.randomUUID().toString();
        transactions.executeWithoutResult(tx -> {
            scope.use(tenant);
            jdbc.update("""
                    INSERT INTO leave.command_receipt(tenant_id,command_key,action,payload_hash,settled_resource_id,response_status,response_body)
                    VALUES (?,?,'SUBMIT_LEAVE_REQUEST','unknown','historical',201,'{}'::jsonb)
                    """, tenant, key);
        });
        assertThatThrownBy(() -> receipts.execute(tenant, "account", "/leave/requests", key, "body", String.class,
                () -> { throw new AssertionError("must not repeat historical command"); }))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
        transactions.executeWithoutResult(tx -> {
            scope.use(tenant);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.command_receipt", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM leave.command_receipt_v2", Integer.class)).isZero();
        });
    }
}
