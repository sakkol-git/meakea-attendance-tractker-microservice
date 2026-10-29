package com.hrflow.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final AtomicLong consumerLag = new AtomicLong(-1);
    private final org.springframework.beans.factory.ObjectProvider<com.hrflow.events.BrokerLagMonitor> broker;
    private final Counter deliveredCounter;
    private final Counter readCounter;

    public NotificationMetrics(MeterRegistry registry, org.springframework.beans.factory.ObjectProvider<com.hrflow.events.BrokerLagMonitor> broker) {
        this.broker = broker;
        Gauge.builder("hrflow.notification.consumer.lag", this, NotificationMetrics::getConsumerLag)
                .description("Number of unprocessed events pending in notification inbox/stream")
                .register(registry);

        this.deliveredCounter = Counter.builder("hrflow.notification.delivered.total")
                .description("Total notifications delivered to inboxes")
                .register(registry);

        this.readCounter = Counter.builder("hrflow.notification.read.total")
                .description("Total notifications marked as read")
                .register(registry);
    }

    public void setConsumerLag(long lag) {
        this.consumerLag.set(lag);
    }

    public long getConsumerLag() {
        var monitor = broker.getIfAvailable();
        return monitor == null ? this.consumerLag.get() : monitor.lag();
    }

    public void incrementDelivered() {
        this.deliveredCounter.increment();
    }

    public double getDeliveredCount() {
        return this.deliveredCounter.count();
    }

    public void incrementRead() {
        this.readCounter.increment();
    }

    public double getReadCount() {
        return this.readCounter.count();
    }
}
