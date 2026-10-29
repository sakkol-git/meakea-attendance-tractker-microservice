package com.hrflow.identityorg.auth.setup;

import com.hrflow.identityorg.audit.AuditRepository;
import com.hrflow.identityorg.audit.IdentityAudit;
import com.hrflow.identityorg.auth.service.record.AuthRoles;
import com.hrflow.identityorg.organization.repository.EmployeeRepository;
import com.hrflow.identityorg.organization.service.OrganizationFacts;
import com.hrflow.identityorg.organization.service.TenantSqlScope;
import com.hrflow.identityorg.profile.entity.User;
import com.hrflow.identityorg.profile.repository.TenantRepository;
import com.hrflow.identityorg.profile.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.nio.CharBuffer;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Private setup only: no HTTP controller, invitation, inferred link or existing-account update. */
@Service
@RequiredArgsConstructor
public class UatAccountService {
    private final TenantRepository tenants;
    private final UserRepository users;
    private final EmployeeRepository employees;
    private final TenantSqlScope scope;
    private final OrganizationFacts facts;
    private final AuditRepository audit;
    private final PasswordEncoder passwords;
    private final Validator validator;
    private final EntityManager entityManager;
    private final Clock clock;

    @Transactional
    public UUID provision(UatAccountRequest request, char[] password) {
        if (request == null || !validator.validate(request).isEmpty())
            throw new IllegalArgumentException("Explicit tenant, employee, valid login email, roles and time zone are required");
        java.util.List<String> roles;
        try { roles = AuthRoles.parse(String.join(",", request.roles())); }
        catch (IllegalStateException invalid) { throw new IllegalArgumentException("Use only EMPLOYEE, MANAGER, HR_ADMIN and OWNER"); }
        if (!roles.contains("EMPLOYEE")) throw new IllegalArgumentException("Include the employee's base EMPLOYEE role explicitly");
        ZoneId.of(request.timeZone());
        if (password == null || password.length < 12 || CharBuffer.wrap(password).chars().allMatch(Character::isWhitespace))
            throw new IllegalArgumentException("Use a nonblank password of at least 12 characters");
        var tenant = tenants.findBySlug(request.tenant()).orElseThrow(() -> new IllegalArgumentException("Tenant does not exist"));
        scope.use(tenant.getId());
        var employee = employees.lockByTenantIdAndId(tenant.getId(), request.employee())
                .orElseThrow(() -> new IllegalArgumentException("Employee does not exist in the selected tenant"));
        if (!"ACTIVE".equals(employee.getStatus())) throw new IllegalArgumentException("Employee must be active");
        if (users.existsByTenantIdAndEmail(tenant.getId(), request.email())
                || users.existsByTenantIdAndEmployeeIdAndEnabledTrue(tenant.getId(), employee.getId()))
            throw new IllegalArgumentException("Account or employee link already exists; inspect it before retrying. No account was changed");
        var account = User.linked(tenant, employee.getId(), request.email(), passwords.encode(CharBuffer.wrap(password)),
                employee.getName(), roles, request.timeZone());
        users.saveAndFlush(account);
        facts.employee(employee, "org.employee.updated.v1");
        String databasePrincipal = (String) entityManager.createNativeQuery("SELECT current_user").getSingleResult();
        audit.save(new IdentityAudit(tenant.getId(), null, "UAT_ACCOUNT_CREATED", "auth_user", account.getId(),
                Map.of("source", "PRIVATE_UAT_SETUP", "databasePrincipal", databasePrincipal,
                        "employeeId", employee.getId().toString(), "roles", roles), clock.instant()));
        return account.getId();
    }
}
