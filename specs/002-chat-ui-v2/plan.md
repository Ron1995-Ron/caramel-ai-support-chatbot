# Implementation Plan: Caramel AI Support Chat UI, Version 2

**Branch**: `002-chat-ui-v2` (no branch created) | **Date**: 2026-11-14 | **Spec**: [prd-ui-2.md](../../prd-ui-2.md)

**Input**: The UI product requirements in [prd-ui-2.md](../../prd-ui-2.md), with the product context in [prd.md](../../prd.md), the existing behavior in [001 spec](../001-support-chat-assistant/spec.md), and the governing rules in [constitution.md](../../.specify/memory/constitution.md) (v1.2.2).

## Summary

Restyle and restructure the existing React chat screen to the visual direction in prd-ui-2.md:
a pastel gradient background, frosted glass cards, pill controls, a quick-start row, a
distinct escalation block, and line-icon feedback. The backend, the streaming protocol, the
session model, and the escalation logic do not change. The work is almost entirely in
`frontend/`, plus a small frontend-only change to how the composer receives a quick-start
draft.

## Technical Context

**Language/Version**: TypeScript 7 with React 19, per CLAUDE.md. No backend language change.

**Primary Dependencies**: Vite 8 (build and dev server), oxlint (lint), Prettier 3 (format).
Font packages for Manrope and IBM Plex Mono are added only after prd-ui-2.md open question 4
(self-hosting) is confirmed. No UI component library is added, because the design depends on
custom glass, pill, and gradient surfaces that a kit would have to be overridden to match.

**Storage**: None. The transcript stays in the in-memory session state from 001. No new
storage, and no cross-session history (UI-9).

**Testing**: Per 001 convention, no unit test tasks are included. Verification is the lint,
format, and build commands from CLAUDE.md, a contrast check over the real gradient colors, a
keyboard-only pass, a reduced-motion check, and a 360px and 1440px width check. Add test tasks
later if requested.

**Target Platform**: Modern mobile and desktop browsers. The layout is mobile first, from
360px width upward.

**Project Type**: Frontend change within the existing web application (`frontend/`).

**Performance Goals**: No additional network requests for the design. Fonts load from the
app's own origin (no third-party request). Backdrop blur is kept on a few surfaces, not every
element, to keep scroll smooth on phones.

**Constraints**:
- Visual values come from one token file only (UI-1, Principle VI).
- The API key never reaches the frontend (Principle II). No frontend file reads `.env`.
- Assistant text is rendered as text, never as HTML, so provider output cannot inject markup.
- Provider error text is never shown (UI-8, Principle VI).
- The escalation sentence is matched exactly (see Decisions).

**Scale/Scope**: One screen and its components. About 8 existing components and 1 stylesheet
are changed or replaced; 4 components are added.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|-----------|-------|--------|
| I. Product Scope Follows the PRD | Every change maps to a prd-ui-2.md requirement (UI-1 to UI-15). Out-of-scope items (stop generating, copy, attachments, recent chats, onboarding, dark mode) are not built. | PASS |
| II. Secrets Stay on the Backend | No frontend file reads or stores a key. No `.env` access added. | PASS |
| III. Grounded and Honest Answers | No change to answer generation. The escalation block only changes how the existing escalation sentence is displayed. | PASS |
| IV. Reliable Streaming, No Silent Failures | Streaming keeps the existing SSE reader. The error card (provider unavailable) and the waiting state are kept and restyled. | PASS |
| V. Minimal Personal Data in Logs | No logging added. No new data stored in the DOM beyond the chat content already shown (UI-14). | PASS |
| VI. Accessible and Consistent UI Design | Tokens in one file. Keyboard and focus, WCAG AA contrast over the gradient, live region, reduced motion, and the frontend-design skill (or the pinned reference, per the 1.2.2 precedence rule) are all tasks. | PASS |

**Post-design re-check (after Phase 1)**: All gates PASS. No unjustified complexity.

## Decisions

1. **Escalation detection in the frontend.** The UI shows the escalation block when an
   assistant message ends with the exact sentence in the backend's `SystemPrompt.ESCALATION_OFFER`.
   The frontend holds a copy of that sentence in `frontend/src/state/constants.ts`. A test task
   in the polish phase, or a comment with the backend file path, keeps the two in sync. The
   alternative, a new backend flag on the message, would be a backend change, which prd-ui-2.md
   excludes.
2. **Quick-start drafts.** `ChatInput` currently owns its text. It gains a controlled `draft`
   prop and an `onDraftChange` callback, so a quick-start chip can fill the composer without
   sending. The customer can edit before sending (prd-ui-2.md section 5).
3. **Code formatting in answers.** Answers stay plain text. A small splitter turns fenced code
   blocks into a `CodeBlock` element and everything else into text. Nothing is parsed as HTML,
   and no markdown library is added.
4. **Icons.** Inline SVG line icons in one `icons.tsx` file. No icon font or icon package.
5. **Brand mark.** A neutral placeholder component (`BrandMark`) until the design owner
   supplies the real mark (prd-ui-2.md open question 1). No mark is invented.
6. **Glass fallback.** Glass surfaces use `backdrop-filter` with a `@supports` fallback to solid
   white at 90% opacity (UI-15).
7. **Stylesheet.** `frontend/src/index.css` is split into `styles/tokens.css` (values only) and
   `styles/base.css` (element defaults, focus, reduced motion), plus one CSS file per component.
   The current file has a duplicated `.chat-page__header` rule; the rewrite removes it.

## Project Structure

### Documentation (this feature)

```text
specs/002-chat-ui-v2/
├── plan.md              # This file
└── tasks.md             # Created by this change, from plan.md and prd-ui-2.md
```

### Source Code (changed files only)

```text
frontend/src/
├── styles/
│   ├── tokens.css           # NEW: colors, type, radius, spacing, shadow (values only)
│   └── base.css             # NEW: gradient background, glass fallback, focus, reduced motion
├── index.css                # REPLACED: imports tokens.css and base.css, then component styles
├── components/
│   ├── BrandMark.tsx        # NEW: neutral placeholder mark (open question 1)
│   ├── icons.tsx            # NEW: thumb up, thumb down, send, back (inline SVG)
│   ├── QuickStarts.tsx      # NEW: Setup, API usage, Errors chips
│   ├── EscalationBlock.tsx  # NEW: escalation sentence, "Connect me", rose border
│   ├── CodeBlock.tsx        # NEW: inset block for fenced code, scrolls horizontally
│   ├── AnswerText.tsx       # NEW: splits answer text into text and CodeBlock parts
│   ├── MessageBubble.tsx    # CHANGED: customer line vs. answer card, error card, escalation block
│   ├── MessageList.tsx      # CHANGED: empty state with QuickStarts, live region kept
│   ├── ChatInput.tsx        # CHANGED: controlled draft, pill field, round send icon button
│   ├── FeedbackButtons.tsx  # CHANGED: line icons, aria-pressed, visible labels
│   └── EscalationButton.tsx # CHANGED: rose pill in header
├── pages/
│   └── ChatPage.tsx         # CHANGED: header layout, quick-start draft wiring, gradient shell
└── state/
    └── constants.ts         # NEW: ESCALATION_SENTENCE (mirrors backend SystemPrompt.ESCALATION_OFFER)
```

**Structure Decision**: Frontend-only. The existing component boundaries are kept, and new
pieces are added only where the design needs a distinct element (the escalation block, code
blocks, quick starts, icons). Backend paths are not touched.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | | |
