# Deploy a checked release

## Status and prerequisites

First draft for implementation; **NOT RUN**. Referenced ops scripts are proposed, not installed today. Inspect/create them in named weekly steps first. Complete operational drills in Weeks 10–12; preserve the original detailed host runbook.

## Numbered procedure

1. Inspect confirmed host, private manifest, current image/config/migration IDs and newest verified backup. Refuse public enablement if TLS/auth/firewall/secrets/restore gate is incomplete.
1. Run Compose config validation and clean/upgrade migration tests locally. Record candidate immutable digest and previous compatible digest.
1. Run reviewed ops/deploy.sh: apply additive migration once, update exact image/config, wait readiness. Never edit applied SQL or clear volume.
1. Run authenticated business smoke from Nuxt/Flutter path and inspect safe correlation log, outbox state and version.
1. On failed smoke stop rollout; follow rollback runbook. Record release evidence/owner and only then enable approved ingress.

## Verification and recovery

- [ ] Actual target, command exit, safe output, operator and previous green release are recorded.
- [ ] No database/backup/private file was destroyed to obtain a green health check.
- [ ] Failure remains visible and the next action names a safe reproduction or recovery checkpoint.

[Host detail](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md), [daily commands](../DAILY-COMMANDS.md), and [stuck protocol](../STUCK-PROTOCOL.md).
