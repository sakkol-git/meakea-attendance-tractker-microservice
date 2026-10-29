package com.hrflow.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.hrflow.reporting.service.ConfidenceScoreService;
import com.hrflow.reporting.service.ConfidenceScoreService.ConfidenceScoreRequest;
import com.hrflow.reporting.service.ConfidenceScoreService.ConfidenceScoreResult;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Testcontainers
class ConnectExistingConfidenceWorkerTest {

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

    private MockRestServiceServer mockServer;
    private ConfidenceScoreService scoreService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://confidence-worker:8000");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        scoreService = new ConfidenceScoreService(restClient);
    }

    @Test
    @DisplayName("Mocked Adapter: Connects existing confidence worker via HTTP contract adapter")
    void highConfidenceScoresAutoInferredViaContractAdapter() {
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Map<String, Object> telemetry = Map.ofEntries(
                Map.entry("in_geofence", 1),
                Map.entry("gps_accuracy_m", 10.0),
                Map.entry("minutes_in_zone", 35.0),
                Map.entry("wifi_connected", 1),
                Map.entry("ble_beacons_seen", 3),
                Map.entry("movement_events", 15),
                Map.entry("interaction_events", 7),
                Map.entry("minutes_after_shift_start", 0.0),
                Map.entry("arrival_deviation_min", 0.0),
                Map.entry("is_usual_device", 1),
                Map.entry("mock_location_flag", 0),
                Map.entry("present_days_last_14", 10)
        );

        ConfidenceScoreRequest req = new ConfidenceScoreRequest(
                requestId, tenantId, employeeId, LocalDate.of(2026, 10, 20), telemetry, 1L, false
        );

        // Expect POST /score to be called on the Python confidence worker with exact json structure
        mockServer.expect(requestTo("http://confidence-worker:8000/score"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.in_geofence").value(1))
                .andExpect(jsonPath("$.wifi_connected").value(1))
                .andExpect(jsonPath("$.mock_location_flag").value(0))
                .andRespond(withSuccess("""
                        {
                          "tenantId": "%s",
                          "employeeId": "%s",
                          "workDate": "2026-10-20",
                          "confidence": 0.9450,
                          "decision": "AUTO_INFER",
                          "source": "model",
                          "modelVersion": "rules-only"
                        }
                        """.formatted(tenantId, employeeId), MediaType.APPLICATION_JSON));

        // Calls worker via contract adapter
        ConfidenceScoreService.WorkerScoreResponse res = scoreService.invokeWorkerAdapter(req);
        assertThat(res.decision()).isEqualTo("AUTO_INFER");
        assertThat(res.confidence()).isEqualTo(0.9450);
        assertThat(res.source()).isEqualTo("model");
        mockServer.verify();
    }

    @Test
    @DisplayName("Blocked: Executable processScore throws UnsupportedOperationException until durable state model is accepted")
    void executableIntegrationIsBlocked() {
        ConfidenceScoreRequest req = new ConfidenceScoreRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 10, 20),
                Map.of(), 1L, false
        );

        assertThatThrownBy(() -> scoreService.processScore(req))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Confidence scoring executable integration is BLOCKED");
    }

    @Test
    @DisplayName("Validation: Missing required telemetry field throws IllegalArgumentException without fabricating defaults")
    void missingTelemetryFieldThrowsException() {
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        // Missing required fields
        ConfidenceScoreRequest req = new ConfidenceScoreRequest(
                UUID.randomUUID(), tenantId, employeeId, LocalDate.of(2026, 10, 20),
                Map.of("in_geofence", 1),
                1L, false
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> scoreService.invokeWorkerAdapter(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing or invalid required telemetry field");
    }

    @Test
    @DisplayName("Validation: Worker response with foreign tenant/employee/workDate is rejected")
    void workerResponseMismatchThrowsException() {
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Map<String, Object> telemetry = Map.ofEntries(
                Map.entry("in_geofence", 1),
                Map.entry("gps_accuracy_m", 10.0),
                Map.entry("minutes_in_zone", 35.0),
                Map.entry("wifi_connected", 1),
                Map.entry("ble_beacons_seen", 3),
                Map.entry("movement_events", 15),
                Map.entry("interaction_events", 7),
                Map.entry("minutes_after_shift_start", 0.0),
                Map.entry("arrival_deviation_min", 0.0),
                Map.entry("is_usual_device", 1),
                Map.entry("mock_location_flag", 0),
                Map.entry("present_days_last_14", 10)
        );

        ConfidenceScoreRequest req = new ConfidenceScoreRequest(
                requestId, tenantId, employeeId, LocalDate.of(2026, 10, 20), telemetry, 1L, false
        );

        // Worker returns a response belonging to a different employee
        mockServer.expect(requestTo("http://confidence-worker:8000/score"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "tenantId": "%s",
                          "employeeId": "%s",
                          "workDate": "2026-10-20",
                          "confidence": 0.90,
                          "decision": "AUTO_INFER",
                          "source": "model",
                          "modelVersion": "rules-only"
                        }
                        """.formatted(tenantId, UUID.randomUUID()), MediaType.APPLICATION_JSON));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> scoreService.invokeWorkerAdapter(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("response tenant/employee/workDate does not match request");
    }
}
