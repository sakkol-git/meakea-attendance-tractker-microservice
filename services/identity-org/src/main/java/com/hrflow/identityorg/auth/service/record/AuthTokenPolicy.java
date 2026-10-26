package com.hrflow.identityorg.auth.service.record;

import java.time.Duration;

public final class AuthTokenPolicy {
    public static final Duration ACCESS_LIFETIME = Duration.ofMinutes(15);
    public static final Duration REFRESH_LIFETIME = Duration.ofDays(14);
    public static final String AUDIENCE = "hrflow-clients";

    private AuthTokenPolicy() {}
}
