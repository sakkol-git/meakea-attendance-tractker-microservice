# Git workflow and checkpoint safety

> **Note:** A disciplined Git workflow ensures that every 30 to 90 minute engineering step produces an atomic commit, keeping the working tree clean, reversible, and bisectable.

## Purpose and core concept

Git is a distributed version control system that tracks changes to source code across time.

In the HRFlow engineering methodology, Git is not treated as an afterthought or an end-of-day dumping ground. Instead, Git commits act as atomic checkpoints that anchor verified progress, enabling immediate rollback to known-green states whenever a build or test fails.

## Problem being solved

Undisciplined version control habits lead to wasted hours and severe technical debt:

- Giant multi-day commits: Combining database migrations, backend controllers, frontend styling, and bug fixes into a single massive commit makes code review impossible and obscures bugs.
- Unreversible broken states: When a complex refactor breaks tests after three hours of uncommitted work, developers cannot cleanly revert to the working baseline without discarding valuable code.
- Accidental secret leaks: Blindly running `git add .` stages `.env` files, production database passwords, and private SSH keys into public Git repositories.

## Simple mental model

A Git checkpoint is like a quick-save feature in a difficult adventure video game.

Before venturing into a dark dungeon or facing a dangerous boss monster (a major database migration or tricky concurrency refactor), you hit quick-save. If your character falls into a pit or gets defeated, you instantly reload your saved game from thirty seconds earlier rather than restarting the entire game from the title screen.

## How it works

The HRFlow stepping stone Git protocol:

1. Pre-step verification: Before starting a step, run `git status` to ensure a clean working tree.
1. Work isolation: Execute a single, focused 30 to 90 minute task (e.g., add `leave_balances` repository query).
1. Verification execution: Run the step's specific test command to verify that tests pass with zero failures.
1. Selective staging: Run `git add <specific-file>` to stage only the intended code and test files. Never stage secrets or temporary logs.
1. Standardized commit: Commit with an informative conventional message (e.g., `feat(week-06): add pessimistic lock query to leave repository`).
1. Stone milestone tagging: When all steps in a stepping stone pass, tag the commit with the milestone tag (e.g., `git tag w06-s02-green`).

## Why HRFlow needs it

In HRFlow's twelve-week curriculum:

- Junior velocity protection: Contributors A and B have bounded weekly hours (16.8 hours planned each). If a step fails, the [Stuck protocol](../STUCK-PROTOCOL.md) allows reverting in seconds.
- Non-destructive migration: Legacy microservice and Nuxt files remain in repository history; alignment of existing services proceeds incrementally on branches without destructive deletes.
- Milestone evidence: Stone tags (`w01-s03-green`) provide auditable proof that intermediate gates were verified before advancing to subsequent phases.

## Minimal example

Standard conventional commit commands:

```bash
# Check current branch and staged status
git status --short

# Stage specific verified files
git add services/leave/src/main/java/com/hrflow/leave/application/LeaveService.java
git add services/leave/src/test/java/com/hrflow/leave/LeaveServiceTest.java

# Commit with standard message prefix
git commit -m "feat(week-06): add pessimistic leave reservation logic"

# Tag milestone upon completing all steps in Stone 2
git tag w06-s02-green
```

## Application-specific implementation

Automated Git pre-commit verification script in `.githooks/pre-commit`:

```bash
#!/usr/bin/env bash
set -euo pipefail

# Check for accidental secret file staging
if git diff --cached --name-only | grep -E '(\.env|\.pem|\.key|credentials\.json)'; then
    echo "ERROR: Attempting to commit sensitive secret file! Aborting."
    exit 1
fi

# Run documentation linter if markdown files are modified
if git diff --cached --name-only | grep -E '\.md$'; then
    python3 docs/roadmap/tools/docs-lint.py docs
fi
```

Configure local repository to use project hooks:

```bash
git config core.hooksPath .githooks
```

## Common mistakes and pitfalls

Avoid these dangerous Git habits:

> **Pitfall:** Running `git add .` or `git commit -a` blindly. This accidentally stages scratch test scripts, credentials, or unrelated WIP files. Always review your diff using `git status` and stage files explicitly.

- Hard resets discarding unrelated work: Running `git reset --hard HEAD` without checking `git status` permanently destroys unstaged code. Always stash (`git stash`) or create a temporary branch before resetting.
- Re-tagging broken commits: Moving a green tag (e.g., `git tag -f w01-s03-green`) to a commit with failing tests destroys milestone integrity. Tags must reflect verified passes only.
- Committing broken code to shared branches: Never commit half-finished code that fails compilation. If you must pause mid-step, commit on a personal spike branch or stash changes.

## Testing and debugging

Verification routines:

- Clean tree check: Run `git status --short`. A completely clean tree prints zero lines.
- History inspection: Run `git log --oneline -n 5` to confirm that recent commit messages follow conventional formats.
- Safe branch recovery: If a step becomes irreparably tangled, run `git stash -u` to save your WIP safely into the stash, then inspect `git status`.

## Trade-offs and alternatives

Branching strategies:

- Trunk-based with frequent commits (HRFlow choice): Short-lived branches, frequent atomic commits, instant integration; requires disciplined automated testing.
- GitFlow: Long-lived `feature`, `develop`, `release`, and `master` branches; complex merge conflicts and excessive ceremony for a two-person team.
- Monolithic commits: Committing once per week; impossible to bisect bugs or revert isolated features.

## When not to use it

Do not use Git commits for:

- Local scratch notes and personal scratchpads: Keep personal notes outside tracked repository folders or in `.git/info/exclude`.
- Binary database backups and raw container images: Heavy binary files belong on external backup disks or container registries, never in Git commit history.

## Related documentation

- [Git safety protocol](../GIT-SAFETY.md)
- [Stuck protocol](../STUCK-PROTOCOL.md)
- [Format guide](../FORMAT-GUIDE.md)
- [Week 01 foundation build guide](../01-FOUNDATION/BUILD-GUIDE.md)
