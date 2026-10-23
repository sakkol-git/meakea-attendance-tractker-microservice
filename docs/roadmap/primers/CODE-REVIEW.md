# Small self-review

## Ten-minute concept

What: Ten minutes before a stone tag inspect scope, tests, permissions and recovery.

Why HRFlow needs it: Review catches changes outside the intended feature before deployment.

## Tiny example

Read staged diff, run relevant tests, confirm no secret/applied migration change, then tag.

## Common mistake

> **Pitfall:** A commit is a checkpoint, not proof that a feature is safe.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
