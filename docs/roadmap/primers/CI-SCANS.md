# Build and supply-chain scans

## Ten-minute concept

What: CI repeats build/test/format and inspects dependencies, images and secrets.

Why HRFlow needs it: A safe local feature can still ship a vulnerable image or exposed token.

## Tiny example

Trivy and Gitleaks fail known synthetic bad fixtures before scanning real artifacts.

## Common mistake

> **Pitfall:** Treating a failed network update as a clean vulnerability scan hides risk.

## Check understanding

- [ ] Explain what the check prevents in two sentences.
- [ ] Name one observable failing result before returning to the step.

[Standards](../ENGINEERING-STANDARDS.md) and [dashboard](../YOU-ARE-HERE.md).
