package com.hrflow.attendance;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Explicit owner snapshots for tests that exercise real attendance commands. */
final class AttendanceFixtures {
    private AttendanceFixtures() {}

    static void reportsTo(JdbcTemplate jdbc, UUID tenant, UUID employee, UUID manager) {
        jdbc.update("UPDATE attendance.employee_projection SET manager_id=?,manager_known=true WHERE tenant_id=? AND employee_id=?",
                manager, tenant, employee);
    }

    static UUID schedule(JdbcTemplate jdbc, UUID tenant, UUID employee) {
        UUID shift = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO attendance.shift_projection
                    (tenant_id,shift_id,name,start_time,end_time,timezone,grace_minutes,working_days,
                     site_id,site_name,latitude,longitude,radius_m,revision)
                VALUES (?,?,'Assigned','08:00','17:00','Asia/Phnom_Penh',10,127,?,'Office',11.56,104.92,100,1)
                """, tenant, shift, UUID.randomUUID());
        String assignments = """
                [{"assignmentId":"%s","shiftId":"%s","validFrom":"2000-01-01","validTo":null}]
                """.formatted(UUID.randomUUID(), shift);
        jdbc.update("""
                INSERT INTO attendance.employee_projection
                    (tenant_id,employee_id,employee_no,full_name,status,assignments,revision)
                VALUES (?,?,'E-1','Synthetic Employee','ACTIVE',?::jsonb,1)
                """, tenant, employee, assignments);
        return shift;
    }
}
