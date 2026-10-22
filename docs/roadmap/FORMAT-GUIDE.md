# Portable Markdown format guide

## Rules

UTF-8, LF, no tabs/trailing spaces, exactly one final newline. One H1 at line 1, headings H1 through H3 only, no skipped levels or inline markup in headings. One paragraph per line. Files stay under 500 lines; long historical documents are split without discarding their content.

Lists use hyphen or repeated `1.` markers; nested lists use four spaces and at most two levels. Blank line before lists. Tasks stay unchecked in the authored guide and have one-line labels. Commands follow tasks in separate fenced blocks.

Only matrix tables, maximum six columns, header separator, no empty cells or tasks. Fences require java, sql, yaml, bash, json, ts, dart, xml, properties, text, or mermaid; code lines at most 100 characters. No prompts or nested fences. No front matter, HTML, wikilinks, footnotes, math, highlights, horizontal rules, or hard line breaks.

Wrap literal placeholders, dollar signs, hash words, wiki/template delimiters in inline code. Relative links target explicit Markdown files without spaces or anchors; external links use HTTPS. Upper-kebab filenames are required except README.md and the explicitly requested canonical Architecture.md.

Callouts are single-paragraph blockquotes with bold Note, Warning, Decision, Evidence, or Pitfall labels. Weekly navigation uses the explicitly requested Navigation label. Mermaid labels are quoted, no styling, at most 15 nodes, approved types only, and followed by a Text version bullet list. Optional diagrams never carry information unavailable in text.

## Three viewer setup checks

IntelliJ: enable Markdown preview and verify Mermaid support in your installed version; open the selftest and compare list/tasks/code/table/link rendering. Obsidian: open `docs` as vault, disable wikilink generation, use relative paths, and verify in your version. Notion: import a ZIP of Markdown plus relative files, then verify in your version that relative links and Mermaid survived; use Text version if Mermaid renders as code. A live local relative link cannot be guaranteed after Notion transforms imports into pages.

No tool access has proven all three viewers render this suite. The three-minute selftest is a user acceptance gate; cross-viewer rendering remains **NOT RUN** until recorded.

## Validation

```bash
python3 docs/roadmap/tools/docs-lint.py docs
```

Before every documentation commit, zero errors is required. The linter enforces syntactic portability and guide structure; it cannot prove estimates, business semantics, viewer plugins, or a working deployment.
