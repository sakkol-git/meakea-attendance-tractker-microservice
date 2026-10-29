package com.hrflow.leave.organization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "hrflow.events.recovery.enabled", havingValue = "false", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class LeaveEmployeeErrors {
    @Bean DefaultErrorHandler leaveEmployeeErrorHandler() {
        // No record may be skipped until the separately reviewed DLQ recovery path is installed.
        var handler = new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS));
        handler.setCommitRecovered(false);
        handler.setAckAfterHandle(false);
        return handler;
    }
}
