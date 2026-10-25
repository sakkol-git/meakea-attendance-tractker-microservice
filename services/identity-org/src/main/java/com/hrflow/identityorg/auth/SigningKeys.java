package com.hrflow.identityorg.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.JWSAlgorithm;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class SigningKeys {
    @Bean
    RSAKey rsaKey(@Value("${hrflow.auth.key-directory:/keys}") String directory,
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
                new PKCS8EncodedKeySpec(decode(Files.readString(privateFile))));
        RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                new X509EncodedKeySpec(decode(Files.readString(publicFile))));
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
        String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 16);
        return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(kid)
                .keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey key) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey key, @Value("${hrflow.auth.issuer:hrflow-local}") String issuer) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private static byte[] decode(String pem) {
        return Base64.getDecoder().decode(pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s", ""));
    }

    private static String pem(String type, byte[] bytes) {
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes);
        return "-----BEGIN " + type + "-----\n" + encoded + "\n-----END " + type + "-----\n";
    }
}
