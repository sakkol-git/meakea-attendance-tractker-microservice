# Git safety

## Before changes

This repo began without commits and with staged source plus untracked work. Never run a blanket add/commit before reviewing secrets and generated folders. During the guide create a private safe initial code commit, then weekly branches such as `build/week-01`. Documentation is on `docs/roadmap-playbook`.

```bash
git status --short
git diff
git diff --cached
```

Inspect intended paths, secrets, and test output before staging explicitly. A commit is a checkpoint, not proof that a feature works.

## Step and stone checkpoints

Every step ends with a focused commit such as `feat(attendance): validate checkout interval`. Every stone ends with a lightweight tag matching its final step, for example `w04-s03-green`. Tests must pass and app startup must be demonstrated before tagging. Never create these tags while merely writing the guide.

```bash
git add PATH_REVIEWED_BY_YOU
git commit -m 'feat(attendance): validate checkout interval'
git tag w04-s03-green
git show --stat w04-s03-green
```

`PATH_REVIEWED_BY_YOU` is a token to replace with a real reviewed path, not a literal command argument to copy blindly.

## Return to green without deleting work

First commit a safe work-in-progress on the current branch, or stash only reviewed non-secret paths. Never stash secrets expecting a security boundary. Then create a recovery branch from the verified tag; old branch and commits remain available.

```bash
git status --short
git stash push -u -m 'safe reviewed work before recovery' -- REVIEWED_PATH
git stash list
git switch -c recovery/week-04 w04-s03-green
```

Inspect `git stash show --stat` before using `git stash apply`; prefer apply over pop so the backup remains until verified. Resolve conflicts in a separate commit; do not force the tag to move. Restore database from scratch backup only in a scratch instance until explicit production recovery approval.

## Commands never to use as routine recovery

Do not run `git reset --hard`, `git clean -fd`, force push, history rewriting, `docker compose down -v`, or `docker system prune`. They can destroy work or data. Do not revert unrelated user changes. Database rollback is not the same as changing a Git branch; use additive migrations and verified restore procedures.
