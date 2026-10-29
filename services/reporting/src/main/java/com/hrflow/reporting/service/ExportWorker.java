package com.hrflow.reporting.service;

import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dedicated metadata-only leadership connection; tenant payload work always uses the scoped app role. */
@Component
@Slf4j
@ConditionalOnProperty(name = "hrflow.reporting.export.worker.enabled", havingValue = "true")
public class ExportWorker {
    private final String url;
    private final String user;
    private final String password;
    private final ExportQueue queue;
    private final CsvExportService exports;
    private final Set<UUID> reconciled = new HashSet<>();
    private Connection leader;
    private UUID lastTenant;

    public ExportWorker(@Value("${spring.datasource.url}") String url,
            @Value("${hrflow.reporting.export.worker.job-user:}") String user,
            @Value("${hrflow.reporting.export.worker.job-password:}") String password,
            ExportQueue queue, CsvExportService exports) {
        if (user.isBlank() || password.isBlank()) throw new IllegalStateException("Private reporting job role required");
        this.url = url;
        this.user = user;
        this.password = password;
        this.queue = queue;
        this.exports = exports;
    }

    @Scheduled(fixedDelayString = "${hrflow.reporting.export.worker.poll-ms:500}",
            initialDelayString = "${hrflow.reporting.export.worker.initial-delay-ms:1000}")
    public synchronized void poll() {
        try {
            if (!lead()) return;
            UUID tenant = next(lastTenant);
            if (tenant == null) tenant = next(null);
            lastTenant = tenant;
            if (tenant == null) return;
            if (!reconciled.contains(tenant)) {
                exports.reconcileInterruptedJobs(tenant);
                reconciled.add(tenant);
            }
            if (!leader.isValid(1)) throw new SQLException("Export leadership lost");
            queue.processNext(tenant);
        } catch (Exception failure) {
            log.warn("Export worker could not complete its bounded poll: {}", failure.getClass().getSimpleName());
            closeLeader();
        }
    }

    private boolean lead() throws SQLException {
        if (leader != null && leader.isValid(2)) return true;
        closeLeader();
        var properties = new java.util.Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "5");
        properties.setProperty("socketTimeout", "5");
        Connection candidate = DriverManager.getConnection(url, properties);
        try (var query = candidate.prepareStatement("SELECT pg_try_advisory_lock(73104219)")) {
            try (var rows = query.executeQuery()) {
                rows.next();
                if (!rows.getBoolean(1)) { candidate.close(); return false; }
            }
        } catch (SQLException failure) { candidate.close(); throw failure; }
        leader = candidate;
        return true;
    }

    private UUID next(UUID cursor) throws SQLException {
        try (var query = leader.prepareStatement("""
                SELECT tenant_id FROM reporting_jobs.tenant_registry
                WHERE (?::uuid IS NULL OR tenant_id>?::uuid) ORDER BY tenant_id LIMIT 1
                """)) {
            query.setObject(1, cursor);
            query.setObject(2, cursor);
            try (var rows = query.executeQuery()) { return rows.next() ? rows.getObject(1, UUID.class) : null; }
        }
    }

    @PreDestroy
    public synchronized void closeLeader() {
        if (leader != null) {
            try { leader.close(); }
            catch (SQLException ignored) { log.warn("Export leader connection failed to close"); }
        }
        leader = null;
        reconciled.clear();
    }
}
