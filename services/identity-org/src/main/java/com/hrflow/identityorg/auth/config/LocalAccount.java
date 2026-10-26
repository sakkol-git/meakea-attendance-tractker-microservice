package com.hrflow.identityorg.auth.config;

import com.hrflow.identityorg.profile.entity.Tenant;
import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.profile.repository.TenantRepository;
import com.hrflow.identityorg.profile.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class LocalAccount {
    @Bean
    ApplicationRunner seedLocalAccount(
            UserRepository users,
            TenantRepository tenants,
            PasswordEncoder passwordEncoder,
            @Value("${hrflow.auth.local-bootstrap:false}") boolean local,
            @Value("${hrflow.auth.dev-password:local-test-password-123}") String password) {
        return args -> {
            if (!local || password.isBlank()) return;
            if (password.length() < 12) throw new IllegalArgumentException("Local password must be at least 12 characters");
            if (users.findByTenantSlugAndEmailAndEnabledTrue("demo", "owner@example.test").isEmpty()) {
                Tenant tenant = tenants.findBySlug("demo").orElseGet(() -> tenants.save(new Tenant("demo")));
                users.save(new User(tenant, "owner@example.test",
                        passwordEncoder.encode(password), "Local Owner"));
            }
        };
    }
}
