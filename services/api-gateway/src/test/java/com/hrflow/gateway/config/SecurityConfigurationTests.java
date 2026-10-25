package com.hrflow.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebFluxTest(SecurityConfigurationTests.ProbeController.class)
@Import({SecurityConfiguration.class, SecurityConfigurationTests.ProbeController.class})
class SecurityConfigurationTests {

    @Autowired
    WebTestClient client;

    @MockitoBean
    ReactiveJwtDecoder decoder;

    @Test
    void deniesBusinessAndSensitiveManagementPaths() {
        client.get().uri("/__security_probe").exchange().expectStatus().isUnauthorized();
        client.get().uri("/actuator/env").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser
    void deniesAuthenticatedBusinessRequestsUntilAuthorizationExists() {
        client.get().uri("/__security_probe").exchange().expectStatus().isForbidden();
    }

    @Test
    void permitsMinimalHealthProbe() {
        client.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk();
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/__security_probe", "/actuator/env", "/actuator/health/liveness"})
        String probe() {
            return "probe";
        }
    }
}
