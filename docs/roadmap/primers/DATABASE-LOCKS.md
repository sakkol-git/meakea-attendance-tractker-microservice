# Database locks and concurrency control

> **Note:** HRFlow ADR 008 chooses explicit JDBC. Any JPA example below is comparative teaching only, never the selected implementation. Use the Spring JDBC primer and owner-scoped SQL; no extra ORM dependency is admitted.

> **Note:** Database locks and concurrency controls protect shared business invariants from race conditions when multiple transactions access the same records concurrently.

## Purpose and core concept

Concurrency control ensures that simultaneous transactions modifying shared data preserve data consistency and business rules. Relational databases provide two primary concurrency strategies:

- Pessimistic locking: An exclusive row lock (`SELECT FOR UPDATE`) prevents concurrent transactions from reading or mutating the row until the locking transaction completes.
- Optimistic locking: Transactions read data without locking; during commit, an incrementing `version` column verifies that no other transaction modified the record in the interim.

## Problem being solved

Without concurrency control, simultaneous requests cause severe business race conditions:

- Lost updates: Two managers approve leave for the same employee simultaneously; the second update overwrites the first, losing track of approved days.
- Overspending balances: An employee with two remaining leave days submits two parallel requests for two days each; both requests read the balance of two, subtract two, and succeed, resulting in four days taken instead of two.
- Phantoms and dirty reads: A transaction calculates total departmental attendance while another transaction is inserting new check-in records, producing inconsistent summary totals.

## Simple mental model

Pessimistic locking is like locking a private meeting room door while you conduct an interview. No one else can enter until you finish, unlock the door, and leave.

Optimistic locking is like editing a shared collaborative document with version numbers. When you click save, the system checks whether the document is still on version 5. If someone else already saved version 6, your save is rejected with a conflict error, prompting you to review the latest changes.

## How it works

Pessimistic concurrency in PostgreSQL:

1. Begin transaction: The client initiates an explicit database transaction.
1. Acquire exclusive row lock: The application executes `SELECT balance, pending_days FROM leave.leave_balances WHERE tenant_id = :tenantId AND employee_id = :employeeId FOR UPDATE`.
1. Concurrent wait: Any second transaction attempting `SELECT ... FOR UPDATE` on that same row blocks and waits until the first transaction commits or rolls back.
1. Business validation: The first transaction checks if `balance - pending_days >= requestedDays`.
1. State mutation: The application updates the row and commits.
1. Lock release: The commit releases the row lock, allowing the waiting second transaction to proceed with fresh, accurate data.

## Why HRFlow needs it

In HRFlow, leave entitlement balances and shift session check-ins are critical shared resources:

- Leave balance reservation: When an employee submits a leave request, HRFlow acquires a pessimistic lock on their `leave_balances` row, reserves the requested days under `pending_days`, and commits.
- Concurrency protection: If the employee clicks the submit button twice rapidly or submits via web and mobile simultaneously, the second request waits for the lock and detects that remaining availability is insufficient.
- Double check-in prevention: When checking in, pessimistic locking or composite unique constraints prevent creating two open attendance sessions on the same work date.

## Minimal example

PostgreSQL row locking:

```sql
BEGIN;

-- Acquire exclusive write lock on employee balance
SELECT id, total_allocated, used_days, pending_days
FROM leave.leave_balances
WHERE tenant_id = 'tenant-corp'
  AND employee_id = 'emp-101'
  AND leave_type_id = 'annual-leave'
  AND year = 2026
FOR UPDATE;

-- Update balance atomically knowing no concurrent update can interfere
UPDATE leave.leave_balances
SET pending_days = pending_days + 2.0,
    updated_at = CURRENT_TIMESTAMP
WHERE tenant_id = 'tenant-corp'
  AND employee_id = 'emp-101'
  AND leave_type_id = 'annual-leave'
  AND year = 2026;

COMMIT;
```

## Application-specific implementation

Spring Data JPA pessimistic locking in HRFlow:

```java
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM LeaveBalance b WHERE b.tenantId = :tenantId " +
           "AND b.employeeId = :employeeId AND b.leaveTypeId = :leaveTypeId AND b.year = :year")
    Optional<LeaveBalance> findForUpdate(@Param("tenantId") String tenantId,
                                         @Param("employeeId") String employeeId,
                                         @Param("leaveTypeId") String leaveTypeId,
                                         @Param("year") int year);
}
```

```java
@Service
public class LeaveReservationService {

    private final LeaveBalanceRepository balanceRepo;

    public LeaveReservationService(LeaveBalanceRepository balanceRepo) {
        this.balanceRepo = balanceRepo;
    }

    @Transactional
    public void reserveLeave(String tenantId, String employeeId,
                             String leaveTypeId, int year, double days) {
        LeaveBalance balance = balanceRepo.findForUpdate(tenantId, employeeId, leaveTypeId, year)
                .orElseThrow(() -> new EntityNotFoundException("Leave balance record not found"));

        if (!balance.hasAvailableDays(days)) {
            throw new InsufficientLeaveBalanceException("Insufficient remaining leave days");
        }

        balance.reserveDays(days);
        balanceRepo.save(balance);
    }
}
```

## Common mistakes and pitfalls

Avoid these dangerous concurrency mistakes:

> **Pitfall:** Locking records in inconsistent order across different transactions. If Transaction A locks Employee 1 then Employee 2, while Transaction B locks Employee 2 then Employee 1, a deadlock occurs. Always sort entity identifiers deterministically before acquiring locks.

- Long-lived transactions holding locks: Never call external HTTP APIs, send emails, or generate large PDF reports while holding an open database lock. Minimize lock duration.
- Relying on application-level state: Checking balance in memory without a database lock (`find` without lock followed by `save`) is vulnerable to race conditions under concurrent requests.
- Forgetting database constraints: Application locks should always be backed by database `CHECK` constraints (e.g., `CHECK (used_days + pending_days <= total_allocated)`).

## Testing and debugging

Validation techniques:

- Parallel execution test: Use an `ExecutorService` with 10 concurrent threads attempting to reserve 1 day each against a balance of 5 days. Assert that exactly 5 succeed and 5 fail with `InsufficientLeaveBalanceException`.
- Database constraint verification: Attempt direct SQL insertion of negative availability and verify that PostgreSQL raises a check constraint violation.
- Deadlock detection: Run test suites with multi-record mutations in random order to confirm that deadlock exceptions are surfaced and handled.

## Trade-offs and alternatives

Comparing concurrency models:

- Pessimistic locking: Guarantees consistency and avoids retry storms; however, holding locks reduces throughput under high contention.
- Optimistic locking (`@Version`): High read throughput and zero locking overhead; however, high write contention triggers frequent `OptimisticLockException` errors requiring complex retry logic.
- Atomic SQL updates (`UPDATE ... SET balance = balance - 1 WHERE balance >= 1`): Extremely fast for single counters; unsuitable when multiple complex business entities must be evaluated together.

## When not to use it

Do not use pessimistic locks for:

- High-throughput read-only queries: Use standard non-locking `SELECT` queries with appropriate indexing.
- Long user think times: Never keep a database row locked while waiting for a human user to complete a multi-step browser form. Use optimistic version checks on form submission instead.

## Related documentation

- [Transactional outbox primer](TRANSACTIONAL-OUTBOX.md)
- [Idempotency primer](IDEMPOTENCY.md)
- [Week 06 leave workflow build guide](../06-LEAVE-WORKFLOW/BUILD-GUIDE.md)
- [API and database design](../../REST-API-DATABASE-DESIGN.md)
