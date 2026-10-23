# Architecture tests

## Ten-minute concept

What: Tests inspect compiled dependencies and reject forbidden package edges.

Why HRFlow needs it: Seven services still need enforceable layer and artifact ownership rather than comments.

## Tiny example

A leave domain class importing Spring JDBC infrastructure makes ArchUnit fail; a Maven dependency on attendance implementation is also forbidden.

## Common mistake

> **Pitfall:** Empty class imports can produce a meaningless green rule.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
