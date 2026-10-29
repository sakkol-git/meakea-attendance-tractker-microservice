package com.hrflow.reporting.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

class KafkaConfigurationTest {

        @EnableKafka
        @TestConfiguration(proxyBeanMethods = false)
        @Import(KafkaAutoConfiguration.class)
        @EnableConfigurationProperties(KafkaProperties.class)
        static class ProbeConfig {
        }

        @Test
        @DisplayName("Disabled mode starts cleanly without Kafka connection or listeners")
        void disabledModeStartsWithoutKafka() {
                File prodConfig = new File("src/main/resources/application.yaml");
                if (!prodConfig.exists()) {
                        prodConfig = new File("services/reporting/src/main/resources/application.yaml");
                }

                SpringApplication app = new SpringApplication(ProbeConfig.class);
                app.setWebApplicationType(WebApplicationType.NONE);
                app.setLogStartupInfo(false);

                try (var context = app.run(
                                "--spring.main.banner-mode=off",
                                "--spring.config.location=" + prodConfig.getAbsolutePath(),
                                "--spring.flyway.enabled=false")) {
                        var env = context.getEnvironment();
                        assertThat(env.getProperty("hrflow.reporting.kafka-consumer.enabled"))
                                        .as("Reporting Kafka consumer must default to disabled in production config")
                                        .isEqualTo("false");

                        KafkaProperties kafkaProps = context.getBean(KafkaProperties.class);
                        assertThat(kafkaProps.getListener().isMissingTopicsFatal())
                                        .as("Effective bound KafkaProperties listener missing-topics-fatal must be true")
                                        .isTrue();

                        ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
                                        .getBean(ConcurrentKafkaListenerContainerFactory.class);
                        assertThat(factory.getContainerProperties().isMissingTopicsFatal())
                                        .as("Effective listener container factory must configure missingTopicsFatal to true")
                                        .isTrue();
                }
        }

        @Test
        @DisplayName("When consumer is explicitly enabled, effective container factory sets missingTopicsFatal=true")
        void whenConsumerExplicitlyEnabledEffectiveMissingTopicsFatalConfiguredOnFactory() {
                File prodConfig = new File("src/main/resources/application.yaml");
                if (!prodConfig.exists()) {
                        prodConfig = new File("services/reporting/src/main/resources/application.yaml");
                }

                SpringApplication app = new SpringApplication(ProbeConfig.class);
                app.setWebApplicationType(WebApplicationType.NONE);
                app.setLogStartupInfo(false);

                try (var context = app.run(
                                "--spring.main.banner-mode=off",
                                "--spring.config.location=" + prodConfig.getAbsolutePath(),
                                "--spring.flyway.enabled=false",
                                "--hrflow.reporting.kafka-consumer.enabled=true")) {
                        var env = context.getEnvironment();
                        assertThat(env.getProperty("hrflow.reporting.kafka-consumer.enabled")).isEqualTo("true");

                        KafkaProperties kafkaProps = context.getBean(KafkaProperties.class);
                        assertThat(kafkaProps.getListener().isMissingTopicsFatal()).isTrue();

                        ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
                                        .getBean(ConcurrentKafkaListenerContainerFactory.class);
                        assertThat(factory.getContainerProperties().isMissingTopicsFatal())
                                        .as("Effective listener factory must have missingTopicsFatal enabled")
                                        .isTrue();

                        ConcurrentMessageListenerContainer<?, ?> container = factory
                                        .createContainer("hrflow.attendance.facts");
                        assertThat(container.getContainerProperties().isMissingTopicsFatal())
                                        .as("Effective created container must inherit missingTopicsFatal=true")
                                        .isTrue();
                }
        }

        @Test
        @DisplayName("When missingTopicsFatal is enabled, container start fails closed on missing topics")
        void whenMissingTopicsFatalEnabledContainerStartFailsClosed() {
                File prodConfig = new File("src/main/resources/application.yaml");
                if (!prodConfig.exists()) {
                        prodConfig = new File("services/reporting/src/main/resources/application.yaml");
                }

                SpringApplication app = new SpringApplication(ProbeConfig.class);
                app.setWebApplicationType(WebApplicationType.NONE);
                app.setLogStartupInfo(false);

                try (var context = app.run(
                                "--spring.main.banner-mode=off",
                                "--spring.config.location=" + prodConfig.getAbsolutePath(),
                                "--spring.flyway.enabled=false",
                                "--hrflow.reporting.kafka-consumer.enabled=true",
                                "--spring.kafka.consumer.bootstrap-servers=127.0.0.1:19999",
                                "--spring.kafka.listener.auto-startup=false")) {
                        ConcurrentKafkaListenerContainerFactory<?, ?> factory = context
                                        .getBean(ConcurrentKafkaListenerContainerFactory.class);
                        ConcurrentMessageListenerContainer<?, ?> container = factory
                                        .createContainer("hrflow.attendance.facts");

                        assertThat(container.getContainerProperties().isMissingTopicsFatal()).isTrue();
                        assertThatThrownBy(container::start)
                                        .as("Starting container with missingTopicsFatal=true must fail closed")
                                        .isInstanceOf(Exception.class);
                }
        }
}
