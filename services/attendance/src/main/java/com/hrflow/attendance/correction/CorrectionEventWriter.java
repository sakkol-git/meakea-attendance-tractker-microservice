package com.hrflow.attendance.correction;

import com.hrflow.attendance.entity.AttendanceCorrection;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class CorrectionEventWriter {
    private final JdbcTemplate jdbc;
    private final CorrectionSnapshotMapper mapper;
    private final JsonMapper json;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(AttendanceCorrection correction) {
        String key = correction.getTenantId() + ":" + correction.getEmployeeId();
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", Object.class, "attendance.stream:" + key);
        jdbc.update("""
                INSERT INTO attendance.attendance_outbox(id,tenant_id,topic,event_id,aggregate_id,aggregate_revision,payload,
                    schema_version,event_type,correlation_id,occurred_at,event_key)
                VALUES (?,?,'attendance.correction.v1',?,?,?,?::jsonb,1,'AttendanceCorrectionSnapshot.v1',?,?,?)
                """, UUID.randomUUID(), correction.getTenantId(), UUID.randomUUID(), correction.getId(),
                Math.addExact(correction.getVersion(), 1L), json.writeValueAsString(mapper.snapshot(correction)),
                UUID.randomUUID(), Timestamp.from(clock.instant()), key);
    }
}
