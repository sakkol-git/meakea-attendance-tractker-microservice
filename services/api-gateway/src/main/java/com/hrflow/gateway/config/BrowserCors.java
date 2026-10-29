package com.hrflow.gateway.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
public class BrowserCors {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CorsWebFilter corsWebFilter(@Value("${hrflow.auth.browser-origin:http://localhost:3000}") String origin) {
        CorsConfiguration rules = new CorsConfiguration();
        rules.setAllowedOrigins(List.of(origin.split(",")));
        rules.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        rules.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN",
                "Idempotency-Key", "If-Match"));
        rules.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", rules);
        source.registerCorsConfiguration("/api/v2/**", rules);
        return new CorsWebFilter(source);
    }
}
