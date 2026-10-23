# Canonical Markdown formatting standard

> **Note:** This standard defines the cross-platform Markdown contract for all HRFlow technical documentation. It guarantees clean rendering across IntelliJ IDEA Markdown Preview, Notion import, and Obsidian Reading and Editing views without requiring proprietary plugins.

## Core principles

Every document in the HRFlow documentation suite must adhere to standard GitHub-Flavored Markdown. To ensure portability across diverse viewers, documents must avoid editor-specific extensions, custom HTML styling, and brittle syntax.

- Predictable layout: Every file follows a uniform structure with an explicit title, logical sections, and actionable next steps.
- Plain text portability: The documentation remains fully readable when viewed in raw text, terminal pagers, or code diffs.
- Automated verification: Format compliance is strictly enforced by the repository linter before any commit is accepted.
- Zero silent failures: Status indicators, test results, and gate criteria must use explicit bold status keywords.

## Heading hierarchy

Documents must maintain an unambiguous heading outline. Navigators and outline panes rely on consistent heading levels to build the document tree.

- Exactly one H1 title at the first line of the document.
- Use H2 for major structural sections.
- Use H3 for detailed subsections within an H2 section.
- Avoid H4 or deeper headings; restructure deeply nested content into separate focused documents.
- Never skip heading levels (e.g., jumping from H1 directly to H3).
- Headings must consist of plain text only. Do not place backticks, links, bold tags, or inline code inside headings.

## Paragraphs and text formatting

Prose explanations must prioritize clarity, density, and readability.

- Write one logical thought per line without hard-wrapped line breaks.
- Separate paragraphs with exactly one blank line.
- Use bold formatting selectively for key terminology, requirements, and status indicators.
- Avoid decorating entire sentences or paragraphs with bold or italic markers.
- Wrap inline variables, placeholders, file paths, and environment settings in backticks.
- Dollar signs (`$`) and hash marks (`#`) must be wrapped in backticks to prevent unwanted math or tag rendering.
- Raw HTML tags, line break tags, and custom font styling are strictly prohibited.

## Lists and actionable checklists

Lists structure non-sequential items, procedural workflows, and verification tasks.

- Unordered lists must use the hyphen marker (`-`).
- Ordered lists must use the number-dot marker (`1.`).
- Nested lists must use four spaces of indentation per level, capped at two levels of nesting.
- Always include a blank line before starting any list block.
- Checklists use the standard format `- [ ]` for incomplete tasks.
- Authored guides must leave checklist items unchecked (`- [ ]`); tasks are checked only during active execution.
- Checklists must never be placed inside table cells.

## Tables

Tables are reserved for structured comparisons, schema definitions, and concise status matrices.

- Limit tables to a maximum of six columns to prevent horizontal overflow in mobile and notebook screens.
- Every table must include a complete header row and a standard separator row (`| --- |`).
- Never leave table cells empty; use an em dash (`—`) when no value is applicable.
- Do not embed code blocks, multi-paragraph text, or task checkboxes inside table cells.
- Keep table cell text concise; link to referenced documents for extensive explanations.

## Code blocks and command conventions

Code examples and commands must provide complete, copy-pasteable context.

- Fenced code blocks must declare an explicit language identifier from the approved set: `java`, `sql`, `yaml`, `bash`, `json`, `ts`, `dart`, `xml`, `properties`, `text`, or `mermaid`.
- Individual lines within code blocks must not exceed 100 characters in width.
- Shell command blocks must omit shell prompts (`$` or `#`) to allow clean copying.
- Every command must clearly state its working directory, prerequisites, expected output, and failure remediation.
- Never place actual passwords, JWT secret keys, API tokens, or production hostnames in code examples.

## Callouts and blockquotes

Portable callouts use standard Markdown blockquotes with an approved bold prefix.

- Callouts must begin with one of the approved labels: `> **Note:**`, `> **Warning:**`, `> **Decision:**`, `> **Evidence:**`, `> **Pitfall:**`, or `> **Navigation:**`.
- Keep callout text focused on a single paragraph.
- Do not use Obsidian admonition syntax (such as `[!NOTE]`) or Notion custom callout blocks.

## Diagrams and visual models

Mermaid diagrams provide architectural and workflow visualizations without external binary assets.

- Supported diagram types are `flowchart`, `sequenceDiagram`, `erDiagram`, and `stateDiagram-v2`.
- Flowchart node labels must be enclosed in double quotes within brackets (e.g., `A["Client"]`).
- Limit diagrams to at most 15 nodes to maintain readability on standard screens.
- Custom node styling, CSS classes, and link styles are forbidden.
- Every Mermaid block must be immediately followed by a `Text version:` label and a bulleted list describing the flow.

## Links and cross-references

Navigation relies exclusively on relative Markdown links.

- Links must point directly to existing `.md` files using relative paths (e.g., `[Architecture](../Architecture.md)`).
- Anchor links (using `#`), folder-only links, and direct source code links are forbidden in portable documentation.
- Use descriptive link text that identifies the target document rather than generic phrases like "click here."

## Target platform verification

The documentation suite must be validated across the three primary viewing environments:

- IntelliJ IDEA: Open the built-in Markdown Preview. Verify heading outlines, table borders, syntax highlighting, and link traversal.
- Notion: Import Markdown documents via the standard page import. Confirm that tables, code fences, and blockquotes render cleanly without markdown syntax leakage.
- Obsidian: Open the `docs` folder as a dedicated vault. Ensure that reading view displays callouts, lists, and Mermaid diagrams correctly without requiring third-party plugins.

## Automated validation command

Run the repository linter before every commit to ensure zero formatting errors:

```bash
python3 docs/roadmap/tools/docs-lint.py docs
```

The script verifies file encoding, line endings, heading structure, table widths, code fences, Mermaid syntax, and relative link targets. All checks must pass with zero errors.

## V2 atomic step fields

Between Expect and Check every upgraded step includes these single-line fields. Why, Do, Expect, Check, If it fails and Status remain mandatory. State a 30–90 minute duration range, target one result and one linked concept, include a real output sample and explicit Run/Not run status. Usually edit at most three files; unknown names require Inspect first.

**Common mistake:** One sentence describing the likely error for this particular step.

**Undo:** `git revert HEAD` for the isolated un-deployed step commit; applied data changes require the reviewed forward-repair procedure.

**Senior habit:** The one professional practice taught by this step.

No existing formatting rule is weakened. Phase 6 will enforce these fields across the entire upgraded weekly set, plus capacity and quality traceability; the current linter does not yet prove them.
