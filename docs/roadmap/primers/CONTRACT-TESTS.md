# Contract tests

## Ten-minute concept

What: The same API specification validates examples, mocks and live responses.

Why HRFlow needs it: Clients must not discover schema drift at final integration.

## Tiny example

A response missing required id fails validation against the single OpenAPI file.

## Common mistake

> **Pitfall:** Maintaining a second client-only schema creates competing authority.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
