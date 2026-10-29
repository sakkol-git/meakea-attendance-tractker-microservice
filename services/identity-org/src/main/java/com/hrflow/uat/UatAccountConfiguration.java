package com.hrflow.uat;

import com.hrflow.identityorg.auth.setup.UatAccountService;
import com.hrflow.identityorg.organization.service.OrganizationFacts;
import com.hrflow.identityorg.organization.service.OrganizationOutbox;
import com.hrflow.identityorg.organization.service.TenantSqlScope;
import java.time.Clock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Separate from the server component scan: no HTTP routes, bootstrap user, keys or scheduled jobs. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan("com.hrflow.identityorg")
@EnableJpaRepositories("com.hrflow.identityorg")
@Import({UatAccountService.class, TenantSqlScope.class, OrganizationFacts.class, OrganizationOutbox.class})
public class UatAccountConfiguration {
    @Bean Clock setupClock() { return Clock.systemUTC(); }
    @Bean PasswordEncoder setupPasswordEncoder() { return new BCryptPasswordEncoder(); }
}
