package com.hrflow.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigurationTests.ProbeController.class)
@Import({SecurityConfiguration.class, SecurityConfigurationTests.ProbeController.class})
class SecurityConfigurationTests {

    @Autowired
    MockMvc mvc;

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

    @RestController
    static class ProbeController {
        @GetMapping({"/__security_probe", "/actuator/env", "/actuator/health/liveness"})
        String probe() {
            return "probe";
        }
    }
}
