package com.hrflow.attendance.jobs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/** One private metadata connection per job; it never reads a business table or disables RLS. */
public final class AttendanceJobLeader implements AutoCloseable {
    private final String url;
    private final java.util.Properties credentials = new java.util.Properties();
    private final long lock;
    private Connection connection;
    private UUID cursor;

    public AttendanceJobLeader(String url, String user, String password, long lock) {
        if (user.isBlank() || password.isBlank()) throw new IllegalStateException("Private attendance job role required");
        this.url = url;
        this.lock = lock;
        credentials.setProperty("user", user);
        credentials.setProperty("password", password);
        credentials.setProperty("connectTimeout", "5");
        credentials.setProperty("socketTimeout", "5");
    }

    public UUID nextTenant() throws SQLException {
        if (!lead()) return null;
        UUID next = after(cursor);
        if (next == null) next = after(null);
        cursor = next;
        return next;
    }

    public boolean valid() throws SQLException { return connection != null && connection.isValid(1); }

    private boolean lead() throws SQLException {
        if (valid()) return true;
        close();
        Connection candidate = DriverManager.getConnection(url, credentials);
        try (var query = candidate.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            query.setLong(1, lock);
            try (var rows = query.executeQuery()) {
                rows.next();
                if (!rows.getBoolean(1)) { candidate.close(); return false; }
            }
        } catch (SQLException failure) { candidate.close(); throw failure; }
        connection = candidate;
        return true;
    }

    private UUID after(UUID previous) throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT tenant_id FROM attendance_jobs.tenant_registry
                WHERE (?::uuid IS NULL OR tenant_id>?::uuid) ORDER BY tenant_id LIMIT 1
                """)) {
            query.setObject(1, previous);
            query.setObject(2, previous);
            try (var rows = query.executeQuery()) { return rows.next() ? rows.getObject(1, UUID.class) : null; }
        }
    }

    @Override public void close() throws SQLException {
        try { if (connection != null) connection.close(); }
        finally { connection = null; }
    }
}
