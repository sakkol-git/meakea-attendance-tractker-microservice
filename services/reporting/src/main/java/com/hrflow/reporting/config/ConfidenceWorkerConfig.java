package com.hrflow.reporting.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ConfidenceWorkerConfig {

    @Bean
    public RestClient confidenceWorkerRestClient(
            @Value("${hrflow.confidence-service.url:http://localhost:8000}") String workerUrl,
            @Value("${hrflow.confidence-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${hrflow.confidence-service.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(workerUrl)
                .build();
    }
}
