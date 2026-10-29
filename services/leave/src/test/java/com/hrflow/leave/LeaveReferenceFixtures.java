package com.hrflow.leave;

import org.springframework.jdbc.core.JdbcTemplate;

/** Explicit relationship fixture only; live owner Avro is verified in its separate transport suite. */
final class LeaveReferenceFixtures {
    private LeaveReferenceFixtures() {}
    static String calendar(JdbcTemplate jdbc, String tenant, String employee, int workingDays) {
        String shift = java.util.UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO leave.shift_projection(tenant_id,shift_id,start_time,end_time,timezone,working_days,deleted,revision)
                VALUES (?,?,'08:00','17:00','Asia/Phnom_Penh',?,false,1)
                """, tenant, shift, workingDays);
        String assignments = "[{\"assignmentId\":\"%s\",\"shiftId\":\"%s\",\"validFrom\":\"2000-01-01\",\"validTo\":null}]"
                .formatted(java.util.UUID.randomUUID(), shift);
        jdbc.update("""
                INSERT INTO leave.employee_projection(tenant_id,employee_id,active,revision,assignments)
                VALUES (?,?,true,1,?::jsonb) ON CONFLICT(tenant_id,employee_id) DO UPDATE SET assignments=EXCLUDED.assignments
                """, tenant, employee, assignments);
        return shift;
    }
    static void employee(JdbcTemplate jdbc, String tenant, String employee, String manager) {
        jdbc.update("""
                INSERT INTO leave.employee_projection(tenant_id,employee_id,manager_id,active,revision)
                VALUES (?,?,?,true,1) ON CONFLICT(tenant_id,employee_id) DO UPDATE SET manager_id=EXCLUDED.manager_id,active=true
                """, tenant, employee, manager);
    }
    static com.hrflow.contracts.organization.OrganizationFact fact(java.util.UUID tenant, java.util.UUID employee, java.util.UUID manager, long revision) {
        var body = com.hrflow.contracts.organization.EmployeeSnapshot.newBuilder().setEmployeeId(employee.toString())
                .setEmployeeNo("E-" + employee).setFullName("Synthetic").setStatus("ACTIVE")
                .setDepartmentId(java.util.UUID.randomUUID().toString()).setPositionId(java.util.UUID.randomUUID().toString())
                .setManagerId(manager == null ? null : manager.toString()).setAssignments(java.util.List.of()).build();
        return com.hrflow.contracts.organization.OrganizationFact.newBuilder().setEventId(java.util.UUID.randomUUID().toString())
                .setType("org.employee.updated.v1").setSchemaVersion(1).setTenantId(tenant.toString()).setAggregateId(employee.toString())
                .setAggregateVersion(revision).setOccurredAt(java.time.Instant.now().toString())
                .setCorrelationId(java.util.UUID.randomUUID().toString()).setPayload(body).build();
    }

}
