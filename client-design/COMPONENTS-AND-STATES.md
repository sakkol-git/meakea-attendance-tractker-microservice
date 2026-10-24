# HRFlow components and states

Status: proposed v0.1 · 23 October 2026

This is a presentation contract for future Figma and client work. Component names are conceptual and do not imply implementation or installed dependencies. Use the [design tokens](DESIGN-SYSTEM.md) and [text roles](TYPOGRAPHY.md).

## Core components

| Component | Anatomy and sizing | Required behavior |
| --- | --- | --- |
| Primary button | Solid primary fill; action text; optional leading icon; 48-unit mobile / 44-unit web minimum height; 16-unit horizontal padding | Default, hover on pointer devices, pressed, focused, disabled, loading; retain a clear label while loading; prevent accidental duplicate actions |
| Secondary button | Surface fill, control border, primary text; same sizing as primary | Same relevant states; lower emphasis than the main task |
| Destructive button | Danger text or danger fill with white text for a dedicated destructive confirmation | Explicit verb; explain consequential destructive outcomes; do not use for ordinary cancellation of an unsaved form |
| Icon button | 20–24-unit icon in a 48-unit mobile / 44-unit web minimum target | Accessible name, tooltip on web where useful, visible focus |
| Text field | Persistent label, field, optional helper, error slot; 48-unit mobile / 44-unit web minimum field height | Empty, filled, focused, error, disabled, read-only; correct keyboard/input type; preserve values after errors |
| Select / date / time field | Field anatomy plus recognizable trigger and selected value | Keyboard-accessible web picker or appropriate native control; cancel preserves previous value; show invalid ranges |
| Text area | Persistent label, comfortable multi-line height, optional character counter | Grow or scroll without hiding input; show limits only when backed by the contract |
| Status badge | Status text plus optional icon; paired semantic colors; 6-unit vertical and 10-unit horizontal padding | Never interactive unless explicitly presented as a control; wrap under text scaling |
| Card | Surface, optional subtle border, 16-unit mobile / 24-unit desktop padding, 16-unit radius | Clear title and content hierarchy; avoid making a card ambiguously clickable |
| List row | Identity/title, supporting details, optional status and action | Comfortable content-led height; separate nested actions from the main link |
| Data table | Header, rows, named actions, loading/empty/error region | Accessible headers; visible sorting state; pagination/filter controls when data volume requires them |
| Alert | Icon, concise title, explanation, optional recovery action | Inline for persistent context; errors remain until resolved/dismissed appropriately |
| Toast / snackbar | Short outcome and optional useful action | Announce accessibly; do not make it the only location for an essential error |
| Dialog / sheet | Title, description, content, action area, appropriate close control | Focus management on web; scrollable content; safe-area and keyboard handling on mobile |
| Navigation | Icon, text, visible active destination | Selection uses shape/weight as well as color; selected route announced; labels remain visible |

Use native controls where they provide better accessibility or device behavior. Platform appearance may differ while semantic meaning stays consistent.

## Form rules

- Place one clear primary action at the end of the form. Use “Submit request” rather than generic “Submit” when it clarifies the outcome.
- Label required or optional fields consistently. Do not rely on a red asterisk alone.
- Validate after meaningful interaction or submission; avoid showing errors before the user has attempted input.
- Put field errors beside their fields. On web, provide a summary/focus path for multiple errors after submission.
- A pending operation must not appear complete. Keep form data until success is confirmed.
- Warn about unsaved changes when leaving would discard meaningful work.
- Use a character limit such as the mockup's 250 only if the API/product contract confirms it.

## Screen-wide asynchronous states

| State | Presentation | User action |
| --- | --- | --- |
| Initial loading | Stable skeleton or labeled progress indicator | Wait; avoid showing fake values |
| Empty | Explain what is absent and whether filters caused it | Create a request or clear filters where applicable |
| Loaded | Show real values and meaningful status | Continue the task |
| Refreshing | Keep current data visible; show refresh activity | Continue reading where safe |
| Recoverable failure | Explain what failed without erasing existing input/data | Retry or return safely |
| Offline | Persistent connectivity notice; distinguish cached records | Retry when connected; queue only supported operations |
| Session expired | Explain sign-in is needed | Reauthenticate; preserve drafts only through an approved storage policy |
| Permission denied | Explain the unavailable action without exposing restricted data | Return to an authorized destination |

## Attendance presentation

The existing home screen needs a defined primary task. Proposed default: show today's attendance state, the next valid action, and a separate “Report an attendance issue” route. Capture methods and policy verification remain unresolved.

| Conceptual state | Visible treatment | Rule |
| --- | --- | --- |
| Not clocked in | “Not recorded”; primary “Clock in” action if permitted | Do not render zero time as a record |
| Clocked in | Recorded clock-in time; next permitted action | Show “Clock out” only when allowed by the workflow |
| Completed | Recorded times and labeled duration | Distinguish recorded outcome from any ongoing review |
| Submitting | “Recording…” with progress | Prevent duplicate gestures; do not announce success early |
| Queued offline | “Waiting to sync” with local timestamp | Show only if offline submission is supported; local queue is not confirmed attendance |
| Pending verification/review | Explicit label and available explanation | Different from transport/sync progress |
| Failed | Failure explanation and available next action | Retry must respect eventual command/idempotency handling |
| Missing record | “Not recorded” and correction route where allowed | Do not invent hours or imply the employee was absent |

These are conceptual UI states. Map them to verified domain/API states before implementation; never create backend enums from this table alone.

### History variants

- Attendance history: date, recorded times, duration, and clearly explained exceptions.
- Remote-work request history: requested period, request status, and detail/decision access. It must not duplicate the attendance table without request information.
- Attendance issue history: affected date, issue type, submitted date, review status, and detail access.
- Add date-range/filter behavior and record details deliberately; do not imply prototype rows already supply them.

## Leave and approval presentation

| Component or view | Required information |
| --- | --- |
| Balance summary | Leave type, available amount and unit, applicable period; pending reservations only if supported and explained |
| Request card/row | Type, dates, duration/unit, submission state, detail action |
| Request form | Supported type/unit, date or time range, reason, policy-backed required attachments, validation, clear submit outcome |
| Request detail | Full request, actual status, relevant decisions/reasons, permitted next actions |
| Approval view | Request context, authorized decision actions, reason requirements from policy, submission feedback |

Proposed display vocabulary: Draft, Pending approval, Approved, Rejected, Cancelled. Use only states the product actually supports. A rejected request is not a network error; a failed submission is not a rejected request.

## Profile and PIN

- Separate identity/contact editing from settings and authentication controls.
- Show editable versus organization-managed fields explicitly. An editable-looking email field must not imply unrestricted identity changes.
- For profile photo selection, cover preview, cancel, upload progress, and failure while preserving the existing photo.
- Distinguish account login from local app unlock and PIN setup. A PIN success message must not claim it authorizes transactions unless that is an actual product feature.
- PIN setup should distinguish creating a PIN, confirming it, mismatch, and saved outcome. PIN unlock needs clear failure/recovery behavior based on security policy.
- Define recovery and retry limits with the authentication implementation; do not invent permissive behavior in the UI.
- Support accessible entry and platform input behavior. The custom Figma keypad is a visual proposal, not a requirement to replace native input.
- Success must offer a clear continuation or a documented transition. Decorative success artwork cannot be the entire flow.

## Web composition direction

The web page has no existing HRFlow application layouts. The following are proposed building blocks, not completed screen designs or confirmed page scope:

- Application shell: organization context, route title, authorized navigation, account menu, and notification entry point if implemented.
- Dashboard: useful summaries with clear time period and links to the underlying records; avoid unsupported or fabricated metrics.
- Employee directory: search/filter, identity and organization context, permitted record actions.
- Attendance workspace: date range, readable records, exceptions, and authorized detail/correction paths.
- Approval workspace: pending work, request details, decision feedback, and appropriate access boundaries.

Use the same labels and state meanings on web and mobile. Actual navigation visibility must follow permissions; hiding a control is not access enforcement.

## Component review before release

- Review the complete task, including empty and failed states, rather than isolated happy-path screenshots.
- Test keyboard focus/order and accessible names on web; text scaling, touch targets, safe areas, and keyboard avoidance on mobile.
- Confirm status meanings with actual contracts and show no false success while an action is queued or awaiting review.
- Check long employee names, large values, translated labels, multi-line errors, and narrow layouts.
- Verify visual consistency against this documentation, and record intentional deviations instead of silently forking the system.
