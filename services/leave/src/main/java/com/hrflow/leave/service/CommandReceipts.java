package com.hrflow.leave.service;

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
    private final TenantSqlScope scope;

    /** Reserve and settle a command in the same transaction as its owner-row mutation. */
    @Transactional(propagation = Propagation.REQUIRED)
    public <T> T execute(String tenantId, String actorId, String path, String keyText,
            Object request, Class<T> responseType, Supplier<T> action) {
        UUID key = key(keyText);
        scope.use(tenantId);
        Number historical = (Number) entityManager.createNativeQuery(
                "SELECT count(*) FROM leave.command_receipt WHERE tenant_id=:tenant AND command_key=:key")
                .setParameter("tenant", tenantId).setParameter("key", key.toString()).getSingleResult();
        if (historical.longValue() != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Legacy command outcome needs reconciliation; its original actor/path is unknown");
        }
        String hash = hash(json.writeValueAsString(request));
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO leave.command_receipt_v2
                    (tenant_id, actor_id, method, path, command_key, request_hash)
                VALUES (:tenant, :actor, 'POST', :path, :key, :hash)
                ON CONFLICT DO NOTHING
                """)
                .setParameter("tenant", tenantId).setParameter("actor", actorId)
                .setParameter("path", path).setParameter("key", key)
                .setParameter("hash", hash).executeUpdate();
        if (inserted == 0) {
            Object[] stored = (Object[]) entityManager.createNativeQuery("""
                    SELECT request_hash, response_body FROM leave.command_receipt_v2
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
                UPDATE leave.command_receipt_v2 SET response_body = :body
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
            if (key.version() == 4 && key.variant() == 2) return key;
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
