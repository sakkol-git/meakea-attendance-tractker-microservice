package com.hrflow.reporting.repository;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Counts people/days, never sessions; today's date is resolved in each owner's actual timezone. */
@Repository
@RequiredArgsConstructor
public class DailyReportRepository {
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public Counts today(UUID tenant, UUID manager, Instant asOf) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                SELECT count(*) FILTER(WHERE payload->>'dayType' IN ('WORKING_DAY','PARTIAL_LEAVE')
                         AND payload->>'clockIn' IS NOT NULL AND NOT (payload->>'late')::boolean),
                       count(*) FILTER(WHERE (payload->>'late')::boolean),
                       count(*) FILTER(WHERE payload->>'status'='ABSENT' AND (payload->>'finalized')::boolean),
                       count(*) FILTER(WHERE (payload->>'reviewRequired')::boolean),
                       count(*) FILTER(WHERE NOT (payload->>'finalized')::boolean)
                FROM reporting.daily_fact_projection d WHERE tenant_id=:tenant
                  AND work_date=(cast(:asOf as timestamptz) AT TIME ZONE (payload->>'timezone'))::date
                  AND (cast(:manager as uuid) IS NULL OR employee_id IN
                    (SELECT employee_id FROM reporting.employee_projection
                     WHERE tenant_id=:tenant AND manager_id=cast(:manager as uuid) AND active=true))
                """).setParameter("tenant", tenant).setParameter("manager", manager).setParameter("asOf", asOf).getSingleResult();
        return new Counts(((Number) row[0]).longValue(), ((Number) row[1]).longValue(), ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(), ((Number) row[4]).longValue());
    }
    public record Counts(long present, long late, long absent, long review, long provisional) {}
}
