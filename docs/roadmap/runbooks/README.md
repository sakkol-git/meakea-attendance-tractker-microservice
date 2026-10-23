# Operational runbooks

## Numbered procedures

- [Deploy a checked release](DEPLOY.md).
- [Roll back a failed deployment](ROLLBACK.md).
- [Restore and prove usable data](RESTORE.md).
- [Reboot the single host safely](HOST-REBOOT.md).
- [Detect and recover low disk without destructive cleanup](DISK-FULL.md).
- [Operate during internet loss](INTERNET-DOWN.md).

## Use and evidence

These procedures are verified through actual synthetic drills in Weeks 10–12. Inspect the target before commands; **NOT RUN** is honest until evidence exists. [Original host details](../10-OPERATIONS-AND-RESTORE/HOST-RUNBOOK.md) remain useful and are not regenerated.

[Script interface and safe target configuration](SCRIPT-INTERFACE.md) defines exact creation order, refusal guards and invocations. Missing prerequisite or report is a failed gate, not permission to guess a target.
