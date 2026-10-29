package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0", "hrflow.reporting.employee-consumer.enabled=true",
        "hrflow.reporting.session-consumer.enabled=true", "spring.kafka.listener.auto-startup=true"})
class ManagerReportingKafkaIT extends SessionJourneyEnvironment {
    @LocalServerPort int port;
    @Autowired KafkaTemplate<Object, Object> kafka;

    @Test void startupReplaysOwnerEmployeesAndLiveReassignmentRevokesTeamReportAccess() throws Exception {
        String managerToken = token(TEAM_TENANT, TEAM_MANAGER, "MANAGER");
        // Owner snapshots were in real Kafka before this application started.
        assertThat(summary(managerToken).statusCode()).isEqualTo(200);
        var session = com.hrflow.contracts.attendance.AttendanceSessionFact.newBuilder(SessionProjectionTest.fact(1, null))
                .setTenantId(TEAM_TENANT.toString()).setEmployeeId(TEAM_EMPLOYEE.toString()).build();
        kafka.send("attendance.session.v1", TEAM_TENANT + ":" + TEAM_EMPLOYEE, session).get();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(JSON.readTree(summary(managerToken).body())
                .path("totalCount").asInt()).isEqualTo(1));
        var reassigned = ManagerReportScopeTest.employee(TEAM_TENANT, TEAM_EMPLOYEE, UUID.randomUUID(), 2);
        kafka.send("org.employee.v1", TEAM_TENANT + ":" + TEAM_EMPLOYEE, reassigned).get();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(JSON.readTree(summary(managerToken).body())
                .path("totalCount").asInt()).isZero());
    }

    private HttpResponse<String> summary(String token) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/reports/attendance/summary"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer " + token).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
