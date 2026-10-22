# Flyway migrations

## Read in ten to twenty minutes

What: Versioned SQL changes bring a database from one known shape to the next.

Why HRFlow needs this: Fresh installs and upgrades must use the same reviewed schema history.

## Tiny example

V1 creates a table; V2 adds a column; neither is edited after application.

## Common mistake

> **Pitfall:** Editing V1 after production applied it causes checksum mismatch or incompatible history.

## Try and explain

- [ ] Run clean and upgrade fixtures and compare schema versions.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
