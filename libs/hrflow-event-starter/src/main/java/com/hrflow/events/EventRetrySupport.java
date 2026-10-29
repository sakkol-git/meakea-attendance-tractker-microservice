package com.hrflow.events;

import java.util.List;
import java.util.function.Consumer;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.retrytopic.DeadLetterPublishingRecovererFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration(proxyBeanMethods = false)
@EnableKafka
@org.springframework.scheduling.annotation.EnableScheduling
public class EventRetrySupport extends RetryTopicConfigurationSupport {
    @org.springframework.context.annotation.Bean
    org.springframework.kafka.retrytopic.RetryTopicSchedulerWrapper recoveryScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1); scheduler.setThreadNamePrefix("event-retry-");
        return new org.springframework.kafka.retrytopic.RetryTopicSchedulerWrapper(scheduler);
    }
    @Override protected void configureBlockingRetries(BlockingRetriesConfigurer retries) {
        var backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(250); backoff.setMultiplier(2); backoff.setMaxInterval(1000);
        retries.retryOn(TransientDataAccessException.class).backOff(backoff);
    }
    @Override protected void manageNonBlockingFatalExceptions(List<Class<? extends Throwable>> exceptions) { exceptions.add(IllegalArgumentException.class); }
    @Override protected Consumer<DeadLetterPublishingRecovererFactory> configureDeadLetterPublishingContainerFactory() {
        return factory -> {
            factory.setDeadLetterPublisherCreator(WireDeadLetterRecoverer::new);
            factory.setPartitionResolver((record, next) -> next != null && next.endsWith(".dlq") ? null : record.partition());
            factory.neverLogListenerException();
        };
    }
    @Override protected void configureCustomizers(CustomizersConfigurer customizers) {
        customizers.customizeErrorHandler(handler -> handler.setLogLevel(KafkaException.Level.DEBUG));
    }
}
