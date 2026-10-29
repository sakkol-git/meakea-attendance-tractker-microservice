package com.hrflow.attendance.service;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class CommandReceipts {
    private final EntityManager entityManager;
    private final JsonMapper json;

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> T execute(UUID tenantId, UUID actorId, String path, String keyText,
            Object request, Class<T> responseType, Supplier<T> action) {
        UUID key = key(keyText);
        String hash = hash(json.writeValueAsString(request));
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO attendance.command_receipt
                    (tenant_id, actor_id, method, path, command_key, request_hash)
                VALUES (:tenant, :actor, 'POST', :path, :key, :hash)
                ON CONFLICT DO NOTHING
                """)
                .setParameter("tenant", tenantId).setParameter("actor", actorId)
                .setParameter("path", path).setParameter("key", key)
                .setParameter("hash", hash).executeUpdate();
        if (inserted == 0) {
            Object[] stored = (Object[]) entityManager.createNativeQuery("""
                    SELECT request_hash, response_body FROM attendance.command_receipt
                    WHERE tenant_id = :tenant AND actor_id = :actor AND method = 'POST'
                      AND path = :path AND command_key = :key FOR UPDATE
                    """)
                    .setParameter("tenant", tenantId).setParameter("actor", actorId)
                    .setParameter("path", path).setParameter("key", key).getSingleResult();
            if (!hash.equals(stored[0]) || stored[1] == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key has different content");
            }
            return json.readValue((String) stored[1], responseType);
        }
        T response = action.get();
        entityManager.createNativeQuery("""
                UPDATE attendance.command_receipt SET response_body = :body
                WHERE tenant_id = :tenant AND actor_id = :actor AND method = 'POST'
                  AND path = :path AND command_key = :key
                """)
                .setParameter("body", json.writeValueAsString(response))
                .setParameter("tenant", tenantId).setParameter("actor", actorId)
                .setParameter("path", path).setParameter("key", key).executeUpdate();
        return response;
    }

    private UUID key(String text) {
        try {
            UUID key = UUID.fromString(text);
            if (key.version() == 4) return key;
        } catch (RuntimeException ignored) {
            // Missing and invalid command keys share the same required-header response.
        }
        throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "UUID v4 Idempotency-Key required");
    }

    private String hash(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
