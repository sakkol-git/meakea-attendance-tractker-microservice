package com.hrflow.leave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
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
class CreateTypePolicyBalanceLedgerTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
            .withInitScript("context-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("Negative balance and invalid portions or dates are rejected by PostgreSQL check constraints")
    void negativeBalanceAndInvalidPortionsRejectedByConstraints() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.execute("SET app.tenant_id = 'tenant-alpha'");
            }

            // 1. Negative available balance is rejected by chk_leave_balance_available_nonneg
            String insertNegativeBalance = "INSERT INTO leave.leave_balance "
                    + "(id, tenant_id, employee_id, leave_type_code, year, allocated, used, pending, available, version) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertNegativeBalance)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, "tenant-alpha");
                ps.setString(3, "emp-001");
                ps.setString(4, "ANNUAL");
                ps.setInt(5, 2026);
                ps.setBigDecimal(6, new BigDecimal("10.00"));
                ps.setBigDecimal(7, BigDecimal.ZERO);
                ps.setBigDecimal(8, BigDecimal.ZERO);
                ps.setBigDecimal(9, new BigDecimal("-1.00")); // Negative available!
                ps.setLong(10, 0L);

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_leave_balance_available_nonneg");
            }
            conn.rollback();

            // 2. Inverted dates (end_date < start_date) rejected by chk_leave_request_dates
            String insertInvertedDates = "INSERT INTO leave.leave_request "
                    + "(id, tenant_id, employee_id, type, start_date, end_date, portion, days, status, version) "
                    + "VALUES (?, ?, ?, ?, '2026-06-10', '2026-06-05', 'FULL_DAY', 1.0, 'PENDING', 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertInvertedDates)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, "tenant-alpha");
                ps.setString(3, "emp-001");
                ps.setString(4, "ANNUAL");

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_leave_request_dates");
            }
            conn.rollback();

            // 3. Invalid portion rejected by chk_leave_request_portion
            String insertInvalidPortion = "INSERT INTO leave.leave_request "
                    + "(id, tenant_id, employee_id, type, start_date, end_date, portion, days, status, version) "
                    + "VALUES (?, ?, ?, ?, '2026-06-05', '2026-06-05', 'NIGHT_SHIFT', 0.5, 'PENDING', 1)";
            try (PreparedStatement ps = conn.prepareStatement(insertInvalidPortion)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, "tenant-alpha");
                ps.setString(3, "emp-001");
                ps.setString(4, "ANNUAL");

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_leave_request_portion");
            }
            conn.rollback();
        }
    }

    @Test
    @DisplayName("Cross-tenant balance references and queries are isolated by PostgreSQL Row Level Security")
    void crossTenantBalancesIsolatedByRls() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // Populate tenant-alpha leave balance
            try (Statement st = conn.createStatement()) {
                st.execute("SET app.tenant_id = 'tenant-alpha'");
            }
            String insertBalance = "INSERT INTO leave.leave_balance "
                    + "(id, tenant_id, employee_id, leave_type_code, year, allocated, used, pending, available, version) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String balanceId = UUID.randomUUID().toString();
            try (PreparedStatement ps = conn.prepareStatement(insertBalance)) {
                ps.setString(1, balanceId);
                ps.setString(2, "tenant-alpha");
                ps.setString(3, "emp-alpha-1");
                ps.setString(4, "ANNUAL");
                ps.setInt(5, 2026);
                ps.setBigDecimal(6, new BigDecimal("15.00"));
                ps.setBigDecimal(7, BigDecimal.ZERO);
                ps.setBigDecimal(8, BigDecimal.ZERO);
                ps.setBigDecimal(9, new BigDecimal("15.00"));
                ps.setLong(10, 0L);
                ps.executeUpdate();
            }
            conn.commit();

            // Under leave_app role, tenant-beta cannot see tenant-alpha balance
            try (Statement st = conn.createStatement()) {
                st.execute("SET ROLE leave_app");
                st.execute("SET app.tenant_id = 'tenant-beta'");
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM leave.leave_balance WHERE employee_id = 'emp-alpha-1'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isZero();
                }

                // But tenant-alpha can see it
                st.execute("SET app.tenant_id = 'tenant-alpha'");
                try (ResultSet rs = st.executeQuery("SELECT available FROM leave.leave_balance WHERE id = '" + balanceId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getBigDecimal(1)).isEqualByComparingTo(new BigDecimal("15.00"));
                }
                st.execute("RESET ROLE");
            }
            conn.commit();
        }
    }
}
