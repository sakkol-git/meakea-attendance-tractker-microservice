package com.hrflow.attendance.organization;

import com.hrflow.contracts.organization.EmployeeSnapshot;
import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.contracts.organization.ShiftSnapshot;
import java.time.Instant;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Apply a complete owner snapshot and its receipt in one attendance transaction. */
@Service
@RequiredArgsConstructor
public class OrganizationProjectionService {
    private static final String CONSUMER = "attendance-org-v1";
    private static final Map<String, Set<String>> TYPES = Map.of(
            "org.employee.v1", Set.of("org.employee.created.v1", "org.employee.updated.v1",
                    "org.employee.terminated.v1"),
            "org.shift.v1", Set.of("org.shift.defined.v1", "org.shift.updated.v1",
                    "org.shift.removed.v1"),
            "org.holiday.v1", Set.of("org.holiday.added.v1", "org.holiday.updated.v1",
                    "org.holiday.removed.v1"));

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final com.hrflow.attendance.daily.HolidayDailyChanges holidayChanges;
    private final com.hrflow.attendance.daily.ScheduleDailyChanges scheduleChanges;

    @Transactional
    public Result apply(String topic, String key, OrganizationFact fact) {
        Envelope envelope = validate(topic, key, fact);
        jdbc.queryForObject("SELECT set_config('app.tenant_id', ?, true)", String.class,
                envelope.tenant().toString());
        int receipt = jdbc.update("""
                INSERT INTO attendance.organization_inbox
                    (consumer_name, tenant_id, event_id, topic, aggregate_id, aggregate_revision)
                VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING
                """, CONSUMER, envelope.tenant(), envelope.event(), topic,
                envelope.aggregate(), fact.getAggregateVersion());
        if (receipt == 0) return Result.DUPLICATE;
        int newer = jdbc.update("""
                INSERT INTO attendance.organization_revision(tenant_id, topic, aggregate_id, revision)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (tenant_id, topic, aggregate_id)
                DO UPDATE SET revision = EXCLUDED.revision
                WHERE attendance.organization_revision.revision < EXCLUDED.revision
                """, envelope.tenant(), topic, envelope.aggregate(), fact.getAggregateVersion());
        if (newer == 0) return Result.STALE;
        switch (topic) {
            case "org.employee.v1" -> {
                employee(envelope, (EmployeeSnapshot) fact.getPayload());
                scheduleChanges.record(envelope.tenant(), envelope.event(), "EMPLOYEE", envelope.aggregate());
            }
            case "org.shift.v1" -> {
                shift(envelope, (ShiftSnapshot) fact.getPayload());
                scheduleChanges.record(envelope.tenant(), envelope.event(), "SHIFT", envelope.aggregate());
            }
            case "org.holiday.v1" -> holiday(envelope, (HolidaySnapshot) fact.getPayload());
            default -> throw new IllegalArgumentException("Unknown organization topic");
        }
        return Result.APPLIED;
    }

    private Envelope validate(String topic, String key, OrganizationFact fact) {
        if (fact == null || fact.getSchemaVersion() != 1 || fact.getAggregateVersion() < 1
                || !TYPES.getOrDefault(topic, Set.of()).contains(fact.getType())) {
            throw new IllegalArgumentException("Unsupported organization fact");
        }
        UUID tenant = uuid(fact.getTenantId());
        UUID aggregate = uuid(fact.getAggregateId());
        UUID event = uuid(fact.getEventId());
        uuid(fact.getCorrelationId());
        if (fact.getCausationId() != null) uuid(fact.getCausationId());
        Instant.parse(fact.getOccurredAt());
        if (!(tenant + ":" + aggregate).equals(key)) {
            throw new IllegalArgumentException("Organization fact key mismatch");
        }
        Object payload = fact.getPayload();
        boolean expected = switch (topic) {
            case "org.employee.v1" -> payload instanceof EmployeeSnapshot employee
                    && aggregate.equals(uuid(employee.getEmployeeId()));
            case "org.shift.v1" -> payload instanceof ShiftSnapshot shift
                    && aggregate.equals(uuid(shift.getShiftId()));
            case "org.holiday.v1" -> payload instanceof HolidaySnapshot holiday
                    && aggregate.equals(uuid(holiday.getHolidayId()));
            default -> false;
        };
        if (!expected) throw new IllegalArgumentException("Organization fact payload mismatch");
        return new Envelope(tenant, aggregate, event, fact.getAggregateVersion());
    }

    private void employee(Envelope envelope, EmployeeSnapshot fact) {
        if (!Set.of("ACTIVE", "INACTIVE", "TERMINATED").contains(fact.getStatus())) {
            throw new IllegalArgumentException("Unknown employee status");
        }
        List<Map<String, Object>> assignments = new ArrayList<>();
        for (var assignment : fact.getAssignments()) {
            uuid(assignment.getAssignmentId());
            uuid(assignment.getShiftId());
            LocalDate from = LocalDate.parse(assignment.getValidFrom());
            if (assignment.getValidTo() != null
                    && LocalDate.parse(assignment.getValidTo()).isBefore(from)) {
                throw new IllegalArgumentException("Invalid assignment period");
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("assignmentId", assignment.getAssignmentId());
            item.put("shiftId", assignment.getShiftId());
            item.put("validFrom", assignment.getValidFrom());
            item.put("validTo", assignment.getValidTo());
            assignments.add(item);
        }
        jdbc.update("""
                INSERT INTO attendance.employee_projection
                    (tenant_id, employee_id, employee_no, full_name, status, assignments, revision,manager_id,manager_known)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?,?,true)
                ON CONFLICT (tenant_id, employee_id) DO UPDATE SET
                    employee_no = EXCLUDED.employee_no, full_name = EXCLUDED.full_name,
                    status = EXCLUDED.status, assignments = EXCLUDED.assignments,
                    revision = EXCLUDED.revision,manager_id=EXCLUDED.manager_id,manager_known=true
                """, envelope.tenant(), envelope.aggregate(), fact.getEmployeeNo(),
                fact.getFullName(), fact.getDeletedAt() == null ? fact.getStatus() : "INACTIVE",
                json.writeValueAsString(assignments), envelope.revision(),
                fact.getManagerId() == null ? null : uuid(fact.getManagerId()));
    }

    private void shift(Envelope envelope, ShiftSnapshot fact) {
        LocalTime start = LocalTime.parse(fact.getStartTime());
        LocalTime end = LocalTime.parse(fact.getEndTime());
        ZoneId.of(fact.getTimezone());
        if (!end.isAfter(start) || fact.getGraceMinutes() < 0 || fact.getWorkingDays() < 1
                || fact.getWorkingDays() > 127 || fact.getSite().getRadiusM() <= 0) {
            throw new IllegalArgumentException("Invalid shift snapshot");
        }
        var site = fact.getSite();
        jdbc.update("""
                INSERT INTO attendance.shift_projection
                    (tenant_id, shift_id, name, start_time, end_time, timezone,
                     grace_minutes, working_days, site_id, site_name, latitude, longitude,
                     radius_m, deleted_at, revision)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, shift_id) DO UPDATE SET
                    name = EXCLUDED.name, start_time = EXCLUDED.start_time,
                    end_time = EXCLUDED.end_time, timezone = EXCLUDED.timezone,
                    grace_minutes = EXCLUDED.grace_minutes, working_days = EXCLUDED.working_days,
                    site_id = EXCLUDED.site_id, site_name = EXCLUDED.site_name,
                    latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude,
                    radius_m = EXCLUDED.radius_m, deleted_at = EXCLUDED.deleted_at,
                    revision = EXCLUDED.revision
                """, envelope.tenant(), envelope.aggregate(), fact.getName(), start, end,
                fact.getTimezone(), fact.getGraceMinutes(), fact.getWorkingDays(),
                uuid(site.getSiteId()), site.getName(), site.getLatitude(), site.getLongitude(),
                site.getRadiusM(), instant(fact.getDeletedAt()), envelope.revision());
    }

    private void holiday(Envelope envelope, HolidaySnapshot fact) {
        if (!Set.of("PUBLIC", "COMPANY").contains(fact.getKind())) {
            throw new IllegalArgumentException("Unknown holiday kind");
        }
        var previousDates = jdbc.queryForList("SELECT date FROM attendance.holiday_projection WHERE tenant_id=? AND holiday_id=?",
                LocalDate.class, envelope.tenant(), envelope.aggregate());
        jdbc.update("""
                INSERT INTO attendance.holiday_projection
                    (tenant_id, holiday_id, date, name, kind, deleted_at, revision)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, holiday_id) DO UPDATE SET
                    date = EXCLUDED.date, name = EXCLUDED.name, kind = EXCLUDED.kind,
                    deleted_at = EXCLUDED.deleted_at, revision = EXCLUDED.revision
                """, envelope.tenant(), envelope.aggregate(), LocalDate.parse(fact.getDate()),
                fact.getName(), fact.getKind(), instant(fact.getDeletedAt()), envelope.revision());
        holidayChanges.record(envelope.tenant(), envelope.event(), previousDates.isEmpty() ? null : previousDates.getFirst(), LocalDate.parse(fact.getDate()));
    }

    private UUID uuid(String value) {
        return UUID.fromString(value);
    }

    private Timestamp instant(String value) {
        return value == null ? null : Timestamp.from(Instant.parse(value));
    }

    public enum Result { APPLIED, DUPLICATE, STALE }
    private record Envelope(UUID tenant, UUID aggregate, UUID event, long revision) {}
}
