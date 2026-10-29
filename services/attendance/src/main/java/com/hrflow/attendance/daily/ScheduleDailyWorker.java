package com.hrflow.attendance.daily;

import com.hrflow.attendance.jobs.AttendanceJobLeader;
import jakarta.annotation.PreDestroy;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(name = "hrflow.attendance.schedule-fanout.enabled", havingValue = "true")
public class ScheduleDailyWorker {
    private final AttendanceJobLeader leader;
    private final ScheduleDailyChanges changes;

    public ScheduleDailyWorker(@Value("${spring.datasource.url}") String url,
            @Value("${hrflow.attendance.relay.job-user:}") String user,
            @Value("${hrflow.attendance.relay.job-password:}") String password, ScheduleDailyChanges changes) {
        leader = new AttendanceJobLeader(url, user, password, 73104225L);
        this.changes = changes;
    }

    @Scheduled(fixedDelayString = "${hrflow.attendance.schedule-fanout.poll-ms:250}", initialDelayString = "${hrflow.attendance.schedule-fanout.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            var tenant = leader.nextTenant();
            if (tenant != null) changes.expandNext(tenant);
        } catch (Exception failure) {
            log.warn("Schedule recalculation fanout remains pending after {}", failure.getClass().getSimpleName());
            close();
        }
    }

    @PreDestroy public synchronized void close() {
        try { leader.close(); } catch (SQLException failure) { log.warn("Schedule worker leadership close failed"); }
    }
}
