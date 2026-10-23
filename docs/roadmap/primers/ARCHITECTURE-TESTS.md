# Architecture tests

## Ten-minute concept

What: Tests inspect compiled dependencies and reject forbidden package edges.

Why HRFlow needs it: One app still needs enforceable domain ownership rather than comments.

## Tiny example

A leave class importing attendance.internal repository makes ArchUnit fail.

## Common mistake

> **Pitfall:** Empty class imports can produce a meaningless green rule.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
