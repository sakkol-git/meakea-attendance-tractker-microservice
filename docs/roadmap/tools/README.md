# Documentation tools

## Linter

`docs-lint.py` uses Python 3 standard library only. Run from repository root before each commit.

```bash
python3 docs/roadmap/tools/docs-lint.py docs
```

It reports `file:line: rule: message` and exits nonzero for errors. It checks format, links, steps, stone tags, weekly file inventory, pattern sections, and sequential step IDs. Deliberately broken fixtures are temporary and never committed. This tool does not execute guide commands.
