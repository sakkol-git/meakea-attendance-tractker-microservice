# State machines and lifecycle transitions

> **Note:** Finite State Machines (FSM) govern entity lifecycles by defining an explicit set of allowable states, legal transitions between states, and transition side effects.

## Purpose and core concept

A Finite State Machine models a business entity that can exist in exactly one of a finite number of named states at any given moment. Changes in state (transitions) occur strictly in response to explicit events or commands.

State machines prevent impossible lifecycle jumps (such as approving a leave request that was already cancelled or checking out of a shift that was never started).

## Problem being solved

Managing complex lifecycles using scattered boolean flags (`isApproved`, `isCancelled`, `isReviewed`) leads to chaotic bugs:

- Illegal state combinations: An entity has both `isApproved = true` and `isCancelled = true`, creating conflicting business logic in reporting queries.
- Bypassed approval checks: Direct SQL updates or careless controller methods jump an entity from `DRAFT` directly to `PAID`, bypassing mandatory managerial review.
- Missing transition side-effects: An entity state changes to `CANCELLED`, but the developer forgets to release reserved leave days because transition logic is scattered across multiple controllers.

## Simple mental model

A state machine is like a subway turnstile gate.

The turnstile has two states: `LOCKED` and `UNLOCKED`. Inserting a valid transit card triggers the transition from `LOCKED` to `UNLOCKED`. Pushing the mechanical bar allows one passenger through and transitions the turnstile back to `LOCKED`.

Inserting a second card while already `UNLOCKED` does not grant double entry; pushing the bar while `LOCKED` sounds an alarm and blocks entry.

## How it works

A formal state machine defines:

1. State enumeration: The discrete list of valid states (e.g., `SUBMITTED`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `CANCELLED`).
1. Trigger events: The actions that provoke transition attempts (e.g., `ApproveCommand`, `CancelCommand`).
1. Guard conditions: Pre-conditions that must evaluate to true before a transition is permitted (e.g., the current user must be the assigned manager).
1. State mutation: The atomic update of the entity's status column.
1. Transition side effects: Coordinated domain actions executed during the transition (e.g., decrementing pending days and incrementing used days).

## Why HRFlow needs it

In HRFlow, multi-step workflows require strict lifecycle governance:

- Leave request lifecycle: `SUBMITTED` -> `PENDING_APPROVAL` -> `APPROVED` (or `REJECTED`). Once `APPROVED`, the request can transition to `CANCELLED` only through an explicit compensation action that refunds the employee's balance.
- Attendance session lifecycle: `OPEN` -> `CLOSED` (or `AUTO_CLOSED`). Sessions with anomalies transition to `NEEDS_REVIEW`, requiring explicit HR correction before inclusion in payroll exports.
- Material request approvals: Multi-level requests progress through `MANAGER_REVIEW` -> `PROCUREMENT_REVIEW` -> `FULFILLED`.

## Minimal example

State machine transition logic in PostgreSQL:

```sql
-- Valid state check constraint
ALTER TABLE leave.leave_requests
ADD CONSTRAINT chk_valid_leave_status
CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'CANCELLED'));

-- Prevent illegal updates via trigger or transactional condition
UPDATE leave.leave_requests
SET status = 'APPROVED',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'a1b2c3d4-0000-0000-0000-000000000001'
  AND tenant_id = 'tenant-corp'
  AND status = 'SUBMITTED'; -- Enforce optimistic state guard
```

## Application-specific implementation

Rich domain model state machine in Java:

```java
public enum LeaveStatus {
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean canTransitionTo(LeaveStatus next) {
        return switch (this) {
            case SUBMITTED -> next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED -> next == CANCELLED;
            case REJECTED, CANCELLED -> false;
        };
    }
}
```

```java
@Entity
@Table(name = "leave_requests", schema = "leave")
public class LeaveRequest {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveStatus status = LeaveStatus.SUBMITTED;

    public void approve(UUID managerId) {
        if (!this.status.canTransitionTo(LeaveStatus.APPROVED)) {
            throw new IllegalStateTransitionException(
                "Cannot approve leave request currently in status: " + this.status);
        }
        this.status = LeaveStatus.APPROVED;
    }

    public void cancel() {
        if (!this.status.canTransitionTo(LeaveStatus.CANCELLED)) {
            throw new IllegalStateTransitionException(
                "Cannot cancel leave request currently in status: " + this.status);
        }
        this.status = LeaveStatus.CANCELLED;
    }
}
```

## Common mistakes and pitfalls

Avoid these state machine errors:

> **Pitfall:** Exposing public setter methods like `setStatus(LeaveStatus status)` on entity classes. Public setters allow any service or test to bypass transition guards and force illegal states. Always encapsulate transitions inside expressive business methods like `approve()` or `cancel()`.

- Missing terminal states: Failing to declare terminal states (`REJECTED`, `CANCELLED`) allows terminated workflows to be accidentally revived by asynchronous events.
- Neglecting compensation logic: Transitioning from `APPROVED` to `CANCELLED` without refunding the employee's leave balance creates accounting discrepancies. Always execute compensating balance refunds during cancellation.
- Re-inventing external workflow engines: Heavy external workflow BPMN engines (like Camunda) add unnecessary complexity for simple 4-state lifecycles. Standard Java enums and domain methods are sufficient for HRFlow's MVP.

## Testing and debugging

Validation test plan:

- Legal transition test: Test that `SUBMITTED` -> `APPROVED` succeeds and updates the entity status cleanly.
- Illegal transition test: Attempt to call `approve()` on an entity already in `REJECTED` status. Assert that the method throws `IllegalStateTransitionException`.
- Database guard test: Attempt direct SQL update of status to an unapproved value (e.g., `'PENDING'`) and verify that PostgreSQL check constraints reject the transaction.

## Trade-offs and alternatives

State modeling approaches:

- Rich domain enums (HRFlow choice): Simple, clean, fast execution, zero external dependencies; ideal for predictable sequential workflows.
- Spring State Machine: Full-featured event-driven FSM library; adds substantial configuration overhead and boilerplate for simple entity lifecycles.
- Distributed sagas: Coordinates distributed state transitions across multiple microservices; unnecessary in a modular monolith sharing a single relational database.

## When not to use it

Do not use a state machine for:

- Static lookup tables: Managing lists of countries, departments, or currency codes does not involve lifecycle state transitions.
- Free-form draft documents: Employee notes or rich-text job descriptions that can be edited continuously without discrete lifecycle stages.

## Related documentation

- [Database locks primer](DATABASE-LOCKS.md)
- [Transactional outbox primer](TRANSACTIONAL-OUTBOX.md)
- [Week 06 leave workflow build guide](../06-LEAVE-WORKFLOW/BUILD-GUIDE.md)
- [Architecture decision record 004](../../adr/004-ATTENDANCE-AND-LEAVE-CONFLICTS.md)
