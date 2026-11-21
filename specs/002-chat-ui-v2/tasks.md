---

description: "Task list for the Caramel AI Support Chat UI, Version 2"
---

# Tasks: Caramel AI Support Chat UI, Version 2

**Input**: Design documents from `/specs/002-chat-ui-v2/`

**Prerequisites**: [plan.md](plan.md), [prd-ui-2.md](../../prd-ui-2.md), [constitution.md](../../.specify/memory/constitution.md) (v1.2.2)

**Tests**: No test tasks are included. Verification is the commands in CLAUDE.md, the checks in the final phase, and the 001 quickstart smoke test. Add test tasks if requested.

**Organization**: Tasks are grouped by user story. The stories map to prd-ui-2.md sections:
- **US1** Read streamed answers (FR-2, UI-5, UI-9)
- **US2** Start from quick starts and the empty state (FR-1, section 5 and 7)
- **US3** Escalation block and provider errors (FR-4, FR-5, UI-6, UI-7, UI-8)
- **US4** Feedback icons (FR-7, UI-13)
- **US5** Visual system, header, composer, and accessibility (UI-1 to UI-3, UI-10 to UI-12, UI-15)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no unfinished dependency)
- **[Story]**: The user story the task serves
- Paths are relative to the repository root. The backend is not changed in this feature.

## Path Conventions

- Frontend only: `frontend/src/`. The existing React and Vite project from 001 is reused.

---

## Phase 1: Setup

**Purpose**: Prepare the frontend for the new styles and components

- [ ] T001 Confirm prd-ui-2.md open question 4 (self-hosting Manrope and IBM Plex Mono). If confirmed, add `@fontsource-variable/manrope` and `@fontsource/ibm-plex-mono` to `frontend/package.json`. If not confirmed, stop this task and keep the system sans-serif fallback in `tokens.css` until the decision is made.
- [X] T002 [P] Create `frontend/src/state/constants.ts` exporting `ESCALATION_SENTENCE` with the exact text "Would you like me to connect you with a human support agent?", and a comment giving the backend source `backend/src/main/java/com/caramelai/support/chat/assistant/SystemPrompt.java` (Decision 1)
- [X] T003 [P] Create `frontend/src/components/icons.tsx` with inline SVG line icons: thumb up, thumb down, send, back. Each takes a size prop, defaults to 20px, and is `aria-hidden` (the label lives on the button)

---

## Phase 2: Foundational

**Purpose**: The token file and base styles every story depends on

**CRITICAL**: No story work can begin until this phase is complete

- [X] T004 Create `frontend/src/styles/tokens.css` with the values from prd-ui-2.md section 6 only: the three gradient stops, glass fill at 55% white, ink, muted, accent, rose, the type scale (12, 14, 16, 20, 28), the four radii (24, 16, 12, full pill), 4px spacing unit, and the one shadow. No selectors other than `:root`.
- [X] T005 Create `frontend/src/styles/base.css`: full-viewport gradient background from the tokens, body margin 0, a `@supports (backdrop-filter)` rule for the glass surface and a solid white fallback at 90% (UI-15), a `:focus-visible` outline of 2px accent offset 2px on every control, and a `@media (prefers-reduced-motion: reduce)` block that turns off all transitions and animations (UI-12). Depends on T004.
- [X] T006 Replace `frontend/src/index.css` so it only imports `styles/tokens.css` and `styles/base.css` and then the component stylesheets. Remove the duplicated `.chat-page__header` rule and the old brown and cream values. Depends on T004, T005.
- [X] T007 Restyle the global `button` rule in `frontend/src/index.css` (or a component file it imports) as a full-pill control with the accent fill and white text, and a disabled state at 50% opacity, using tokens only. Depends on T006.

**Checkpoint**: Tokens and base styles are in place. Story work can begin.

---

## Phase 3: User Story 1: Read streamed answers (P1)

**Goal**: Assistant answers appear as glass cards, stream in place, and show code blocks clearly.

**Independent Test**: Send a question with a fenced code block in a test reply. The answer streams into one card, and the code appears in an inset block that scrolls horizontally.

- [X] T008 [P] [US1] Create `frontend/src/components/CodeBlock.tsx`: an inset block with a 12px radius, the code face (IBM Plex Mono), horizontal scroll inside the block, and no line wrapping of identifiers (UI-11)
- [X] T009 [P] [US1] Create `frontend/src/components/AnswerText.tsx` that splits answer text on fenced ``` blocks into plain text parts and `CodeBlock` parts. Output is always React text nodes, never HTML (plan Decision 3)
- [X] T010 [US1] Restyle `frontend/src/components/MessageBubble.tsx` for assistant answers: a glass card with 16px radius and 16px padding, a 32px `BrandMark` to its left aligned to the top, the body rendered through `AnswerText`, and a 60-character measure. Depends on T008, T009, T014.
- [X] T011 [US1] Keep the "Thinking" waiting state inside the answer card, muted, with the three-dot pulse replaced by a static label under reduced motion (prd-ui-2.md section 7). Depends on T010.
- [X] T012 [US1] Keep the streaming behavior: text appends in the same card, the list keeps the newest text in view, and the layout does not jump per token (UI-5). Confirm in `frontend/src/components/MessageList.tsx` that `aria-live` is set only on the complete answer, so screen readers are not flooded per token (UI-4). Depends on T010.

---

## Phase 4: User Story 2: Start from quick starts and the empty state (P2)

**Goal**: A new customer sees three starter chips and can fill the composer from one without sending.

**Independent Test**: Load the empty chat. Tap "API usage". The composer contains the starter question, the text is editable, and nothing is sent until Send.

- [X] T013 [P] [US2] Create `frontend/src/components/QuickStarts.tsx` with three glass pill chips labeled "Setup", "API usage", and "Errors". Each chip calls `onPick` with its starter question. The row scrolls sideways when it does not fit (prd-ui-2.md section 5).
- [X] T014 [US2] Create `frontend/src/components/BrandMark.tsx`: a neutral circular placeholder with `aria-hidden` and no invented logo (plan Decision 5, prd-ui-2.md open question 1). Needed by T010.
- [X] T015 [US2] Change `frontend/src/components/ChatInput.tsx` to accept a controlled `draft` prop and `onDraftChange` callback, keeping the existing validation messages. Depends on nothing in this phase.
- [X] T016 [US2] Wire quick starts in `frontend/src/pages/ChatPage.tsx`: hold the draft in page state, pass it to `ChatInput`, and set it from `QuickStarts`. Show `QuickStarts` and the empty-state line "Ask about setup, API usage, or an error message." only when the transcript is empty. Depends on T013, T015.
- [X] T017 [US2] Update the empty state text in `frontend/src/components/MessageList.tsx` to match prd-ui-2.md section 7, replacing the current greeting. Depends on T016.

---

## Phase 5: User Story 3: Escalation block and provider errors (P3)

**Goal**: An uncertain answer shows the escalation offer as a distinct block with "Connect me". Provider failures show a plain error card with "Try again".

**Independent Test**: Make the assistant end with the escalation sentence. The sentence appears in a rose-bordered block with "Connect me", and the action creates a ticket reference. Stop the provider. The error card shows no provider text and a working "Try again".

- [X] T018 [P] [US3] Create `frontend/src/components/EscalationBlock.tsx`: a glass card with a 2px rose border, the offer sentence in ink, and a "Connect me" pill button in rose. "Connect me" calls the same handler as the header action (UI-7, prd-ui-2.md section 7).
- [X] T019 [US3] Update `frontend/src/components/MessageBubble.tsx`: if the answer text ends with `ESCALATION_SENTENCE`, render the text before it as an answer card and the sentence as `EscalationBlock`. Depends on T002, T010, T018.
- [X] T020 [P] [US3] Restyle the provider error in `frontend/src/components/MessageBubble.tsx` as a rose-bordered card with the text "The assistant could not answer. Your message was not lost." and a "Try again" button. Do not render any provider error text (UI-8). Depends on T010.
- [X] T021 [US3] Restyle the escalation result in `frontend/src/components/MessageBubble.tsx`: the ticket reference appears as an answer card with the reference in the code face. The failure reads "Could not create a ticket. Try again, or email the support team." with no invented address (prd-ui-2.md section 7, open question 6). Depends on T019.
- [X] T022 [US3] Keep the header "Talk to a person" action always visible during the session (UI-6). Verify in `frontend/src/pages/ChatPage.tsx` that it renders whenever `ready` is true, and carry the restyle through T025.

---

## Phase 6: User Story 4: Feedback icons (P4)

**Goal**: Thumbs up and down are line icons with accessible names and pressed state.

**Independent Test**: Rate an answer up, then down. The down icon fills, `aria-pressed` changes, and "Feedback recorded" appears in muted text.

- [X] T023 [US4] Restyle `frontend/src/components/FeedbackButtons.tsx`: replace the emoji with the thumb icons from T003, a visible text label on hover and focus, and `aria-pressed` on the active button. Keep the rating logic and error handling (FR-007). Depends on T003.
- [X] T024 [US4] Confirm "Feedback recorded" uses the muted token and that the chosen icon fills with the accent color (prd-ui-2.md section 7). Depends on T023.

---

## Phase 7: User Story 5: Visual system, header, composer, and accessibility (P5)

**Goal**: The header, composer, and layout match prd-ui-2.md sections 5 and 6 at every width from 360px upward.

**Independent Test**: Open the page at 360px and at 1440px. Both are usable with no horizontal page scroll, and every control has a visible focus ring.

- [X] T025 [US5] Restyle the header in `frontend/src/pages/ChatPage.tsx`: a round glass back control on the left, "Caramel AI Support" centered at 20px bold, the subtitle in muted text, and the "Talk to a person" pill on the right. Below 600px the pill moves to a row under the title (section 5). Depends on T003.
- [X] T026 [US5] Restyle `frontend/src/components/EscalationButton.tsx` as a rose-outlined pill, using tokens only (UI-1).
- [X] T027 [US5] Restyle `frontend/src/components/ChatInput.tsx` as a glass container with a 24px radius, a pill text field, the character counter above the field, and a round accent send button with the send icon and a visible "Send" accessible name (section 5). Depends on T015, T003.
- [X] T028 [US5] Cap the chat column at 440px centered, and check the layout from 360px to 1440px (UI-10). Set `max-width` on the chat page container in `frontend/src/pages/ChatPage.tsx` or its stylesheet. Depends on T025, T027.
- [X] T029 [US5] Make the composer and transcript usable with the keyboard: Enter sends, Shift+Enter adds a line, Tab reaches every control in order, and the focus outline is never removed (UI-2). Confirm in `frontend/src/components/ChatInput.tsx`. Depends on T027.

---

## Phase 8: Polish and Verification

**Purpose**: Checks required by the constitution and CLAUDE.md before merge

- [X] T030 Run `npm run lint`, `npm run format:check`, and `npm run build` in `frontend/`. Fix any failures and record the results in the PR (CLAUDE.md commands).
- [X] T031 Contrast check: for every text pairing in prd-ui-2.md section 6, measure the contrast against the real gradient and glass colors behind it at the top, middle, and bottom of the screen. Record the results. All must reach 4.5:1 (UI-3, Principle VI).
- [ ] T032 Keyboard pass: send a question, read an answer, rate it, choose "Talk to a person", and reach the ticket reference without a mouse (prd-ui-2.md section 10).
- [ ] T033 Screen reader pass: each completed answer is announced once, and errors are announced immediately (UI-4).
- [ ] T034 Reduced-motion pass: with "reduce motion" enabled, the pulse, the escalation fade, and any transition are off (UI-12).
- [X] T035 Check that `ESCALATION_SENTENCE` in `frontend/src/state/constants.ts` exactly matches `ESCALATION_OFFER` in `backend/src/main/java/com/caramelai/support/chat/assistant/SystemPrompt.java`. Plan Decision 1.
- [X] T036 Check that no frontend file contains the API key, reads `.env`, or renders provider error text (Principle II, UI-8). Search `frontend/src` for `GOOGLE_API_KEY` and `.env`.
- [ ] T037 Run the 001 quickstart smoke test (`specs/001-support-chat-assistant/quickstart.md`) against the redesigned screen.

---

## Dependencies and Execution Order

- Phase 1 and Phase 2 block all stories. T004 blocks T005 and T006. T006 blocks T007.
- Stories can run in priority order. Within Phase 3, T010 depends on T008, T009, and T014 (from Phase 4), so T014 is done early.
- Phase 8 runs after all story phases.

## Parallel Example

```text
# After Phase 2, launched together:
Task: "T008 Create CodeBlock.tsx"
Task: "T009 Create AnswerText.tsx"
Task: "T013 Create QuickStarts.tsx"
Task: "T018 Create EscalationBlock.tsx"
Task: "T023 Restyle FeedbackButtons.tsx"
```

## Notes

- Blocked until confirmed: T001 (prd-ui-2.md open question 4), T014 real mark (open question 1),
  T021 email address (open question 6), T013 starter topics (open question 7).
- The design direction itself needs sign-off (prd-ui-2.md open question 8) before Phase 3 starts.
- Commit after each task or logical group.
