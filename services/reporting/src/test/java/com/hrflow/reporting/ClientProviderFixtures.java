package com.hrflow.reporting;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Explicit disposable attendance reference inputs; organization-to-attendance replay has separate transport tests. */
final class ClientProviderFixtures {
    private ClientProviderFixtures() {}

    static void attendance(JdbcTemplate admin, UUID tenant, UUID employee, UUID manager) {
        UUID shift = UUID.randomUUID();
        admin.update("""
                INSERT INTO attendance.shift_projection(tenant_id,shift_id,name,start_time,end_time,timezone,grace_minutes,
                  working_days,site_id,site_name,latitude,longitude,radius_m,revision)
                VALUES (?,?,'Synthetic assigned shift','08:00','17:00','Asia/Phnom_Penh',10,127,?,'Synthetic office',11.56,104.92,100,1)
                """, tenant, shift, UUID.randomUUID());
        for (UUID selected : List.of(employee, manager)) {
            String assignments = "[{\"assignmentId\":\"" + UUID.randomUUID() + "\",\"shiftId\":\"" + shift + "\",\"validFrom\":\"2000-01-01\",\"validTo\":null}]";
            admin.update("""
                    INSERT INTO attendance.employee_projection(tenant_id,employee_id,employee_no,full_name,status,
                      assignments,revision,manager_id,manager_known) VALUES (?,?,'FIXTURE','Synthetic','ACTIVE',?::jsonb,1,?,true)
                    """, tenant, selected, assignments, selected.equals(employee) ? manager : null);
        }
    }
}
