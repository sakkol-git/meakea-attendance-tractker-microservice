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
import org.springframework.web.bind.annotation.PostMapping;
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

    @Test
    void requiresAuthenticationForProfileRoutes() {
        client.get().uri("/api/v1/auth/me").exchange().expectStatus().isUnauthorized();
        client.patch().uri("/api/v1/auth/profile").exchange().expectStatus().isUnauthorized();
        client.post().uri("/api/v1/auth/change-password").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser
    void permitsAuthenticatedProfileRoutes() {
        client.get().uri("/api/v1/auth/me").exchange().expectStatus().isOk();
        client.patch().uri("/api/v1/auth/profile").exchange().expectStatus().isOk();
        client.post().uri("/api/v1/auth/change-password").exchange().expectStatus().isOk();
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/__security_probe", "/actuator/env", "/actuator/health/liveness",
                "/api/v1/auth/me"})
        String probe() {
            return "probe";
        }

        @org.springframework.web.bind.annotation.PatchMapping("/api/v1/auth/profile")
        String patchProfile() {
            return "probe";
        }

        @PostMapping("/api/v1/auth/change-password")
        String changePassword() {
            return "probe";
        }
    }
}
