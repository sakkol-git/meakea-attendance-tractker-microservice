package com.hrflow.identityorg.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hrflow.identityorg.auth.service.record.SessionTokens;
import com.hrflow.identityorg.auth.service.record.AuthTokenPolicy;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {
    public static TokenResponse of(SessionTokens pair, boolean isBrowser) {
        return new TokenResponse(
                pair.accessToken(),
                "Bearer",
                AuthTokenPolicy.ACCESS_LIFETIME.toSeconds(),
                isBrowser ? null : pair.refreshToken()
        );
    }
}
