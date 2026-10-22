# Stuck protocol

## Forty-five minute boundary

1. Stop editing. Write the current step ID and last observed result.
1. Re-read Why, Before you start, and Expect; confirm you are not running a future command.
1. Run the current week's diagnostic in TROUBLESHOOTING.md and capture safe output.
1. Build the smallest reproduction: one request, one employee, one transaction, one failing test.
1. Write the question below; ask the other contributor or mentor with the reproduction.

## Question template

- Did: exact step and command, with secrets removed.
- Expected: the guide's exact response/test/row count.
- Happened: actual code/message and reproducible conditions.
- Tried: each attempted change and its result, including what was reverted safely.
- Last green: tag and database migration state.
- Needed: one concrete answer, not a screenshot of an entire desktop.

## Ninety-minute boundary

Apply the stone's skip-or-simplify rule from the weekly README. Cut stretch only. A critical authentication, attendance, leave concurrency, event durability, deploy, or restore failure is **FAIL** and moves into next week's first stone. Record owner and next action; do not label it passed to maintain the schedule.

## Shut down safely

- [ ] Commit reviewed safe work or keep it on an explicit WIP branch.
- [ ] Put the next small diagnostic in the dashboard.
- [ ] Preserve test evidence and last green tag.
- [ ] Stop for the day if tired; buffer time is planned work, not failure.
