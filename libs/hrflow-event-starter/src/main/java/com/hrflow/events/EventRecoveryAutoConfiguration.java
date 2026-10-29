package com.hrflow.events;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

@AutoConfiguration(before = KafkaAutoConfiguration.class)
@ConditionalOnProperty(name = "hrflow.events.recovery.enabled", havingValue = "true")
@EnableConfigurationProperties(KafkaProperties.class)
@Import(EventRetrySupport.class)
public class EventRecoveryAutoConfiguration {
    @Bean DefaultKafkaConsumerFactoryCustomizer originalWireConsumer() {
        return factory -> factory.updateConfigs(Map.of(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, WireAvroDeserializer.class));
    }
    @Bean DefaultKafkaProducerFactory<Object, Object> recoveryProducerFactory(KafkaProperties properties) {
        var config = properties.buildProducerProperties();
        var key = new DelegatingByTypeSerializer(Map.of(String.class, new StringSerializer(), byte[].class, new ByteArraySerializer()));
        var value = new DelegatingByTypeSerializer(Map.of(byte[].class, new ByteArraySerializer(), org.apache.avro.specific.SpecificRecord.class,
                new KafkaAvroSerializer()), true);
        return new DefaultKafkaProducerFactory<>(config, key, value);
    }
    @Bean(name = {"kafkaTemplate", "hrflowRecoveryKafkaTemplate"})
    KafkaTemplate<?, ?> recoveryKafkaTemplate(DefaultKafkaProducerFactory<Object, Object> recoveryProducerFactory) {
        return new KafkaTemplate<>(recoveryProducerFactory);
    }
    @Bean RetryTopicConfiguration ownerRetryTopics(@Qualifier("hrflowRecoveryKafkaTemplate") KafkaTemplate<?, ?> kafka) {
        return RetryTopicConfigurationBuilder.newInstance().includeTopics(SourceRecords.TOPICS)
                .exponentialBackoff(1000, 2, 4000).maxAttempts(4).retryTopicSuffix(".retry").suffixTopicsWithIndexValues()
                .dltSuffix(".dlq").autoStartDltHandler(false).doNotAutoCreateRetryTopics().traversingCauses()
                .notRetryOn(IllegalArgumentException.class).create(kafka);
    }
    @Bean BrokerLagMonitor brokerLagMonitor(KafkaProperties properties, org.springframework.kafka.config.KafkaListenerEndpointRegistry registry,
            io.micrometer.core.instrument.MeterRegistry meters) {
        var admin = properties.buildAdminProperties();
        admin.put("request.timeout.ms", 3000); admin.put("default.api.timeout.ms", 5000);
        admin.put("metadata.recovery.strategy", "none");
        var monitor = new BrokerLagMonitor(org.apache.kafka.clients.admin.Admin.create(admin), registry);
        io.micrometer.core.instrument.Gauge.builder("hrflow.events.consumer.lag", monitor, BrokerLagMonitor::lag)
                .description("Broker source and retry backlog; -1 means unknown or stale").register(meters);
        return monitor;
    }
    @Bean org.springframework.boot.health.contributor.HealthIndicator eventRecoveryHealthIndicator(BrokerLagMonitor monitor) {
        return () -> monitor.lag() < 0 ? org.springframework.boot.health.contributor.Health.unknown().build()
                : monitor.lag() > 1000 ? org.springframework.boot.health.contributor.Health.down().build()
                : org.springframework.boot.health.contributor.Health.up().build();
    }
}
