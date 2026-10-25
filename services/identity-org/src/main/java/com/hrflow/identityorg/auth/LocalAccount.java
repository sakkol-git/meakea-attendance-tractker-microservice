package com.hrflow.identityorg.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class LocalAccount {
    @Bean
    ApplicationRunner seedLocalAccount(
            AuthUserRepository users,
            AuthTenantRepository tenants,
            @Value("${hrflow.auth.local-bootstrap:false}") boolean local,
            @Value("${hrflow.auth.dev-password:}") String password) {
        return args -> {
            if (!local || password.isBlank()) return;
            if (password.length() < 12) throw new IllegalArgumentException("Local password must be at least 12 characters");
            if (users.findByTenantSlugAndEmailAndEnabledTrue("demo", "owner@example.test").isEmpty()) {
                AuthTenant tenant = tenants.findBySlug("demo").orElseGet(() -> tenants.save(new AuthTenant("demo")));
                users.save(new AuthUser(tenant, "owner@example.test",
                        new BCryptPasswordEncoder().encode(password), "Local Owner"));
            }
        };
    }
}
