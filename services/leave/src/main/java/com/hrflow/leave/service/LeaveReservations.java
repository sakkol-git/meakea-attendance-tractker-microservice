package com.hrflow.leave.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Named halves are exclusive slots; FULL_DAY reserves both within the actual owner date set. */
@Component
@RequiredArgsConstructor
public class LeaveReservations {
    private final JdbcTemplate jdbc;

    public void requireFree(String tenant, String employee, List<LocalDate> dates, String portion) {
        if (dates.isEmpty()) return;
        Set<LocalDate> selected = Set.copyOf(dates);
        var rows = jdbc.query("""
                SELECT date,portion FROM leave.day_reservation
                WHERE tenant_id=? AND employee_id=? AND date BETWEEN ? AND ?
                """, (row, index) -> new Reserved(row.getObject(1, LocalDate.class), row.getString(2)),
                tenant, employee, dates.getFirst(), dates.getLast());
        if (rows.stream().anyMatch(row -> selected.contains(row.date()) && ("FULL_DAY".equals(portion) || portion.equals(row.portion())))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The requested leave portion overlaps an existing reservation");
        }
    }
    private record Reserved(LocalDate date, String portion) {}
}
