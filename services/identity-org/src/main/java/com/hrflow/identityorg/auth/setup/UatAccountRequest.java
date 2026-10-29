package com.hrflow.identityorg.auth.setup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Explicit private operator inputs. Passwords never belong in arguments, DTOs or audit data. */
public record UatAccountRequest(
        @NotBlank @Size(max = 63) String tenant,
        @NotNull UUID employee,
        @NotBlank @Email @Size(max = 254) String email,
        @NotEmpty List<String> roles,
        @NotBlank @Size(max = 80) String timeZone) {
    public UatAccountRequest {
        tenant = tenant == null ? null : tenant.strip().toLowerCase(Locale.ROOT);
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
