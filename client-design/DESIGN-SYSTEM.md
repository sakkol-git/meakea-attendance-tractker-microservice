# HRFlow design system

Status: implemented shared foundation v0.2 · 25 October 2026

See [source evidence and open decisions](README.md) and the [typography specification](TYPOGRAPHY.md).

## Principles

1. Make the next task clear. Home should prioritize the current attendance action and today's status; exception handling remains available nearby.
2. Make state understandable. Distinguish a locally queued action, a server receipt, a policy review, and a confirmed outcome.
3. Keep related information together. A request's dates, duration, status, and next action belong in one readable unit.
4. Share identity across platforms. Reuse semantic tokens and terminology, while adapting navigation and information density to the available space.
5. Prefer restrained surfaces. Use spacing, borders, and hierarchy before strong shadows or large decorative headers.

## Cross-client brand contract

Mobile and web implement one HRFlow identity. The shared contract is Kantumruy Pro, primary teal `#006151`, the semantic colors below, the same status meanings, and the same product terminology. New screens must consume the existing Flutter theme or CSS tokens instead of introducing a feature-specific brand palette.

The platforms may express the contract through their native interaction patterns: mobile uses safe areas, bottom navigation and touch-first controls; web uses a sidebar, responsive tables, keyboard focus and pointer states. These are intentional platform adaptations, not different branding. Screen density and navigation placement may differ while labels, workflow outcomes, validation, status meaning and action hierarchy remain consistent.

The implemented sources are:

- Flutter: `mobile/lib/core/theme.dart` and the bundled `KantumruyPro` font family.
- Nuxt: `web/app/assets/css/tokens.css` and `@fontsource-variable/kantumruy-pro`.
- Product language and state rules: this document, `CONTENT-GUIDE.md`, and the accepted shared API contract.

Review any token change in this document and both client implementations together. A platform-only token is allowed when the other platform has no equivalent interaction, such as web pointer hover, but it must remain derived from this palette.

## Color system

**Observed:** the mobile primary color is `#006151`. The remaining values are proposed to support that identity. The blue/navy web reference palette is not adopted as a second HRFlow brand palette.

| Semantic token | Value | Use |
| --- | --- | --- |
| `color.brand.primary` | `#006151` | Primary actions, selected navigation, brand emphasis |
| `color.brand.hover` | `#005447` | Pointer hover on primary actions |
| `color.brand.pressed` | `#00463B` | Pressed primary actions |
| `color.brand.subtle` | `#E8F4F0` | Selected backgrounds and quiet brand emphasis |
| `color.canvas` | `#F5F7F8` | Application background |
| `color.surface` | `#FFFFFF` | Cards, menus, dialogs, form surfaces |
| `color.surface.subtle` | `#EEF2F3` | Inset sections and neutral placeholders |
| `color.text.primary` | `#172B29` | Headings and body text |
| `color.text.secondary` | `#526560` | Supporting copy and placeholders |
| `color.text.on-brand` | `#FFFFFF` | Text on primary brand surfaces |
| `color.border.subtle` | `#D9E2DF` | Decorative separators and card borders |
| `color.border.control` | `#71857F` | Boundaries needed to identify fields and controls |
| `color.focus` | `#006151` | Visible focus ring on light surfaces |
| `color.focus.on-brand` | `#FFFFFF` | Visible focus ring on dark brand surfaces |
| `color.success.text` | `#17613F` | Confirmed/success label and icon |
| `color.success.surface` | `#EAF6EF` | Success badge or notice background |
| `color.warning.text` | `#805400` | Pending attention or caution label and icon |
| `color.warning.surface` | `#FFF4D6` | Warning badge or notice background |
| `color.danger.text` | `#A12B2B` | Errors, rejected outcomes, destructive actions |
| `color.danger.surface` | `#FCEDED` | Error badge or notice background |
| `color.info.text` | `#245A91` | Informational notices and synchronization activity |
| `color.info.surface` | `#EAF2FC` | Information badge or notice background |
| `color.neutral.text` | `#526560` | Draft, inactive, or unavailable status |
| `color.neutral.surface` | `#EEF2F3` | Neutral badge background |
| `color.disabled.surface` | `#E4EAE7` | Disabled filled control background |
| `color.disabled.text` | `#66766F` | Disabled control label |
| `color.overlay` | `rgba(15, 35, 29, 0.40)` | Modal backdrop |

### Application rules

- Use paired status foreground/background tokens. Include a readable label and, where helpful, a distinct icon.
- Reserve success for confirmed outcomes. Teal selection does not mean approved or successful.
- Pending is a workflow state, not an error. Show the reason and next step when known.
- Use `border.control` for inputs; `border.subtle` is primarily decorative.
- Avoid green/red icons without text in attendance history. Use labels such as Recorded, Missing, or Pending review only when those meanings match the data.
- Place white text on the solid primary color. Do not assume arbitrary tint/foreground combinations are readable.
- Check actual foreground/background pairs against internal targets of 4.5:1 for normal text, 3:1 for large text, and 3:1 for meaningful control boundaries and indicators. These are acceptance targets, not measured conformance results.
- Dark mode is outside this first specification. Do not invert this palette automatically.

## Spacing and shape

Numbers represent CSS pixels on web and logical pixels in Flutter. Keep token intent shared; do not copy physical device pixels.

| Token | Value | Typical use |
| --- | --- | --- |
| `space.1` | 4 | Tight icon/label spacing |
| `space.2` | 8 | Related inline content |
| `space.3` | 12 | Compact group spacing |
| `space.4` | 16 | Mobile gutters, card padding, field gaps |
| `space.5` | 20 | Comfortable card padding |
| `space.6` | 24 | Section gaps, desktop card padding |
| `space.8` | 32 | Major section separation |
| `space.10` | 40 | Page composition |
| `space.12` | 48 | Large page sections |
| `radius.sm` | 6 | Badges and small inset elements |
| `radius.md` | 10 | Inputs and buttons |
| `radius.lg` | 16 | Cards and menus |
| `radius.xl` | 24 | Sheet top corners and large dialogs |
| `radius.pill` | 999 | Avatars, pills, circular actions |

Use a 1-unit standard border and 2-unit selected/error emphasis when needed. Reserve stronger outlines for meaningful states.

## Elevation

| Token | Web reference | Intent |
| --- | --- | --- |
| `shadow.none` | `none` | Most cards and content surfaces |
| `shadow.low` | `0 2px 8px rgba(15,35,29,.06)` | Subtle floating card |
| `shadow.overlay` | `0 8px 24px rgba(15,35,29,.14)` | Menus, popovers, dialogs |

Flutter should approximate the same hierarchy with its native shadow model; exact blur mathematics need not match CSS. Avoid applying strong drop shadows to every list row, as seen in the initial mobile mockups.

## Responsive layout

### Mobile app

- Start with 16-unit horizontal gutters and 16–24-unit vertical section gaps.
- Retain Home, History, Leave, Profile as the initial navigation proposal. Additional domains require an explicit navigation decision.
- Use native safe-area insets. The Figma device bezel, simulated status bar, and home indicator are presentation chrome, not application assets.
- Use a 56-unit minimum app-bar content height and a 64-unit minimum bottom-navigation content height, plus relevant safe-area insets. Allow growth for text scaling.
- Let pages scroll. Do not reproduce the 402 × 874 frame as fixed absolute coordinates.
- Keep form actions reachable when the keyboard opens. Focused fields and errors must remain visible.
- At wider tablet sizes, cap form width and use space intentionally rather than stretching fields across the whole screen.

### Web app

| Width | Layout behavior |
| --- | --- |
| Below 768 | Single column; drawer navigation; 16-unit page gutters |
| 768–1023 | One or two content columns where labels fit; drawer navigation; 24-unit gutters |
| 1024 and above | 256-unit sidebar; 32-unit content gutters; persistent navigation |

- Use a 64-unit minimum top bar and a 1280-unit maximum inner content width for standard pages. Dense report/table pages may use the available content width.
- Cap ordinary forms at 640 units; use multiple columns only for related short fields with enough room for labels and errors.
- Tables may scroll horizontally inside a labeled region. Keep key identity and actions discoverable; avoid causing the entire page to overflow.
- On narrow layouts, use request/employee cards when they preserve the task better than a table.
- Breakpoints are initial guidance. Adjust a component when its content stops fitting, including with translated labels and zoom.

## Interaction and accessibility

- Target at least 48 × 48 logical units for mobile touch areas; icon artwork may remain 20–24 units.
- Standard web buttons and fields are at least 44 units high; touch-first web layouts use 48. Permit larger heights for wrapping and user text settings.
- Show web keyboard focus with a 2-unit ring and 2-unit offset. On a matching dark surface, use the inverse focus token. Never remove focus without a replacement.
- Keep labels visible above fields. Placeholders are examples, not replacements for labels.
- Provide accessible names for icon-only controls, and announce meaningful asynchronous outcomes.
- Dialogs must manage focus, provide a visible dismiss control where dismissal is allowed, and restore focus to the trigger. Prevent accidental loss of unsaved input.
- Preserve entered values on recoverable errors. Explain the failure near the affected control and provide a next action.
- Every important workflow must remain understandable without animation, hover, or color alone.

## Icons and motion

Use one consistent outline icon style per client. Lucide-style icons may guide web selection; use an equivalent licensed Flutter icon set or approved shared assets. Confirm license and package choice during implementation. Use 20-unit standard and 24-unit navigation icons, with consistent optical weight.

Use 120–160ms feedback transitions and 180–240ms panel transitions as proposed defaults. Honor reduced-motion preferences. Never delay task completion to play a decorative success animation.

## Platform translation

| Shared intent | Nuxt/Vue | Flutter |
| --- | --- | --- |
| Semantic tokens | CSS custom properties, e.g. `--color-brand-primary` | Theme extension/named constants, e.g. `brandPrimary` |
| Text roles | Named text classes or component variants | Named `TextTheme` roles or extensions |
| Controls | Semantic HTML inside reusable Vue components | Native Material controls with shared theme overrides |
| Navigation | Route-aware links; keyboard and focus behavior | Navigator/router-aware navigation destinations |
| User scaling | Relative text units; respect zoom | Respect system text scaling |

These are implementation guidelines only. No package, token source file, or generated Figma binding is introduced by this documentation.
