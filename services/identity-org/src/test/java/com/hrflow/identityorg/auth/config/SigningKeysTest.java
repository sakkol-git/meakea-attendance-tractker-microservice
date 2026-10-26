package com.hrflow.identityorg.auth.config;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SigningKeysTest {
    @Test
    void bootstrapsKeyWhenMissingAndLocalBootstrapTrue(@TempDir Path temp) throws Exception {
        SigningKeys config = new SigningKeys();
        RSAKey key = config.rsaKey(temp.toString(), true);
        assertNotNull(key);
        assertTrue(Files.exists(temp.resolve("private.pem")));
        assertTrue(Files.exists(temp.resolve("public.pem")));
    }

    @Test
    void failsFastWhenMissingAndLocalBootstrapFalse(@TempDir Path temp) {
        SigningKeys config = new SigningKeys();
        assertThrows(IllegalStateException.class, () -> config.rsaKey(temp.toString(), false));
    }

    @Test
    void rejectsMismatchedPrivateAndPublicKeys(@TempDir Path temp) throws Exception {
        Path first = Files.createDirectory(temp.resolve("first"));
        Path second = Files.createDirectory(temp.resolve("second"));
        SigningKeys config = new SigningKeys();
        config.rsaKey(first.toString(), true);
        config.rsaKey(second.toString(), true);
        Files.copy(second.resolve("public.pem"), first.resolve("public.pem"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> config.rsaKey(first.toString(), false));
        assertTrue(failure.getMessage().contains("do not match"));
    }
}
