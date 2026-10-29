package com.hrflow.attendance.config;

import java.util.Collection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
public class AccessTokenDecoder {
    @Bean
    JwtDecoder jwtDecoder(
            @Value("${hrflow.auth.jwks-uri:http://localhost:8101/.well-known/jwks.json}") String jwksUri,
            @Value("${hrflow.auth.issuer:hrflow-local}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<>("aud", value -> value instanceof Collection<?> audience
                        && audience.contains("hrflow-clients")),
                new JwtClaimValidator<>("tenantId", AccessTokenDecoder::isNonblank),
                new JwtClaimValidator<>("employeeId", AccessTokenDecoder::isNonblank),
                new JwtClaimValidator<>("roles", value -> value instanceof Collection<?> roles
                        && !roles.isEmpty())));
        return decoder;
    }

    private static boolean isNonblank(Object value) {
        return value instanceof String text && !text.isBlank();
    }
}
