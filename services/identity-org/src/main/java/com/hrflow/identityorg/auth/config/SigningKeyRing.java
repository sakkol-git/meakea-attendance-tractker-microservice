package com.hrflow.identityorg.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Publishes the active verification key and one retiring key until its explicit deadline. */
public final class SigningKeyRing {
    private final RSAKey active;
    private final RSAKey retiring;
    private final Instant retiringValidUntil;
    private final Clock clock;

    public SigningKeyRing(RSAKey active, RSAKey retiring, Instant retiringValidUntil, Clock clock) {
        if ((retiring == null) != (retiringValidUntil == null)) {
            throw new IllegalArgumentException("Retiring key and deadline must be configured together");
        }
        if (retiring != null && active.getKeyID().equals(retiring.getKeyID())) {
            throw new IllegalArgumentException("Active and retiring key IDs must differ");
        }
        this.active = active.toPublicJWK();
        this.retiring = retiring == null ? null : retiring.toPublicJWK();
        this.retiringValidUntil = retiringValidUntil;
        this.clock = clock;
    }

    public JWKSet jwks() {
        if (retiring != null && clock.instant().isBefore(retiringValidUntil)) {
            return new JWKSet(List.of(active, retiring));
        }
        return new JWKSet(active);
    }
}
