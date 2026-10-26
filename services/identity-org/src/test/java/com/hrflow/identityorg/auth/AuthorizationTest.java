package com.hrflow.identityorg.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hrflow.identityorg.auth.service.test.MethodSecurity;
import com.hrflow.identityorg.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(AuthorizationTest.AuthorizationProbeController.class)
@Import({SecurityConfiguration.class, MethodSecurity.class, AuthorizationTest.AuthorizationProbeController.class})
class AuthorizationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder decoder;

    @Test
    void unauthenticatedCallerIsRejectedWith401ProblemJson() throws Exception {
        mvc.perform(get("/auth/test/hr-action"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void visibleForbiddenActionReturns403ProblemJsonForEmployee() throws Exception {
        mvc.perform(get("/auth/test/hr-action"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void permittedHrPathSucceedsForHrAdmin() throws Exception {
        mvc.perform(get("/auth/test/hr-action"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("hr-admin-authorized"));
    }

    @Test
    void serviceSideRejectedTokenReturns401ProblemJson() throws Exception {
        org.mockito.Mockito.when(decoder.decode("rejected-by-service"))
                .thenThrow(new BadJwtException("Invalid token signature or audience"));

        mvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer rejected-by-service"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void forgedCallerHeadersDoNotElevatePrivileges() throws Exception {
        // Forged caller tries to claim HR_ADMIN via headers
        mvc.perform(get("/auth/test/hr-action")
                        .header("X-Roles", "ROLE_HR_ADMIN")
                        .header("X-Tenant-Id", "forged-tenant"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @RestController
    static class AuthorizationProbeController {
        private final MethodSecurity methodSecurity;

        AuthorizationProbeController(MethodSecurity methodSecurity) {
            this.methodSecurity = methodSecurity;
        }

        @GetMapping("/auth/test/hr-action")
        @PreAuthorize("hasRole('HR_ADMIN')")
        java.util.Map<String, String> hrAction() {
            return java.util.Map.of("result", methodSecurity.executeHrAdminOperation());
        }
    }
}
