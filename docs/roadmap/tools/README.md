# Documentation tools

## Linter

`docs-lint.py` uses Python 3 standard library only. Run from repository root before each commit.

```bash
python3 docs/roadmap/tools/docs-lint.py docs
```

It reports `file:line: rule: message` and exits nonzero for errors. It checks format, links, steps, stone tags, weekly file inventory, pattern sections, and sequential step IDs. Deliberately broken fixtures are temporary and never committed. This tool does not execute guide commands.

## V2 semantic checks and proof harness

The linter also validates required V2 fields, tag/ID maps, quality gate coverage and owner-hour arithmetic from the single dashboard input. Every README declares Planned hours and Extra hours; reading is included in step durations, while extra review/mentoring is added once. The vague-title heuristic warns rather than errors; scope/file/concept judgments remain human review.

```bash
python3 docs/roadmap/tools/prove-docs-lint.py
```

[Proof results](../LINTER-PROOFS.md) show each isolated failing temp fixture and the real-doc pass. The proof harness uses only the standard library and deletes its temporary document copies. Neither tool executes future application/production commands.
