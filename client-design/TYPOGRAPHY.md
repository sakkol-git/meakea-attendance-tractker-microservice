# HRFlow typography system

Status: proposed v0.1 · 23 October 2026

## Typeface decision

**Observed:** mobile mockups use Kantumruy Pro for application content and Figtree for simulated system status text. Web reference boards use Inter and technical monospace families.

**Proposed:** use Kantumruy Pro for both English and Khmer application UI across mobile and web. This retains the mobile identity and avoids defining two unrelated typographic systems before the web screens exist. The Inter web references remain inspiration, not an implementation requirement.

Use regular 400, medium 500, semibold 600, and bold 700 weights. Bundle the required licensed font files during implementation and record their license; the font assets are not included here. Check glyph coverage and real weight availability before adopting a particular distribution. Avoid synthetic bold.

| Context | Proposed family/fallback |
| --- | --- |
| Web application text | `"Kantumruy Pro", "Noto Sans Khmer", system-ui, sans-serif` |
| Flutter application text | Bundled `KantumruyPro`, with a tested Khmer-capable fallback where needed |
| Identifiers requiring fixed alignment | Platform monospace; always preserve an accessible full value |
| OS status bar and native system UI | Platform-managed typography |

Noto Sans Khmer is an optional fallback to provision and test, not an asset assumed to exist. Technical details do not belong in ordinary user flows unless they help a user complete a task.

## Type scale

Each role lists font size / line height in logical units, followed by weight. These are baseline English metrics at default scaling, not fixed container heights. Khmer line-height guidance follows below.

| Semantic role | Mobile | Web | Use |
| --- | --- | --- | --- |
| `type.display` | 32 / 40 · 600 | 40 / 48 · 600 | Rare welcome or high-level overview heading |
| `type.pageTitle` | 24 / 32 · 600 | 30 / 40 · 600 | Main page heading |
| `type.sectionTitle` | 20 / 28 · 600 | 20 / 28 · 600 | Major section or dialog heading |
| `type.cardTitle` | 18 / 26 · 600 | 18 / 26 · 600 | Card heading or emphasized record title |
| `type.body` | 16 / 24 · 400 | 16 / 24 · 400 | Main content, field values, explanatory text |
| `type.bodySmall` | 14 / 22 · 400 | 14 / 22 · 400 | Supporting content and standard table cells |
| `type.label` | 14 / 20 · 500 | 14 / 20 · 500 | Field labels and compact navigation |
| `type.action` | 16 / 24 · 600 | 14 / 20 · 600 | Button labels; mobile web uses the mobile treatment |
| `type.caption` | 12 / 18 · 400 | 12 / 18 · 400 | Nonessential metadata and timestamps |
| `type.status` | 14 / 20 · 500 | 14 / 20 · 500 | Request and attendance status |
| `type.metric` | 28 / 36 · 600 | 32 / 40 · 600 | Dashboard counts and important time values |
| `type.navigation` | 12 / 18 · 500 | 14 / 20 · 500 | Bottom navigation labels / web sidebar labels |

### Hierarchy rules

- Use one primary page heading. Web heading levels must reflect the document structure rather than whichever size looks convenient.
- Prefer sentence case: “Leave requests”, “Attendance history”, “Save changes”. Avoid all-caps page titles and success messages.
- Use semibold for emphasis; bold is reserved for exceptional emphasis. Do not make every label and value bold.
- Keep essential status, error messages, and action text at 14 units or larger at baseline. Do not reproduce the initial 9px attendance statuses.
- Avoid placing an oversized greeting above the everyday task. Show the employee name or helpful context instead of placeholder promotional copy.
- Use zero extra letter spacing by default, especially for Khmer. Do not track uppercase labels as a universal style.
- Use tabular numerals for clocks, durations, and numeric tables when supported by the selected font. Verify support rather than assuming it exists.

## English and Khmer behavior

Typography supports Khmer structurally; this does not expand the current release's translation scope.

- Mark web content with the correct language attribute (`en` or `km`) and set the application's locale in Flutter.
- For Khmer body, labels, captions, and status roles, start at a line height of at least 1.6 times font size. For headings and metrics, start at 1.4 times size. Retain the role's weight and adjust after visual review.
- Increase component height to fit those metrics. For example, a 14-unit Khmer label starts at a 23-unit line height; do not force it into a fixed 20-unit text box.
- Never apply fixed-height text boxes or aggressive clipping around combining marks. Check text above and below the apparent baseline.
- Allow multi-line labels and button growth when needed. Do not insert arbitrary spaces into Khmer to force wrapping.
- Test mixed Khmer, English names, numerals, punctuation, and email addresses in the same record.
- Confirm actual font rendering on both Android and iOS and the browsers used for web review.

## Responsive text and scaling

- On web, express font sizes in `rem`; a listed size of 16 corresponds to `1rem` under the default 16px root. Do not lock the root to defeat user preferences.
- On Flutter, use logical font sizes and respect system text scaling. Do not disable scaling to preserve a mockup.
- At narrow web widths, use the mobile page-title and display sizes. Body and error text should stay readable rather than shrink to fit.
- Review at default and 200% text scaling/zoom as an internal acceptance check. Expand rows, stack columns, and allow scroll before truncating essential content.
- A line-height token sets text rhythm, not the height of its parent control.

## Wrapping and density

| Content | Rule |
| --- | --- |
| Paragraphs | Left aligned; aim for 45–75 Latin characters per line on wide layouts |
| Page headings | Allow two lines on small screens |
| Names and request titles | Wrap in details; lists may use two lines with access to the full record |
| Status and errors | Preserve the full meaning; wrap instead of hiding essential text |
| Table cells | Use 14 / 22 baseline text; wrap or provide deliberate column sizing |
| Long identifiers | Wrap safely or provide accessible copy/full-value behavior |
| Numbers | Right align comparable quantities in web tables |

Do not reduce text size to solve overflowing tables or oversized forms. Change the layout first.

## Dates, times, and quantities

- Display human-readable dates using the user's selected locale. Compact English example: “23 Oct 2026”; detail example: “Friday, 23 October 2026”.
- Use the organization's relevant timezone for attendance records; show the timezone when ambiguity matters. Do not silently reinterpret records using the viewer's device timezone.
- Use a consistent user/organization time convention. Example: “09:00” in 24-hour format or “9:00 AM” in 12-hour format. Never display “00:00 PM”.
- Label durations explicitly: “7h 25m” or “7 hours 25 minutes”. A bare “07:25” can be mistaken for a time of day.
- Use “Not recorded” for an absent clock time. Zero is a value, not an empty-state placeholder.
- Show leave units explicitly, such as “2 days” or “2 hours”, only where the product policy supports those units.
- Example values are fixtures, not actual entitlements or company policy.

## UI writing

| Initial wording | Proposed wording or rule |
| --- | --- |
| “Chock Out” | “Clock out” |
| “Leaves request” | “Leave requests” |
| “Haft Day Application” | “Half-day request” |
| “Come late” | “Late arrival” |
| “Decline” as a completed status | “Rejected”, if that matches the domain state |
| “Awaiting” | “Pending approval”, if approval is the actual next step |
| “Confirming” | Name the real state: “Syncing” or “Pending review”; these meanings are distinct |
| “Cannot Clock In/Out” | “Report an attendance issue” for an exception route; normal recording needs its own action |
| “Enter your PIN agian” | “Incorrect PIN. Try again.” for a failed unlock; “PINs do not match” for confirmation mismatch |
| “Forget PIN?” | “Forgot PIN?” with a defined recovery route |
| “SUCCESS” | “PIN saved”, with an explicit next step |

Use direct labels: “Submit request”, “Save changes”, “Cancel request”. Error copy should state the problem and an available recovery action. Do not promise approval, synchronization, or recovery that the system cannot perform.

## Typography review checklist

- All content uses a named role instead of arbitrary font sizes.
- Key tasks remain legible without tiny metadata or color interpretation.
- English and Khmer glyphs render with the intended weights and without clipping.
- Increased text size does not hide controls, error messages, or navigation labels.
- Dates, times, duration units, and status words remain consistent across both clients.
- Placeholder names, lorem ipsum, and unrelated business-portal content are replaced before demonstration.
