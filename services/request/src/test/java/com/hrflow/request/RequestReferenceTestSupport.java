package com.hrflow.request;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Local owner-policy fixtures only. Real startup replay/listener behavior has its own broker suite. */
abstract class RequestReferenceTestSupport {
    @Autowired JdbcTemplate referenceJdbc;
    @MockitoBean com.hrflow.request.organization.RequestEmployeeBootstrap employeeReplay;

    @BeforeEach void referenceReplayReadyForPolicyFixtures() {
        org.mockito.Mockito.when(employeeReplay.isReady()).thenReturn(true);
    }

    void referenceTeam(String tenant, String employee, String manager) {
        for (String id : java.util.List.of(employee, manager)) {
            referenceJdbc.update("""
                    INSERT INTO request.employee_projection(tenant_id,employee_id,manager_id,active,revision)
                    VALUES (?,?,?,true,1) ON CONFLICT(tenant_id,employee_id) DO UPDATE SET manager_id=EXCLUDED.manager_id,active=true
                    """, tenant, id, id.equals(employee) ? manager : null);
        }
    }
}
