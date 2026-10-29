package com.hrflow.reporting.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfidenceScoreService {

    private final RestClient confidenceWorkerRestClient;

    public record ConfidenceScoreRequest(
            UUID requestId,
            UUID tenantId,
            UUID employeeId,
            LocalDate workDate,
            Map<String, Object> telemetry,
            long aggregateRevision,
            boolean hasManualEvidence
    ) {}

    public record ConfidenceScoreResult(
            UUID requestId,
            UUID tenantId,
            String decision,
            double confidence,
            boolean applied,
            String source
    ) {}

    public record WorkerEvidence(
            String tenantId,
            String employeeId,
            String workDate,
            int in_geofence,
            double gps_accuracy_m,
            double minutes_in_zone,
            int wifi_connected,
            int ble_beacons_seen,
            int movement_events,
            int interaction_events,
            double minutes_after_shift_start,
            double arrival_deviation_min,
            int is_usual_device,
            int mock_location_flag,
            int present_days_last_14
    ) {}

    public record WorkerScoreResponse(
            String tenantId,
            String employeeId,
            String workDate,
            double confidence,
            String decision,
            String source,
            String modelVersion
    ) {}

    public ConfidenceScoreResult processScore(ConfidenceScoreRequest request) {
        throw new UnsupportedOperationException(
                "Confidence scoring executable integration is BLOCKED: requires an accepted durable state model defining reservation, result persistence, unknown outcomes, retry and reconciliation. Do not invoke the worker or return applied=true."
        );
    }

    public WorkerScoreResponse invokeWorkerAdapter(ConfidenceScoreRequest request) {
        // Validate required telemetry without fabricating values
        Map<String, Object> t = request.telemetry();
        if (t == null) {
            throw new IllegalArgumentException("Telemetry payload is required");
        }
        WorkerEvidence evidence = extractEvidence(request.tenantId(), request.employeeId(), request.workDate(), t);

        // Delegate scoring to the Python confidence worker via HTTP contract adapter
        WorkerScoreResponse response = confidenceWorkerRestClient.post()
                .uri("/score")
                .body(evidence)
                .retrieve()
                .body(WorkerScoreResponse.class);

        if (response == null) {
            throw new IllegalStateException("Confidence worker returned empty response");
        }

        // Validate worker response tenant, employee and work date against the request
        if (!request.tenantId().toString().equals(response.tenantId())
                || !request.employeeId().toString().equals(response.employeeId())
                || !request.workDate().toString().equals(response.workDate())) {
            throw new IllegalStateException("Confidence worker response mismatch: response tenant/employee/workDate does not match request");
        }

        return response;
    }

    private WorkerEvidence extractEvidence(UUID tenantId, UUID employeeId, LocalDate workDate, Map<String, Object> t) {
        return new WorkerEvidence(
                tenantId.toString(),
                employeeId.toString(),
                workDate.toString(),
                requireInt(t, "in_geofence"),
                requireDouble(t, "gps_accuracy_m"),
                requireDouble(t, "minutes_in_zone"),
                requireInt(t, "wifi_connected"),
                requireInt(t, "ble_beacons_seen"),
                requireInt(t, "movement_events"),
                requireInt(t, "interaction_events"),
                requireDouble(t, "minutes_after_shift_start"),
                requireDouble(t, "arrival_deviation_min"),
                requireInt(t, "is_usual_device"),
                requireInt(t, "mock_location_flag"),
                requireInt(t, "present_days_last_14")
        );
    }

    private static int requireInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number num) {
            return num.intValue();
        }
        throw new IllegalArgumentException("Missing or invalid required telemetry field: " + key);
    }

    private static double requireDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        throw new IllegalArgumentException("Missing or invalid required telemetry field: " + key);
    }
}
