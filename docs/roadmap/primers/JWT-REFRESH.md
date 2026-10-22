# JWT and rotating refresh

## Read in ten to twenty minutes

What: A signed short-lived access token identifies a principal; a separate opaque refresh secret obtains replacements.

Why HRFlow needs this: Validate signature/issuer/audience/time; get tenant from verified token. Rotate refresh once under lock.

## Tiny example

Old refresh R1 becomes revoked when R2 is issued; replay R1 revokes its family.

## Common mistake

> **Pitfall:** Decoding base64 without signature checks is not authentication.

## Try and explain

- [ ] Explain why concurrent refresh requests must be serialized.
- [ ] Explain the concept in two sentences before returning to the build step.

## Return

[Dashboard](../YOU-ARE-HERE.md) and [stuck protocol](../STUCK-PROTOCOL.md) tell you the next action. Reading is not a substitute for the step test.
