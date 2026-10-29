package com.hrflow.attendance.daily;

import com.hrflow.attendance.jobs.AttendanceJobLeader;
import com.hrflow.attendance.service.TenantSqlScope;
import jakarta.annotation.PreDestroy;
import java.sql.SQLException;
import java.time.LocalDate;
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
@ConditionalOnProperty(name = "hrflow.attendance.daily-worker.enabled", havingValue = "true")
public class DailyRecalculationWorker {
    private final AttendanceJobLeader leader;
    private final TransactionTemplate transactions;
    private final TenantSqlScope scope;
    private final JdbcTemplate jdbc;
    private final DailyStatusWriter writer;

    public DailyRecalculationWorker(@Value("${spring.datasource.url}") String url,
            @Value("${hrflow.attendance.relay.job-user:}") String user,
            @Value("${hrflow.attendance.relay.job-password:}") String password,
            TransactionTemplate transactions, TenantSqlScope scope, JdbcTemplate jdbc, DailyStatusWriter writer) {
        this.leader = new AttendanceJobLeader(url, user, password, 73104221L);
        this.transactions = transactions;
        this.scope = scope;
        this.jdbc = jdbc;
        this.writer = writer;
    }

    @Scheduled(fixedDelayString = "${hrflow.attendance.daily-worker.poll-ms:250}",
            initialDelayString = "${hrflow.attendance.daily-worker.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            UUID tenant = leader.nextTenant();
            if (tenant != null) transactions.executeWithoutResult(tx -> process(tenant));
        } catch (Exception failure) {
            log.warn("Daily recalculation remains pending after {}", failure.getClass().getSimpleName());
            close();
        }
    }

    private void process(UUID tenant) {
        scope.use(tenant);
        var candidates = jdbc.query("""
                SELECT employee_id,work_date FROM attendance.daily_recalculation
                WHERE tenant_id=? AND available_at<=now() ORDER BY requested_at,employee_id,work_date LIMIT 1
                """, (row, index) -> new Day(row.getObject(1, UUID.class), row.getObject(2, LocalDate.class)), tenant);
        if (candidates.isEmpty()) return;
        Day day = candidates.getFirst();
        // Every producer takes the employee lock before touching the queue; preserve that lock order.
        scope.lockEmployee(tenant, day.employee());
        var claimed = jdbc.queryForList("""
                SELECT 1 FROM attendance.daily_recalculation WHERE tenant_id=? AND employee_id=? AND work_date=?
                  AND available_at<=now() FOR UPDATE SKIP LOCKED
                """, tenant, day.employee(), day.date());
        if (claimed.isEmpty()) return;
        requireLeader();
        if (!writer.refresh(tenant, day.employee(), day.date())) {
            jdbc.update("""
                    UPDATE attendance.daily_recalculation SET available_at=now()+interval '30 seconds',attempts=attempts+1
                    WHERE tenant_id=? AND employee_id=? AND work_date=?
                    """, tenant, day.employee(), day.date());
        }
        requireLeader();
    }

    private void requireLeader() {
        try { if (!leader.valid()) throw new IllegalStateException("Daily worker lost leadership"); }
        catch (SQLException failure) { throw new IllegalStateException("Daily worker leadership unavailable", failure); }
    }
    @PreDestroy public synchronized void close() {
        try { leader.close(); } catch (SQLException failure) { log.warn("Daily worker leadership close failed"); }
    }
    private record Day(UUID employee, LocalDate date) {}
}
