package com.hrflow.events;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;

/** Private group-level measurements include retry backlogs. No payload, account or tenant dimensions. */
public final class BrokerLagMonitor implements AutoCloseable {
    private final Admin admin;
    private final KafkaListenerEndpointRegistry registry;
    private final ConsumerLagState state = new ConsumerLagState();
    public BrokerLagMonitor(Admin admin, KafkaListenerEndpointRegistry registry) { this.admin = admin; this.registry = registry; }
    public long lag() { return state.lag(Instant.now()); }

    @Scheduled(fixedDelayString = "${hrflow.events.recovery.lag-poll-ms:10000}", initialDelay = 1000)
    public synchronized void poll() {
        try {
            Map<String, Set<String>> groups = new LinkedHashMap<>();
            for (var container : registry.getListenerContainers()) {
                var properties = container.getContainerProperties();
                var topics = properties.getTopics(); String group = container.getGroupId();
                if (topics == null || group == null || Arrays.stream(topics).anyMatch(topic -> topic.endsWith(".dlq"))) continue;
                groups.computeIfAbsent(group, ignored -> new LinkedHashSet<>()).addAll(Arrays.asList(topics));
            }
            if (groups.isEmpty()) { state.unavailable(); return; }
            var topics = groups.values().stream().flatMap(Set::stream).distinct().toList();
            var descriptions = admin.describeTopics(topics).allTopicNames().get(3, TimeUnit.SECONDS);
            Map<TopicPartition, OffsetSpec> requested = new LinkedHashMap<>();
            descriptions.forEach((topic, metadata) -> metadata.partitions().forEach(partition -> requested.put(new TopicPartition(topic, partition.partition()), OffsetSpec.latest())));
            var ends = admin.listOffsets(requested).all();
            var commits = new LinkedHashMap<String, org.apache.kafka.common.KafkaFuture<Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata>>>();
            groups.keySet().forEach(group -> commits.put(group, admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata()));
            var end = ends.get(3, TimeUnit.SECONDS); long total = 0;
            for (var group : commits.entrySet()) {
                var offsets = group.getValue().get(3, TimeUnit.SECONDS);
                for (var partition : end.entrySet()) if (groups.get(group.getKey()).contains(partition.getKey().topic())) {
                    long committed = offsets.containsKey(partition.getKey()) ? offsets.get(partition.getKey()).offset() : 0;
                    total = Math.addExact(total, Math.max(0, partition.getValue().offset() - committed));
                }
            }
            var change = state.observe(total, Instant.now());
            if (change == ConsumerLagState.Change.ALERT) LoggerFactory.getLogger(getClass()).warn("Consumer backlog exceeds 100 records for five minutes; inspect private broker recovery topics");
            if (change == ConsumerLagState.Change.RECOVERED) LoggerFactory.getLogger(getClass()).info("Consumer backlog alert recovered");
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); state.unavailable(); }
        catch (Exception unavailable) { state.unavailable(); }
    }
    @Override public void close() { admin.close(java.time.Duration.ofSeconds(2)); }
}
