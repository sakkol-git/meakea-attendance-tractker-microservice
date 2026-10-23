# V2 linter proof results

## Method and real-doc result

Run python3 docs/roadmap/tools/prove-docs-lint.py from repository root. It first requires the real docs to pass, then makes an isolated temporary copy for each mutation, proves the clean copy passes, changes one rule and checks the expected rule ID/exit. Application source is never copied or modified. The parent README is included so relative root links remain valid in the temp copy. All temp copies are removed automatically.

Real-doc result at this run: **PASS**, 273 Markdown files, zero errors and zero warnings. Additional validation-log files added later are covered by the final full lint. Detailed machine results are preserved in docs/validation/DOCS-LINT-V2-PROOFS.json.

| Broken fixture | Rule | Expected exit | Actual result |
| --- | --- | --- | --- |
| missing Why | step | nonzero | **PASS**, exit 1 |
| missing Do | step | nonzero | **PASS**, exit 1 |
| missing Expect | step | nonzero | **PASS**, exit 1 |
| missing Common mistake | step | nonzero | **PASS**, exit 1 |
| missing Undo | step | nonzero | **PASS**, exit 1 |
| missing Senior habit | step | nonzero | **PASS**, exit 1 |
| missing Check | step | nonzero | **PASS**, exit 1 |
| missing If it fails | step | nonzero | **PASS**, exit 1 |
| missing Status | step | nonzero | **PASS**, exit 1 |
| duration over 90 | duration | nonzero | **PASS**, exit 1 |
| duplicate ID | step-ids | nonzero | **PASS**, exit 1 |
| nonsequential ID | step-ids | nonzero | **PASS**, exit 1 |
| unresolved map target | step-map | nonzero | **PASS**, exit 1 |
| missing old map row | step-map | nonzero | **PASS**, exit 1 |
| missing enterprise gate | enterprise-gate | nonzero | **PASS**, exit 1 |
| missing minimum gate | minimum-gate | nonzero | **PASS**, exit 1 |
| capacity overflow | capacity | nonzero | **PASS**, exit 1 |
| capacity arithmetic mismatch | capacity-total | nonzero | **PASS**, exit 1 |
| missing Q introducing week | quality-week | nonzero | **PASS**, exit 1 |
| missing Q enforcing ID | quality-steps | nonzero | **PASS**, exit 1 |
| missing literal Q gate | quality-gate | nonzero | **PASS**, exit 1 |
| vague title warning | vague-title | zero with warning | **PASS**, exit 0 |

## Checks and limits

The old formatting/links/stone rules are retained. Added checks enforce Common mistake/Undo/Senior habit alongside existing fields, duration, unique sequential week IDs and matching tag endings, five-to-eight stones/week, complete old/draft maps with resolvable targets, Enterprise/Minimum gates, three-to-six Q tasks, numeric single-source capacity and README sums from actual upper step durations plus explicit extra hours, and all fourteen introducing-week/enforcing-ID/literal-gate chains.

Vague-verb title detection is intentionally a warning: titles can be linguistically ambiguous, so this heuristic cannot block a valid step. File-count/new-concept/observable-result quality and real deployment behavior remain human-reviewed; a linter cannot establish them from a keyword alone. No existing format check was downgraded. Real docs currently have zero warnings.
