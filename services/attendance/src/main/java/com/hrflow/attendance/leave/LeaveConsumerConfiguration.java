package com.hrflow.attendance.leave;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "hrflow.events.recovery.enabled", havingValue = "false", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class LeaveConsumerConfiguration {
    @Bean
    DefaultErrorHandler attendanceFactErrorHandler() {
        // Until reviewed DLQ recovery exists, failed facts remain replayable and cannot be skipped.
        var handler = new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS));
        handler.setCommitRecovered(false);
        handler.setAckAfterHandle(false);
        return handler;
    }
}
