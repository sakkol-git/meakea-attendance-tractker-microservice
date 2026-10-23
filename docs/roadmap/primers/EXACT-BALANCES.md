# Exact balance arithmetic

## Ten-minute concept

What: Decimal/numeric types represent defined leave units without binary rounding drift.

Why HRFlow needs it: Concurrent reservations must reconcile to an immutable component ledger.

## Tiny example

1.5 days minus 0.5 equals exactly 1.0 using BigDecimal/numeric.

## Common mistake

> **Pitfall:** Float arithmetic plus unlocked read-modify-write can misstate entitlement.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
