# Problem details

## Ten-minute concept

What: A typed HTTP error body gives clients stable code and status without server internals.

Why HRFlow needs it: One shared mapper prevents every feature inventing a new error shape.

## Tiny example

401 uses application/problem+json and stable AUTH_REQUIRED code, never a Java stack trace.

## Common mistake

> **Pitfall:** Returning exception.message can expose SQL or personal data.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
