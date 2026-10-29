package com.hrflow.reporting.service;

import com.hrflow.reporting.entity.ExportJob;
import com.hrflow.reporting.repository.ExportJobRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Bounded immutable input captured at request time; filesystem work occurs after the claim commits. */
@Service
public class ExportQueue {
    private final EntityManager entityManager;
    private final TenantSqlScope scope;
    private final CsvExportService exports;
    private final ExportJobRepository jobs;
    private final TransactionTemplate transactions;
    private final JsonMapper json;
    private final ExportRows data;
    private final int rowCap;

    public ExportQueue(EntityManager entityManager, TenantSqlScope scope, CsvExportService exports,
            ExportJobRepository jobs, TransactionTemplate transactions, JsonMapper json, ExportRows data,
            @Value("${hrflow.reporting.export.row-cap:10000}") int rowCap) {
        if (rowCap < 1 || rowCap > 100000) throw new IllegalArgumentException("Export row cap must be 1..100000");
        this.entityManager = entityManager;
        this.scope = scope;
        this.exports = exports;
        this.jobs = jobs;
        this.transactions = transactions;
        this.json = json;
        this.data = data;
        this.rowCap = rowCap;
    }

    @Transactional
    public ExportJob create(UUID tenant, UUID requester, String report, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }
        scope.use(tenant);
        List<List<String>> rows = data.capture(tenant, report, from, to, rowCap);
        ExportJob job = exports.createExportJob(tenant, requester, report, from, to);
        jobs.flush();
        entityManager.createNativeQuery("INSERT INTO reporting.export_snapshot(tenant_id,job_id,rows) VALUES (:tenant,:job,cast(:rows as jsonb))")
                .setParameter("tenant", tenant).setParameter("job", job.getId())
                .setParameter("rows", json.writeValueAsString(rows)).executeUpdate();
        return job;
    }

    public boolean processNext(UUID tenant) throws IOException {
        Work work = transactions.execute(tx -> claim(tenant));
        if (work == null) return false;
        exports.completeClaimedExport(tenant, work.job(), work.rows());
        return true;
    }

    private Work claim(UUID tenant) {
        scope.use(tenant);
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:tenant,0))")
                .setParameter("tenant", "reporting.export:" + tenant).getSingleResult();
        Number running = (Number) entityManager.createNativeQuery("""
                SELECT count(*) FROM reporting.export_job WHERE tenant_id=:tenant AND status IN ('RUNNING','PROCESSING')
                """).setParameter("tenant", tenant).getSingleResult();
        if (running.longValue() > 0) return null;
        @SuppressWarnings("unchecked")
        List<Object[]> pending = entityManager.createNativeQuery("""
                SELECT j.id,s.rows::text FROM reporting.export_job j
                JOIN reporting.export_snapshot s ON s.tenant_id=j.tenant_id AND s.job_id=j.id
                WHERE j.tenant_id=:tenant AND j.status='QUEUED'
                ORDER BY j.created_at,j.id LIMIT 1 FOR UPDATE OF j SKIP LOCKED
                """).setParameter("tenant", tenant).getResultList();
        if (pending.isEmpty()) return null;
        Object[] selected = pending.getFirst();
        var job = jobs.findByIdAndTenantId((UUID) selected[0], tenant).orElseThrow();
        job.markRunning();
        jobs.saveAndFlush(job);
        List<List<String>> rows = json.readValue((String) selected[1],
                new tools.jackson.core.type.TypeReference<List<List<String>>>() {});
        return new Work(job.getId(), rows);
    }

    private record Work(UUID job, List<List<String>> rows) {}
}
