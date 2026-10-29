package com.hrflow.attendance.daily;

import com.hrflow.attendance.jobs.AttendanceJobLeader;
import com.hrflow.attendance.service.TenantSqlScope;
import jakarta.annotation.PreDestroy;
import java.sql.SQLException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
@ConditionalOnProperty(name = "hrflow.attendance.cutoff.enabled", havingValue = "true")
public class ShiftCutoffWorker {
    private final AttendanceJobLeader leader;
    private final JdbcTemplate jdbc;
    private final TenantSqlScope scope;
    private final TransactionTemplate transactions;
    private final ShiftCutoff cutoff;
    private final java.util.Map<UUID, UUID> employeeCursors = new java.util.HashMap<>();

    public ShiftCutoffWorker(@Value("${spring.datasource.url}") String url,
            @Value("${hrflow.attendance.relay.job-user:}") String user,
            @Value("${hrflow.attendance.relay.job-password:}") String password,
            JdbcTemplate jdbc, TenantSqlScope scope, TransactionTemplate transactions, ShiftCutoff cutoff) {
        leader = new AttendanceJobLeader(url, user, password, 73104222L);
        this.jdbc = jdbc;
        this.scope = scope;
        this.transactions = transactions;
        this.cutoff = cutoff;
    }

    @Scheduled(fixedDelayString = "${hrflow.attendance.cutoff.poll-ms:250}",
            initialDelayString = "${hrflow.attendance.cutoff.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            UUID tenant = leader.nextTenant();
            if (tenant == null) return;
            UUID previousEmployee = employeeCursors.get(tenant);
            UUID selected = transactions.execute(tx -> {
                scope.use(tenant);
                var ids = jdbc.queryForList("""
                        SELECT employee_id FROM attendance.employee_projection WHERE tenant_id=? AND status='ACTIVE'
                          AND (?::uuid IS NULL OR employee_id>?::uuid) ORDER BY employee_id LIMIT 1
                        """, UUID.class, tenant, previousEmployee, previousEmployee);
                return ids.isEmpty() ? null : ids.getFirst();
            });
            if (selected == null) { employeeCursors.remove(tenant); return; }
            employeeCursors.put(tenant, selected); // Bad references for one employee cannot starve the next one.
            requireLeader();
            transactions.executeWithoutResult(tx -> { cutoff.inspect(tenant, selected); requireLeader(); });
        } catch (org.springframework.web.ErrorResponseException missingReference) {
            log.warn("Cutoff waits for a verified employee schedule");
        } catch (Exception failure) {
            log.warn("Cutoff remains retryable after {}", failure.getClass().getSimpleName());
            close();
        }
    }

    private void requireLeader() {
        try { if (!leader.valid()) throw new IllegalStateException("Cutoff worker lost leadership"); }
        catch (SQLException failure) { throw new IllegalStateException("Cutoff leadership unavailable", failure); }
    }
    @PreDestroy public synchronized void close() {
        try { leader.close(); } catch (SQLException failure) { log.warn("Cutoff leadership close failed"); }
    }
}
