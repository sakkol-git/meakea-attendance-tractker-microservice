package com.hrflow.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class CreateRequestAdvisoryCatalogueTest {

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
    @DisplayName("Negative stock and invalid quantities or statuses are rejected by PostgreSQL check constraints")
    void negativeStockAndInvalidQuantitiesRejectedByConstraints() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.execute("SET app.tenant_id = 'tenant-alpha'");
            }

            // 1. Negative advisory stock is rejected by chk_material_stock_nonneg
            String insertNegativeStock = "INSERT INTO request.material_item "
                    + "(id, tenant_id, code, name, category, advisory_stock, unit, active) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertNegativeStock)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, "tenant-alpha");
                ps.setString(3, "LAPTOP-01");
                ps.setString(4, "Standard Laptop");
                ps.setString(5, "EQUIPMENT");
                ps.setInt(6, -1); // Negative stock!
                ps.setString(7, "UNIT");
                ps.setBoolean(8, true);

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_material_stock_nonneg");
            }
            conn.rollback();

            // 2. Zero or negative request quantity is rejected by chk_request_quantity
            String insertInvalidQuantity = "INSERT INTO request.general_request "
                    + "(id, tenant_id, employee_id, employee_name, type, title, reason, status, quantity, version) "
                    + "VALUES (?, ?, ?, ?, 'MATERIAL', 'Need mouse', 'Broken mouse', 'DRAFT', 0, 0)";
            try (PreparedStatement ps = conn.prepareStatement(insertInvalidQuantity)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, "tenant-alpha");
                ps.setString(3, "emp-001");
                ps.setString(4, "John Doe");

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_request_quantity");
            }
            conn.rollback();

            // 3. Invalid request status rejected by chk_request_status
            String insertInvalidStatus = "INSERT INTO request.general_request "
                    + "(id, tenant_id, employee_id, employee_name, type, title, reason, status, quantity, version) "
                    + "VALUES (?, ?, ?, ?, 'OTHER', 'Special request', 'Just because', 'INVALID_STATUS', 1, 0)";
            try (PreparedStatement ps = conn.prepareStatement(insertInvalidStatus)) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, "tenant-alpha");
                ps.setString(3, "emp-001");
                ps.setString(4, "John Doe");

                assertThatThrownBy(ps::executeUpdate)
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_request_status");
            }
            conn.rollback();
        }
    }

    @Test
    @DisplayName("Cross-tenant catalogue and request items are isolated by PostgreSQL Row Level Security")
    void crossTenantCatalogueAndRequestsIsolatedByRls() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // Populate tenant-alpha material and request
            try (Statement st = conn.createStatement()) {
                st.execute("SET app.tenant_id = 'tenant-alpha'");
            }
            String matId = UUID.randomUUID().toString();
            String reqId = UUID.randomUUID().toString();

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO request.material_item (id, tenant_id, code, name, category, advisory_stock, unit, active) "
                            + "VALUES (?, 'tenant-alpha', 'MONITOR-27', '27 inch Monitor', 'HARDWARE', 5, 'UNIT', true)")) {
                ps.setString(1, matId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO request.general_request (id, tenant_id, employee_id, employee_name, type, title, reason, status, quantity, version) "
                            + "VALUES (?, 'tenant-alpha', 'emp-alpha', 'Alpha Employee', 'OTHER', 'Alpha Office supplies', 'Relocation', 'DRAFT', 1, 0)")) {
                ps.setString(1, reqId);
                ps.executeUpdate();
            }
            conn.commit();

            // Under request_app role, tenant-beta cannot see tenant-alpha material or request
            try (Statement st = conn.createStatement()) {
                st.execute("SET ROLE request_app");
                st.execute("SET app.tenant_id = 'tenant-beta'");
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM request.material_item WHERE id = '" + matId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isZero();
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM request.general_request WHERE id = '" + reqId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isZero();
                }

                // But tenant-alpha can see both
                st.execute("SET app.tenant_id = 'tenant-alpha'");
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM request.material_item WHERE id = '" + matId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1L);
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM request.general_request WHERE id = '" + reqId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1L);
                }
                st.execute("RESET ROLE");
            }
            conn.commit();
        }
    }

    @Test
    @DisplayName("Stock advisory is not a reservation promise and OTHER requests create no material lines")
    void advisoryStockIsNotReservationPromiseAndOtherTypeCreatesNoLines() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                st.execute("SET app.tenant_id = 'tenant-gamma'");
            }

            // Material with advisory stock = 2
            String matId = UUID.randomUUID().toString();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO request.material_item (id, tenant_id, code, name, category, advisory_stock, unit, active) "
                            + "VALUES (?, 'tenant-gamma', 'KEYBOARD-MECH', 'Mechanical Keyboard', 'EQUIPMENT', 2, 'UNIT', true)")) {
                ps.setString(1, matId);
                ps.executeUpdate();
            }

            // Create OTHER general request requesting quantity 10
            String reqId = UUID.randomUUID().toString();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO request.general_request (id, tenant_id, employee_id, employee_name, type, title, reason, status, quantity, version) "
                            + "VALUES (?, 'tenant-gamma', 'emp-gamma', 'Gamma Employee', 'OTHER', 'Budget adjustment', 'Fiscal year', 'DRAFT', 10, 0)")) {
                ps.setString(1, reqId);
                ps.executeUpdate();
            }
            conn.commit();

            // Verify advisory stock remains unchanged (advisory stock is not a hard balance deduction or reservation lock)
            try (Statement st = conn.createStatement()) {
                st.execute("SET app.tenant_id = 'tenant-gamma'");
                try (ResultSet rs = st.executeQuery("SELECT advisory_stock FROM request.material_item WHERE id = '" + matId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(2); // Unchanged!
                }

                // Verify request_line has 0 lines for this OTHER request
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM request.request_line WHERE request_id = '" + reqId + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isZero();
                }
            }
            conn.commit();
        }
    }
}
