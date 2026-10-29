package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hrflow.contracts.leave.LeaveRequestFact;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0", "hrflow.reporting.leave-consumer.enabled=true",
        "hrflow.reporting.export.worker.enabled=true", "hrflow.reporting.export.worker.poll-ms=50",
        "spring.kafka.listener.auto-startup=true"})
class LeaveReportingKafkaIT extends SessionJourneyEnvironment {
    @LocalServerPort int port;
    @Autowired KafkaTemplate<Object, Object> kafka;

    @Test void approvedLeaveConvergesAndReviewRetainsOverlayUntilCancellation() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), leave = UUID.randomUUID();
        String token = token(tenant, employee, "HR_ADMIN");
        var approved = fact(tenant, employee, leave, 1, "APPROVED");
        send(approved);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(active(token)).isEqualTo(1));
        assertPrivateCsvCompletes(token, tenant, employee, leave);
        send(fact(tenant, employee, leave, 2, "NEEDS_REVIEW"));
        send(approved); // Receipt duplicate, followed by an old revision with a new event identity.
        send(fact(tenant, employee, leave, 1, "CANCELLED"));
        send(fact(UUID.randomUUID(), employee, UUID.randomUUID(), 1, "APPROVED"));
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(active(token)).isEqualTo(1));
        send(fact(tenant, employee, leave, 3, "CANCELLED"));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(active(token)).isZero());
        send(fact(tenant, employee, leave, 2, "APPROVED"));
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(active(token)).isZero());
    }

    private void send(LeaveRequestFact fact) throws Exception {
        kafka.send("leave.request.v1", fact.getTenantId() + ":" + fact.getEmployeeId(), fact).get();
    }

    private long active(String token) throws Exception {
        var response = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/reports/dashboard"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer " + token).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body()).path("activeLeavesToday").asLong();
    }

    private void assertPrivateCsvCompletes(String token, UUID tenant, UUID employee, UUID leave) throws Exception {
        String base = "http://127.0.0.1:" + port + "/reports/exports";
        var created = HTTP.send(HttpRequest.newBuilder(URI.create(base)).timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"report\":\"LEAVE\",\"format\":\"CSV\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(created.statusCode()).isEqualTo(202);
        String id = JSON.readTree(created.body()).path("id").asText();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var status = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/" + id))
                    .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(JSON.readTree(status.body()).path("status").asText()).isEqualTo("COMPLETED");
        });
        var download = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/" + id + "/download"))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).contains("chargedDays", employee.toString(), leave.toString(), "APPROVED");
        var foreignRequester = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/" + id + "/download"))
                .header("Authorization", "Bearer " + token(tenant, UUID.randomUUID(), "HR_ADMIN"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(foreignRequester.statusCode()).isEqualTo(404);
    }

    static LeaveRequestFact fact(UUID tenant, UUID employee, UUID leave, long revision, String state) {
        var today = LocalDate.now();
        return LeaveRequestFact.newBuilder().setEventId(UUID.randomUUID().toString())
                .setType("LeaveRequestSnapshot.v1").setSchemaVersion(1).setTenantId(tenant.toString())
                .setAggregateId(leave.toString()).setAggregateVersion(revision).setOccurredAt(Instant.now())
                .setCorrelationId(UUID.randomUUID().toString()).setEmployeeId(employee.toString())
                .setLeaveType("ANNUAL").setStartDate(today).setEndDate(today).setPortion("FULL_DAY")
                .setChargedDays("1.00").setWorkingDates(List.of(today)).setState(state).build();
    }
}
