# Project baseline and capacity

## Inputs and honesty

The user replied “24” to the combined hours question. This guide interprets that as A 24 and B 24 hours per week; correct the dashboard if the answer meant only one contributor or 24 combined. Start date remains unknown. Working rhythm is Monday through Friday; steps last 45 to 60 minutes, or 30 to 45 in light weeks. Deadline is start plus exactly 84 calendar days, with release completed by the preceding Friday if needed.

Only 70 percent is planned: 16.8 hours each in ordinary weeks. Weeks 4 and 8 are 15 percent lighter: 14.28 hours each. No weekend work is allocated. Production setup is not ready, according to the user; host/domain/backup gates remain blocked.

## Capacity math

Normal weeks: 10 times 33.6 equals 336 combined hours. Light weeks: 2 times 28.56 equals 57.12. Total planned capacity is 393.12 contributor-hours; the unplanned 30 percent is not allocated secretly.

Each week has 15 steps: ten A-owned-or-joint and seven B-owned-or-joint participations. Shared steps count for both. At maximum 60 minutes in normal weeks and 45 in light weeks, core upper estimate is 10 times 17 plus 2 times 12.75 equals 195.5 combined hours. Reading allowance is 2 hours each per week, 48 total. Planned buffer is 149.62 hours for diagnostics, review, recovery, pairing and slower learning.

A: 115 core plus 24 reading plus 57.56 buffer equals 196.56. B: 80.5 core plus 24 reading plus 92.06 buffer equals 196.56. B uses buffer for real-device/contract acceptance and pairing, not unreviewed backend architecture. These estimates are provisional; a junior step that cannot fit is split or carried forward, not called done.

## Architecture comparison

| Outcome | Core estimate basis | Combined estimate | Verdict |
| --- | --- | --- | --- |
| A seven deployables | C core plus seven-service wiring, broker, security and recovery work | 195.5 plus 180 overhead plus 48 learning plus 149.62 buffer | **FAIL** baseline mostly unbuilt |
| B two deployables | C core plus separate worker deployment, credentials and integration | 195.5 plus 70 overhead plus 48 learning plus 149.62 buffer | **FAIL** no demonstrated benefit |
| C modular monolith | Counted 180-step ledger, package boundaries, local outbox worker | 195.5 core plus 48 learning plus 84.1 buffer | **CONDITIONAL PASS** at interpreted 24 h each |

Overhead estimates are planning estimates, not measured velocity. C is selected because source is mostly unbuilt and it has the fewest runtime failure modes. If actual contributor hours are below 24 or week 1 velocity exceeds estimates, do not certify this full scope as feasible: replan explicit cuts while preserving security, correctness, and restore gates.

## Current safe checkpoint

There is no green application tag. Dashboard starts at W01-S01, status **Not run**. Documentation commits are not evidence that any weekly gate passed. User-staged source is preserved outside documentation commits.
