package com.hrflow.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.notification.entity.NotificationRecord;
import com.hrflow.notification.repository.NotificationRecordRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@Testcontainers
class ExposeOwnInboxPreferencesTest {

    @Container
    static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("hrflow")
            .withUsername("hrflow")
            .withPassword("test-only")
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
    MockMvc mvc;

    @Autowired
    NotificationRecordRepository recordRepository;

    private JwtRequestPostProcessor employee(UUID tenantId, UUID employeeId) {
        return jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("employeeId", employeeId.toString())
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
    }

    @Test
    @DisplayName("Unauthenticated requests are denied across all notification endpoints")
    void unauthenticatedRequestsDenied() throws Exception {
        UUID randomId = UUID.randomUUID();
        mvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/notifications/unread-count")).andExpect(status().isUnauthorized());
        mvc.perform(post("/notifications/{id}/read", randomId)).andExpect(status().isUnauthorized());
        mvc.perform(post("/notifications/read-all")).andExpect(status().isUnauthorized());
        mvc.perform(get("/notifications/preferences")).andExpect(status().isUnauthorized());
        mvc.perform(put("/notifications/preferences")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Missing tenant claim is denied with 403 Forbidden")
    void missingTenantClaimDenied() throws Exception {
        UUID employeeId = UUID.randomUUID();
        JwtRequestPostProcessor noTenant = jwt().jwt(token -> token
                .subject(employeeId.toString())
                .claim("employeeId", employeeId.toString())
                .claim("roles", List.of("EMPLOYEE"))
        ).authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        mvc.perform(get("/notifications").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/notifications/unread-count").with(noTenant)).andExpect(status().isForbidden());
        mvc.perform(get("/notifications/preferences").with(noTenant)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tenant B cannot enumerate or read Tenant A notifications")
    void crossTenantIsolationVerified() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();

        // Create 2 notifications for Employee A in Tenant A
        NotificationRecord rec1 = recordRepository.save(new NotificationRecord(
                tenantA, employeeA, "Shift Assigned", "You are assigned to Day Shift", "shift-1", "SHIFT"
        ));
        NotificationRecord rec2 = recordRepository.save(new NotificationRecord(
                tenantA, employeeA, "Attendance Alert", "Check-in recorded at 09:00", "att-1", "ATTENDANCE"
        ));

        // Employee A sees both notifications and count = 2
        mvc.perform(get("/notifications").with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].title").exists())
                .andExpect(jsonPath("$.items[1].title").exists());

        mvc.perform(get("/notifications/unread-count").with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.unreadCount").value(2));

        // Employee B under Tenant B sees total = 0 and count = 0
        mvc.perform(get("/notifications").with(employee(tenantB, employeeB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        mvc.perform(get("/notifications/unread-count").with(employee(tenantB, employeeB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.unreadCount").value(0));

        // Employee B attempting to read Tenant A's notification gets 404
        mvc.perform(post("/notifications/{id}/read", rec1.getId()).with(employee(tenantB, employeeB)))
                .andExpect(status().isNotFound());

        // Employee A marks rec1 as read
        mvc.perform(post("/notifications/{id}/read", rec1.getId()).with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rec1.getId().toString()))
                .andExpect(jsonPath("$.read").value(true));

        // Unread count for Employee A is now 1
        mvc.perform(get("/notifications/unread-count").with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // Employee A marks all read
        mvc.perform(post("/notifications/read-all").with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Unread count for Employee A is now 0
        mvc.perform(get("/notifications/unread-count").with(employee(tenantA, employeeA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("Notification preferences manage optimistic concurrency with ETag and If-Match")
    void preferencesOptimisticConcurrency() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID emp = UUID.randomUUID();

        // 1. Get default preferences: version 0, ETag "0"
        mvc.perform(get("/notifications/preferences").with(employee(tenant, emp)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.email").value(true))
                .andExpect(jsonPath("$.emailNotifications").value(true))
                .andExpect(jsonPath("$.inApp").value(true))
                .andExpect(jsonPath("$.pushNotifications").value(true))
                .andExpect(jsonPath("$.reminders").value(true))
                .andExpect(jsonPath("$.digestFrequency").value("DAILY"))
                .andExpect(jsonPath("$.version").value(0));

        // 2. Update with matching If-Match: "0" -> version 1
        String updateBody = """
                {
                    "email": false,
                    "inApp": true,
                    "reminders": false,
                    "digestFrequency": "WEEKLY"
                }
                """;

        mvc.perform(put("/notifications/preferences").with(employee(tenant, emp))
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.email").value(false))
                .andExpect(jsonPath("$.reminders").value(false))
                .andExpect(jsonPath("$.digestFrequency").value("WEEKLY"))
                .andExpect(jsonPath("$.version").value(1));

        // 3. Stale update with If-Match: "0" -> 412 Precondition Failed
        mvc.perform(put("/notifications/preferences").with(employee(tenant, emp))
                .header("If-Match", "\"0\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
                .andExpect(status().isPreconditionFailed());
    }
}
