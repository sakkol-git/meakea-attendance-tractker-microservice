package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hrflow.contracts.attendance.AttendanceDailyFact;
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
        "management.server.port=0", "hrflow.reporting.daily-consumer.enabled=true", "spring.kafka.listener.auto-startup=true"})
class DailyReportingKafkaIT extends SessionJourneyEnvironment {
    @LocalServerPort int port;
    @Autowired KafkaTemplate<Object, Object> kafka;

    @Test void finalizedDailyAbsenceConvergesAndAProvisionalOrStaleRecordCannotInventAnotherPerson() throws Exception {
        UUID tenant = UUID.randomUUID(), employee = UUID.randomUUID(), day = UUID.randomUUID();
        String token = token(tenant, employee, "HR_ADMIN");
        send(fact(tenant, employee, day, 1, false));
        assertThat(absent(token)).isZero();
        send(fact(tenant, employee, day, 2, true));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(absent(token)).isEqualTo(1));
        send(fact(tenant, employee, day, 1, false));
        send(fact(UUID.randomUUID(), employee, UUID.randomUUID(), 1, true));
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(absent(token)).isEqualTo(1));
    }

    private long absent(String token) throws Exception {
        var response = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/reports/dashboard"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body()).path("totalAbsentToday").asLong();
    }
    private void send(AttendanceDailyFact fact) throws Exception {
        kafka.send("attendance.daily-status.v1", fact.getTenantId() + ":" + fact.getEmployeeId(), fact).get();
    }
    static AttendanceDailyFact fact(UUID tenant, UUID employee, UUID day, long revision, boolean finalized) {
        return AttendanceDailyFact.newBuilder().setEventId(UUID.randomUUID().toString()).setType("AttendanceDailySnapshot.v1")
                .setSchemaVersion(1).setTenantId(tenant.toString()).setAggregateId(day.toString()).setAggregateVersion(revision)
                .setOccurredAt(Instant.now()).setCorrelationId(UUID.randomUUID().toString()).setEmployeeId(employee.toString())
                .setWorkDate(LocalDate.now(java.time.ZoneId.of("Asia/Phnom_Penh"))).setTimezone("Asia/Phnom_Penh")
                .setStatus(finalized ? "ABSENT" : "PENDING").setDayType("WORKING_DAY").setLate(false).setEarlyDeparture(false)
                .setReviewRequired(false).setFinalized(finalized).setOpenSession(false).setMinutesWorked(0).setMinutesLate(0)
                .setExpectedStart(Instant.now().minusSeconds(9 * 3600L)).setExpectedEnd(Instant.now().minusSeconds(3600))
                .setShiftId(UUID.randomUUID().toString()).setShiftRevision(1).setLeaveReferences(List.of()).setSessionReferences(List.of()).build();
    }
}
