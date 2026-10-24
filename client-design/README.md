# HRFlow client design documentation

Status: design foundation + mobile implementation · 24 October 2026

This directory defines a shared design direction for the Flutter mobile client and Nuxt/Vue web client. The mobile client now applies the teal typography and component direction, with local workflows and a separate remote adapter. The Nuxt web UI remains a starter with data-source infrastructure. No Figma library publication or native-device validation is claimed. The canonical synthetic seed is kept here alongside the integration notes.

## Read in this order

1. [Design system](DESIGN-SYSTEM.md): principles, colors, layout, spacing, shape, and interaction rules.
2. [Typography](TYPOGRAPHY.md): English/Khmer fonts, type scale, content hierarchy, and formatting.
3. [Components and states](COMPONENTS-AND-STATES.md): reusable controls and domain-specific state patterns.
4. [Data sources](DATA-SOURCES.md): local/remote status and client-specific handover.
5. [Mobile delivery](../mobile/README.md): implemented screens, architecture, workflow coverage and validation.

These documents own proposed client presentation rules. Backend contracts and authoritative product requirements remain in [`docs/`](../docs/SOURCE-OF-TRUTH.md). A visual example does not establish an API field, permission, leave entitlement, or attendance policy.

## Evidence and decisions

| Source | Observed | How this proposal uses it |
| --- | --- | --- |
| [Mobile Figma page](https://www.figma.com/design/c9A6nVzLu14iw7H6X9bnNQ/ITE-Y4_Business_Portal?node-id=10-5) | Sixteen mobile mockups; teal `#006151`; Kantumruy Pro; white cards, gray canvas, rounded controls; Home, History, Leave, Profile navigation | Preserve the recognizable teal direction, font family, and employee task structure; improve legibility and incomplete states |
| [Web Figma page](https://www.figma.com/design/c9A6nVzLu14iw7H6X9bnNQ/ITE-Y4_Business_Portal?node-id=224-92) | Reference boards labeled Relay / Business Portal, including blue/navy and separate teal treatments; Inter; no HRFlow application screens | Treat as reference material, not an approved HRFlow web design or bound token library |
| [`mobile/lib/main.dart`](../mobile/lib/main.dart) | Flutter mobile workspace | Implemented mobile extension of the unfinished Figma reference; see [mobile README](../mobile/README.md) |
| [`web/app/app.vue`](../web/app/app.vue) | Nuxt welcome starter | Use Nuxt/Vue conventions in future implementation; React examples in Figma do not dictate the stack |

All prescriptions below are proposed decisions unless explicitly labeled **Observed**. They are ready for review, not a claim of user approval.

## Proposed foundation

- One teal-led identity across both clients; platform-appropriate layouts and controls.
- Kantumruy Pro for English and Khmer UI, with language-aware line height.
- Comfortable mobile controls and readable text instead of reproducing tiny Figma labels.
- Explicit pending, failure, and offline feedback; success requires confirmed application state.
- Shared semantic token names, with separate CSS and Flutter representations later.

## Decisions to resolve before implementation

| Decision | Working assumption | What needs confirmation |
| --- | --- | --- |
| Web audience | Employee self-service plus authorized management workspaces | Actual roles and page permissions from product/API contracts |
| Branding | Teal is the shared primary color | Whether the mobile identity should extend to web |
| Languages | English baseline; typography supports Khmer | Khmer translation scope and release priority; project roadmap currently places localization in Tier 2 |
| Attendance | A clear clock-in/out action and separate correction route | Capture method, verification steps, remote-work policy, offline eligibility |
| PIN | A distinct device/app unlock flow | Its relationship to account login, recovery, and retry policy |
| Leave and approvals | Shared presentation patterns | Supported units, types, balances, cancellation rules, approval stages |

Unresolved business decisions must not be silently encoded through labels or interactions.

## Shared design acceptance criteria

- Both clients use the documented semantic colors, spacing, and type roles.
- Core components cover their relevant interactive and asynchronous states.
- Mobile is reviewed at 360, 402, and 430 logical pixels, with keyboard and larger text.
- Web is reviewed at 390, 768, 1024, and 1440 CSS pixels, including keyboard operation and browser zoom.
- English and representative Khmer text are checked for wrapping, missing glyphs, clipping, and baseline issues before Khmer release.
- Contrast, focus, accessible labels, screen-reader announcements, and non-color status cues are verified in the actual clients. No accessibility conformance is claimed by this document.
- Visual differences and unresolved product decisions are recorded before sign-off.
