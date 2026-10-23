# Scoped code coverage

## Ten-minute concept

What: Coverage measures executed lines/branches, not correctness by itself.

Why HRFlow needs it: Critical auth/attendance/leave decision code needs visible untested paths.

## Tiny example

JaCoCo targets 80 percent lines and 70 percent branches only in recorded critical packages.

## Common mistake

> **Pitfall:** Excluding difficult logic or writing assert-true tests defeats the purpose.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
