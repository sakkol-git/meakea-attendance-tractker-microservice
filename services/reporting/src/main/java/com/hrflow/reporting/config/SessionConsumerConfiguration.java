package com.hrflow.reporting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "hrflow.events.recovery.enabled", havingValue = "false", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class SessionConsumerConfiguration {
    @Bean
    DefaultErrorHandler sessionErrorHandler() {
        // No accepted DLQ recovery is installed yet. Keep failed records replayable instead of skipping them.
        var handler = new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS));
        handler.setCommitRecovered(false);
        handler.setAckAfterHandle(false);
        return handler;
    }
}
