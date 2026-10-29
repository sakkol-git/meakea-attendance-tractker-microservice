package com.hrflow.request;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RequestPayloadContractTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine").withInitScript("context-schema.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl); p.add("spring.datasource.username", () -> "request_app");
        p.add("spring.datasource.password", () -> "test-pass"); p.add("spring.flyway.user", POSTGRES::getUsername); p.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;

    @Test void canonicalMaterialItemsAreStoredAndForeignMaterialsCannotBeSmuggledIntoTheRequest() throws Exception {
        String tenant = id(), employee = id(), material = id(), foreignMaterial = id();
        var admin = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        admin.update("INSERT INTO request.material_item(id,tenant_id,code,name,category,active) VALUES (?,?,'OWN','Own material','EQUIPMENT',true),(?,?,'FOREIGN','Private','EQUIPMENT',true)", material, tenant, foreignMaterial, id());
        String body = "{\"title\":\"Equipment\",\"type\":\"MATERIAL\",\"reason\":\"UAT\",\"quantity\":2,\"items\":[{\"materialId\":\"" + material + "\",\"quantity\":2}]}";
        mvc.perform(post("/requests").with(actor(tenant, employee)).header("Idempotency-Key", id()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.items[0].materialId").value(material)).andExpect(jsonPath("$.lines[0].materialItemId").value(material));
        mvc.perform(post("/requests").with(actor(tenant, employee)).header("Idempotency-Key", id()).contentType(MediaType.APPLICATION_JSON).content(body.replace(material, foreignMaterial)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test void supportedDateAndTimeFieldsRoundTripAndInvalidRangesHaveNoCommittedRequest() throws Exception {
        String tenant = id(), employee = id();
        String body = "{\"title\":\"Late arrival\",\"type\":\"LATE_ARRIVAL\",\"reason\":\"UAT\",\"startDate\":\"2026-11-02\",\"endDate\":\"2026-11-02\",\"fromTime\":\"08:30\",\"toTime\":\"09:30\"}";
        mvc.perform(post("/requests").with(actor(tenant, employee)).header("Idempotency-Key", id()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.startDate").value("2026-11-02")).andExpect(jsonPath("$.toTime").value("09:30"));
        for (String invalid : List.of(body.replace("\"endDate\":\"2026-11-02\"", "\"endDate\":\"2026-11-01\""), body.replace("09:30", "08:00"), body.replace("08:30", "25:99")))
            mvc.perform(post("/requests").with(actor(tenant, employee)).header("Idempotency-Key", id()).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/requests/me").with(actor(tenant, employee)).param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].fromTime").value("08:30"));
    }
    @Test void unrelatedLegacyRoleNamesAndOwnerAloneCannotReadAnotherEmployeesPrivateRequest() throws Exception {
        String tenant = id(), employee = id();
        var created = mvc.perform(post("/requests").with(actor(tenant, employee)).header("Idempotency-Key", id())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Private\",\"type\":\"OTHER\",\"reason\":\"Private reason\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String request = tools.jackson.databind.json.JsonMapper.builder().build().readTree(created).path("id").asText();
        for (String role : List.of("ADMIN", "HR", "OWNER")) {
            mvc.perform(get("/requests/{id}", request).with(jwt().jwt(token -> token.subject(id()).claim("tenantId", tenant)
                    .claim("employeeId", id()).claim("roles", List.of(role)))))
                    .andExpect(status().isForbidden());
        }
    }

    private static String id() { return UUID.randomUUID().toString(); }
    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor actor(String tenant, String employee) {
        return jwt().jwt(token -> token.subject(id()).claim("tenantId", tenant).claim("employeeId", employee).claim("roles", List.of("EMPLOYEE")));
    }
}
