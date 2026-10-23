# A failing test first

## Ten-minute concept

What: An assertion states the missing behavior before the implementation changes.

Why HRFlow needs it: It proves the test would detect the defect you intend to fix.

## Tiny example

Run one method and see the expected status mismatch, then make that same assertion green.

## Common mistake

> **Pitfall:** Missing Docker/classpath is an environment failure, not a useful red behavior test.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
