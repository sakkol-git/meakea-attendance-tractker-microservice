package com.hrflow.reporting.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(SecurityConfigurationTests.ProbeController.class)
@Import({AccessTokenDecoder.class, SecurityConfiguration.class, SecurityConfigurationTests.ProbeController.class})
class SecurityConfigurationTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder decoder;

    @Autowired
    JwtAuthenticationConverter converter;

    @Test
    void deniesBusinessAndSensitiveManagementPaths() throws Exception {
        mvc.perform(get("/__security_probe")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void deniesAuthenticatedBusinessRequestsUntilAuthorizationExists() throws Exception {
        mvc.perform(get("/__security_probe")).andExpect(status().isForbidden());
    }

    @Test
    void permitsMinimalHealthProbe() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test
    void forwardedBearerNeedsLocalValidationAndHeadersGrantNothing() throws Exception {
        mvc.perform(get("/__security_probe").header("X-Tenant-Id", "forged")
                .header("X-Roles", "HR_ADMIN")).andExpect(status().isUnauthorized());
        when(decoder.decode("invalid")).thenThrow(new BadJwtException("invalid signature"));
        mvc.perform(get("/__security_probe").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        verify(decoder).decode("invalid");

        Jwt validated = Jwt.withTokenValue("valid").header("alg", "RS256")
                .claim("sub", "user").claim("tenantId", "tenant")
                .claim("employeeId", "employee").claim("roles", java.util.List.of("HR_ADMIN"))
                .build();
        assertThat(converter.convert(validated).getAuthorities())
                .extracting("authority").contains("ROLE_HR_ADMIN");
        when(decoder.decode("valid")).thenReturn(validated);
        mvc.perform(get("/__security_probe").header("Authorization", "Bearer valid"))
                .andExpect(status().isForbidden());
        verify(decoder).decode("valid");
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/__security_probe", "/actuator/env", "/actuator/health/liveness"})
        String probe() {
            return "probe";
        }
    }
}
