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
@ConditionalOnProperty(name = "hrflow.attendance.holiday-fanout.enabled", havingValue = "true")
public class HolidayDailyWorker {
    private final AttendanceJobLeader leader;
    private final HolidayDailyChanges changes;

    public HolidayDailyWorker(@Value("${spring.datasource.url}") String url,
            @Value("${hrflow.attendance.relay.job-user:}") String user,
            @Value("${hrflow.attendance.relay.job-password:}") String password, HolidayDailyChanges changes) {
        leader = new AttendanceJobLeader(url, user, password, 73104224L);
        this.changes = changes;
    }

    @Scheduled(fixedDelayString = "${hrflow.attendance.holiday-fanout.poll-ms:250}", initialDelayString = "${hrflow.attendance.holiday-fanout.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            var tenant = leader.nextTenant();
            if (tenant != null) changes.expandNext(tenant);
        } catch (Exception failure) {
            log.warn("Holiday recalculation fanout remains pending after {}", failure.getClass().getSimpleName());
            close();
        }
    }

    @PreDestroy public synchronized void close() {
        try { leader.close(); } catch (SQLException failure) { log.warn("Holiday worker leadership close failed"); }
    }
}
