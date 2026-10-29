package com.hrflow.leave.organization;

import com.hrflow.contracts.organization.HolidaySnapshot;
import com.hrflow.contracts.organization.OrganizationFact;
import com.hrflow.contracts.organization.ShiftSnapshot;
import com.hrflow.leave.service.TenantSqlScope;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveCalendarConsumer {
    private static final Map<String, Set<String>> TYPES = Map.of(
            "org.shift.v1", Set.of("org.shift.defined.v1", "org.shift.updated.v1", "org.shift.removed.v1"),
            "org.holiday.v1", Set.of("org.holiday.added.v1", "org.holiday.updated.v1", "org.holiday.removed.v1"));
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;

    @Transactional
    public void apply(String topic, String key, OrganizationFact event) {
        if (event == null || event.getSchemaVersion() != 1 || event.getAggregateVersion() < 1
                || !TYPES.getOrDefault(topic, Set.of()).contains(event.getType())) throw new IllegalArgumentException("Unsupported leave calendar fact");
        String tenant = UUID.fromString(event.getTenantId()).toString(), id = UUID.fromString(event.getAggregateId()).toString();
        UUID eventId = UUID.fromString(event.getEventId());
        UUID.fromString(event.getCorrelationId());
        Instant.parse(event.getOccurredAt());
        if (!(tenant + ":" + id).equals(key)) throw new IllegalArgumentException("Calendar key mismatch");
        scope.use(tenant);
        int receipt = jdbc.update("INSERT INTO leave.organization_inbox(tenant_id,event_id) VALUES (?,?) ON CONFLICT DO NOTHING", tenant, eventId);
        if (receipt == 0) return;
        switch (topic) {
            case "org.shift.v1" -> shift(tenant, id, event.getAggregateVersion(), event.getPayload());
            case "org.holiday.v1" -> holiday(tenant, id, event.getAggregateVersion(), event.getPayload());
            default -> throw new IllegalArgumentException("Unknown leave calendar topic");
        }
    }

    private void shift(String tenant, String id, long revision, Object payload) {
        if (!(payload instanceof ShiftSnapshot fact) || !id.equals(fact.getShiftId())) throw new IllegalArgumentException("Shift payload mismatch");
        LocalTime start = LocalTime.parse(fact.getStartTime()), end = LocalTime.parse(fact.getEndTime());
        ZoneId.of(fact.getTimezone());
        if (!end.isAfter(start) || fact.getWorkingDays() < 1 || fact.getWorkingDays() > 127) throw new IllegalArgumentException("Invalid shift window/calendar");
        jdbc.update("""
                INSERT INTO leave.shift_projection(tenant_id,shift_id,start_time,end_time,timezone,working_days,deleted,revision)
                VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(tenant_id,shift_id) DO UPDATE
                SET start_time=EXCLUDED.start_time,end_time=EXCLUDED.end_time,timezone=EXCLUDED.timezone,
                    working_days=EXCLUDED.working_days,deleted=EXCLUDED.deleted,revision=EXCLUDED.revision
                WHERE leave.shift_projection.revision<EXCLUDED.revision
                """, tenant, id, start, end, fact.getTimezone(), fact.getWorkingDays(), fact.getDeletedAt() != null, revision);
    }

    private void holiday(String tenant, String id, long revision, Object payload) {
        if (!(payload instanceof HolidaySnapshot fact) || !id.equals(fact.getHolidayId())
                || fact.getKind() == null || !Set.of("PUBLIC", "COMPANY").contains(fact.getKind())) throw new IllegalArgumentException("Holiday payload mismatch");
        jdbc.update("""
                INSERT INTO leave.holiday_projection(tenant_id,holiday_id,date,deleted,revision)
                VALUES (?,?,?,?,?) ON CONFLICT(tenant_id,holiday_id) DO UPDATE
                SET date=EXCLUDED.date,deleted=EXCLUDED.deleted,revision=EXCLUDED.revision
                WHERE leave.holiday_projection.revision<EXCLUDED.revision
                """, tenant, id, LocalDate.parse(fact.getDate()), fact.getDeletedAt() != null, revision);
    }
}
