# ADR-004: Attendance and leave conflicts

- Status: Proposed
- Date: 2026-10-22
- Decision owners: contributor A and contributor B; product/HR, security, and operator review where applicable.

## Supersession

[Current ADR](006-FINISH-LINE-MODULAR-MONOLITH.md) supersedes any old seven-service/Kafka/cloud assumptions; other proposals require current review.

## Context

The plan says Holiday > Approved leave > Manual > Inferred > Absent and also manual records always win. The old API supports multiple sessions and append-only corrections. The leave saga moves approval conflicts to NEEDS_REVIEW but does not define balances/compensation.

## Problem

A single precedence enum can discard evidence or hide work on leave/holiday. Automatic compensation can refund leave incorrectly.

## Decision

Separate calendar day type, effective session evidence, attendance flags, and review state. Manual approved interpretation supersedes inferred interpretation without deleting evidence. Overlapping approved leave/manual work produces a review conflict. Proposed NEEDS_REVIEW retains debit/overlay until HR explicitly keeps or cancels leave; cancellation creates compensating ledger and newer snapshot. O02 approval is required before automatic resolution; overnight and inference windows remain O03.

## Alternatives Considered

Calendar-always-wins hides observed work; manual-always-wins silently invalidates approved leave; immediate automatic refund risks wrong balances; explicit conflict review costs human effort but preserves facts.

## Consequences

More expressive day response and visible exceptions are needed. Reports use current daily revision, not raw session count. Legacy enum alone is insufficient when late and early both apply.

## Security and Operational Implications

Only authorized independent approvers resolve conflicts/corrections. Record reasons, actor, request approval revision, session references, and decision history. Do not use evidence confidence as disciplinary proof.

## Implementation and Migration

Implement conflict-free same-day check-in first. Block silent overlap finalization until policy approval. Use request revision guards so delayed conflict cannot resurrect cancelled leave; test duplicate compensation and late evidence.

## References

- [Detailed specification](../Architecture.md).
- [Execution plan](../PLAN.md).
