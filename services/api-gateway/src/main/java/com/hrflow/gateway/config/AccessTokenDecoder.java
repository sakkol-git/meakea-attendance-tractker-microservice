package com.hrflow.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration(proxyBeanMethods = false)
public class AccessTokenDecoder {
    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${hrflow.auth.jwks-uri:http://localhost:8101/.well-known/jwks.json}") String jwksUri,
            @Value("${hrflow.auth.issuer:hrflow-local}") String issuer) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<java.util.List<String>>("aud",
                        audience -> audience != null && audience.contains("hrflow-clients"))));
        return decoder;
    }
}
