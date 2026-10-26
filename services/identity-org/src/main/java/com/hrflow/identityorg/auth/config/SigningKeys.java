package com.hrflow.identityorg.auth.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import com.hrflow.identityorg.auth.service.record.AuthTokenPolicy;

@Configuration(proxyBeanMethods = false)
public class SigningKeys {
    @Bean
    public RSAKey rsaKey(@Value("${hrflow.auth.key-directory:/keys}") String directory,
                  @Value("${hrflow.auth.local-bootstrap:false}") boolean localBootstrap) throws Exception {
        Path folder = Path.of(directory);
        Path privateFile = folder.resolve("private.pem");
        Path publicFile = folder.resolve("public.pem");
        if (!Files.exists(privateFile) || !Files.exists(publicFile)) {
            if (!localBootstrap) throw new IllegalStateException("Mount signing keys before starting identity-org");
            Files.createDirectories(folder);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            Files.writeString(privateFile, pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
            Files.setPosixFilePermissions(privateFile, EnumSet.of(PosixFilePermission.OWNER_READ));
            Files.writeString(publicFile, pem("PUBLIC KEY", pair.getPublic().getEncoded()));
        }
        KeyFactory factory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                new PKCS8EncodedKeySpec(der("PRIVATE KEY", Files.readString(privateFile))));
        RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                new X509EncodedKeySpec(der("PUBLIC KEY", Files.readString(publicFile))));
        if (!privateKey.getModulus().equals(publicKey.getModulus())) {
            throw new IllegalStateException("Configured JWT private and public keys do not match");
        }
        String kid = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(publicKey.getEncoded())).substring(0, 16);
        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(kid)
                .algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey key) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(key)));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey key, @Value("${hrflow.auth.issuer:hrflow-local}") String issuer) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<>("aud",
                audience -> audience instanceof java.util.Collection<?> values
                        && values.contains(AuthTokenPolicy.AUDIENCE));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return decoder;
    }

    private static String pem(String type, byte[] payload) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(payload)
                + "\n-----END " + type + "-----\n";
    }

    private static byte[] der(String type, String pem) {
        String base64 = pem.replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
