# PRD: Caramel AI Support Chat UI, Version 2

## 1. Objective

Replace the current chat UI with a visual design built for its job. Customers come to this
screen with a technical problem, often an error or a failed API call, and want a correct answer
fast. The design must make three things easy: reading an answer, knowing when the assistant is
not sure, and reaching a person.

The visual direction follows the reference screens supplied with this PRD (section 4). This PRD
changes the presentation and interaction design of the existing chat screen. It does not add
features, change the backend, or change the escalation or streaming behavior defined in
`001-prd.md` and `specs/001-support-chat-assistant/`.

## 2. Users and Context

- **Who:** developers and technical operators using Caramel AI's inferencing API. Many arrive
  mid-task, with a terminal or an error log open in another window. Most use a phone or a
  narrow browser window.
- **Their job here:** get the exact fix for a setup, API, or error problem, or find out quickly
  that the assistant cannot help.
- **What they tolerate:** a slow answer is acceptable. An answer that sounds certain and is
  wrong is not (Principle III).

Design implication: the screen should feel calm and modern, with a precise technical answer at
its center. The escalation path must never be hidden behind the decoration.

## 3. Current State (what changes)

The screen in `frontend/src` today has:
- A centered 760px column on a flat cream background, with a white page.
- Customer messages in a brown bubble and assistant messages in a flat grey bubble.
- Brown rounded buttons, with one radius on every control.
- An outlined "Talk to a person" button in the header.
- Thumbs up and down as emoji.
- Plain text answers, with no formatting for code, commands, or error codes.

All of these change. The reference direction replaces the flat palette with a gradient
background and glass surfaces, and the emoji thumbs with line icons.

## 4. Design Direction

**Subject:** a technical support assistant for developers, used mostly on a phone.

**Primary job:** answer precisely, and make uncertainty and escalation impossible to miss.

**Visual reference:** three supplied screens (onboarding, home, and chat). The chat screen is
the one this PRD covers. The reference establishes:
- A soft pastel gradient background that moves from pale blue at the top, through lavender, to
  pink at the bottom.
- Frosted glass surfaces: white at about 55% opacity, a 1px white border, and a background
  blur. Cards and the composer use this surface.
- Pill-shaped controls for chips, buttons, and the input field.
- Assistant messages in rounded glass cards with a small circular brand mark beside them.
- A single large dark button for the primary action.
- A bold sans-serif with generous, rounded geometry for all text.

This is a deliberate choice from the brief. The reference's gradient and glass are followed,
even though they overlap with the defaults listed in section 4 below.

**One memorable thing:** the escalation state. When the assistant does not know, it says so in
plain words, and the offer to connect a person appears as a distinct block with a rose tint, a
border, and a direct action. Everything else stays calm, so this moment carries the weight.

**Rejected defaults** (do not use these, even though the reference is glossy):
- A large headline number with supporting stats.
- Uppercase letter-spaced labels above headings.
- Middle-dot separators in meta text ("Support · Help · Chat").
- Arrows appended to button text ("Send →").
- Numbered markers unless the content is a real sequence.
- A fade-and-slide entrance on every element.
- Decorative labels above messages (the reference's "Your Prompt" label is not carried over).

## 5. Layout

Mobile first. The desktop layout is the same column, centered, with a maximum width of 440px.

- **Background:** the full-viewport gradient in section 6. It sits behind the whole screen.
- **Header:** a single row. A round glass back control on the left, the product name
  "Caramel AI Support" centered, and the subtitle "Technical help for LLM inferencing" below
  it. On the right, the "Talk to a person" action as a pill with a rose outline. On a narrow
  screen the pill moves to a row below the title.
- **Quick starts:** a horizontal row of glass pill chips under the header: "Setup", "API usage",
  and "Errors". Each chip inserts its topic as a starter question into the composer, so the
  customer can edit before sending. The row scrolls sideways if it does not fit.
- **Transcript:** a vertical list, with 16px between messages.
  - *Customer messages:* right-aligned, in the ink color, with no bubble. A plain sentence,
    no label.
  - *Assistant answers:* left-aligned, in a glass card with 16px corner radius and 16px padding.
    A 32px circular brand mark sits to the left, aligned to the top of the card. The brand mark
    is a placeholder until the team supplies the real Caramel AI mark (section 12).
  - *Code and identifiers:* inside the answer card, in a darker-tinted inset block with a 12px
    radius. Long lines scroll inside the block.
  - *Measure:* answer text is capped at about 60 characters per line, through the card's
    padding and max width.
- **Composer:** pinned to the bottom, in a glass container with 24px radius. It holds a pill
  text field, a character counter, and a round send button. The send button is the accent
  violet with a white arrow icon (no text, so the arrow is an icon, not decoration in a label).
  The counter and any problem message sit on a line above the field.

Sketch, mobile (about 390px wide):

```
  ( <  )         Caramel AI Support          
              Technical help for LLM inferencing
            [ Talk to a person ]  (rose outline)

  ( Setup )  ( API usage )  ( Errors )

                          How do I set my key?  

  (o)  +--------------------------------------+
       | To set the key, send it in the       |
       | Authorization header:                |
       |  +--------------------------------+  |
       |  | Authorization: Bearer <key>    |  |
       |  +--------------------------------+  |
       +--------------------------------------+

  (o)  +--------------------------------------+
       | I don't know how to rotate a key.    |
       | The support content doesn't cover it.|
       |                                      |
       || Would you like me to connect you    |
       || with a human support agent?         |
       ||                   [ Connect me ]    |
       +--------------------------------------+
       (|| = rose 2px border of the escalation block)

  +--------------------------------------+
  | Ask a question about Caramel AI      | (o)
  +--------------------------------------+
  0 / 4000                       [ send ]
```

## 6. Visual Tokens

Background gradient (top to bottom): pale blue `#A9CBF7`, lavender `#C7B3F5`, pink `#F5B8EE`.

Named palette:

| Token      | Hex       | Use                                                       |
|------------|-----------|-----------------------------------------------------------|
| sky        | `#A9CBF7` | Top of the background gradient                            |
| lilac      | `#C7B3F5` | Middle of the background gradient                         |
| blush      | `#F5B8EE` | Bottom of the background gradient                         |
| glass      | `#FFFFFF` at 55% | Cards, chips, composer, and header controls         |
| ink        | `#12131A` | Body text, customer messages, primary button              |
| muted      | `#5B5F6E` | Secondary text, counter, subtitle, "Feedback recorded"    |
| accent     | `#6A56E0` | Send button and focus ring                                |
| rose       | `#A8184F` | Escalation block border, escalation action, error text    |

Glass fallback: where `backdrop-filter` is not supported, the glass surface becomes solid white
at 90% opacity, so text remains readable.

Contrast (WCAG 2.1 AA, normal text needs 4.5:1), computed from the hex values:
- Ink on white: 18.5:1. Ink on lavender: 14.7:1.
- White text on accent violet: 5.2:1.
- Muted on white: 6.4:1. Muted on lavender: 5.1:1.
- Rose on white: 7.2:1.

Every text pairing is checked against the actual gradient colors behind it before merge,
because the gradient varies and glass lets it show through (Principle VI).

Type: one sans family, with one code face.
- **UI and answer sans:** Manrope, weights 400, 600, and 700. Used for the header, chips,
  buttons, composer, counter, and assistant answers.
- **Code face:** IBM Plex Mono, for code blocks and inline identifiers only.

Scale: 12, 14, 16, 20, 28 px. No other sizes. Body is 16px with line height 1.5. The product
name is 20px at weight 700.

Radius: 24px for the composer, 16px for answer cards, 12px for code blocks, and a full pill for
chips, buttons, and the header action. Four values, each tied to one role.

Spacing: 4px base unit. Gaps between messages are 16px. Padding inside cards is 16px.

Icons: line icons at 20px for the back control, send, thumb up, and thumb down. Each icon
control has a visible text label on hover and focus, and the label is its accessible name.

Shadow: one soft shadow for glass surfaces, `0 8px 24px rgba(18, 19, 26, 0.08)`. No other shadows.

## 7. Interaction and States

- **Empty state:** the three quick-start chips, and one short line: "Ask about setup, API
  usage, or an error message." No illustration.
- **Waiting for the first token:** the text "Thinking" in muted color inside an answer card,
  with a subtle three-dot pulse. Replaced by the first streamed token.
- **Streaming:** text appends in place inside the card. The card's height grows smoothly, and
  the transcript keeps the latest text in view.
- **Uncertain answer (FR-4, FR-5):** when the assistant ends with the escalation sentence, that
  sentence appears as the escalation block: a rose-tinted glass card with a 2px rose border, the
  sentence in ink, and a "Connect me" pill button in rose. "Connect me" performs the same action
  as "Talk to a person." This is the only reveal animation in the design: a 150ms fade and 8px
  rise, disabled under reduced motion.
- **Provider unavailable (Principle IV):** a rose-bordered card with the text "The assistant
  could not answer. Your message was not lost." and a "Try again" pill button. No provider
  error text is shown (Principle VI).
- **Escalation result:** the ticket reference appears as a normal assistant card, with the
  reference in the code face. A failure reads: "Could not create a ticket. Try again, or email
  the support team." The email address must come from the support content, not be invented.
- **Feedback:** two line icons (thumb up, thumb down) below each completed answer. The chosen
  icon fills with the accent color and reports its state with `aria-pressed`. After a rating,
  "Feedback recorded" appears in muted text. Choosing the other icon changes the rating (FR-007).
- **Focus:** a 2px accent outline offset by 2px on every control. Never removed.
- **Reduced motion:** all animation off when the user prefers reduced motion. The pulse becomes
  a static "Thinking" label.

## 8. Copy

Write in the interface's voice, not a person's. Sentence case. Each action keeps one name
through its flow.

| Element               | Copy                                                     |
|-----------------------|----------------------------------------------------------|
| Product name          | Caramel AI Support                                       |
| Subtitle              | Technical help for LLM inferencing                       |
| Header action         | Talk to a person                                         |
| Quick starts          | Setup · API usage · Errors (rendered as separate chips)  |
| Composer placeholder  | Ask a question about Caramel AI                          |
| Send (icon label)     | Send                                                     |
| Counter               | 0 / 4000                                                 |
| Too long              | Keep your message under 4000 characters.                 |
| Empty send            | Type a question before sending.                          |
| Connect offer button  | Connect me                                               |
| Retry                 | Try again                                                |

The "·" in the quick-start row is a list separator in this table only. On screen, the three
chips are separate elements with no separator characters.

Use the names the interface already uses. Do not rename "Talk to a person" to "Support",
"Agent", or "Help", so the customer finds the same action in every place it appears.

## 9. Requirements

| ID    | Requirement                                                                  | Source             |
|-------|------------------------------------------------------------------------------|--------------------|
| UI-1  | The redesign uses the tokens in section 6 and no other colors, sizes, or radii. | Principle VI    |
| UI-2  | Every control is reachable and operable by keyboard, with a visible focus.   | Principle VI       |
| UI-3  | Every text pairing meets WCAG 2.1 AA contrast against the actual background. | Principle VI       |
| UI-4  | New messages and errors are announced through a live region.                  | Principle VI       |
| UI-5  | Answers stream in place, with no layout jump on each token.                   | FR-2, Principle IV |
| UI-6  | The escalation control stays visible in the header during the whole session.  | FR-5               |
| UI-7  | An uncertain answer shows the escalation offer as a distinct block.           | FR-4, FR-5         |
| UI-8  | Provider errors show a plain message and a retry. No provider text shown.     | Principle IV, VI   |
| UI-9  | The transcript shows the current session only. No cross-session history.      | FR-6, PRD §4       |
| UI-10 | The layout works from 360px width upward, with no horizontal page scroll.     | Quality floor      |
| UI-11 | Code blocks scroll inside their own box. Long identifiers never break layout. | Quality floor      |
| UI-12 | Reduced-motion preference disables all non-essential motion.                  | Quality floor      |
| UI-13 | Feedback icons have accessible names and report their state with aria-pressed.| FR-7, Principle VI |
| UI-14 | No personal data is added to the DOM beyond what the chat already shows.      | Principle V        |
| UI-15 | Glass surfaces fall back to solid white where backdrop blur is unsupported.   | Quality floor      |

## 10. Acceptance Criteria

- A keyboard-only user can send a question, read an answer, rate it, choose "Talk to a
  person," and reach the ticket reference without a mouse.
- A screen reader announces each streamed answer once when complete, and announces errors
  immediately.
- At 360px and at 1440px width, the transcript and composer are fully usable with no overlap.
- With the provider stopped, the customer sees the error card and can retry, and no provider
  text is visible.
- An uncertain answer is distinguishable from a confident answer without relying on color
  alone (the escalation block has a label-like sentence, a border, and a button).
- Contrast checks pass for every text pairing in section 6 over the gradient and glass.

## 11. Out of Scope

- Any change to the backend, API, streaming protocol, session model, or escalation logic.
- Features shown in the reference that the MVP does not have. These are not built:
  - Stop generating mid-answer (a new control that needs backend cancellation).
  - Copy-to-clipboard on answers.
  - Attaching a file or image from the composer.
  - Recent chats or a chat list (cross-session history, PRD §4).
  - Service tiles, plan badges, and the PDF scan sheet.
  - The onboarding screens ("Embrace Smart Risks" and the progress bar).
- Voice, multi-language, account views, and a marketing or landing page.
- Dark mode. It is a candidate for a later version (see open questions).

## 12. Open Questions

1. **Brand mark.** The reference shows a green circular mark beside each answer. The repo has
   no Caramel AI logo. The design owner must supply it. Until then the card shows a neutral
   placeholder, not an invented mark.
2. **Stop, copy, and attachments.** The reference shows these controls. They are excluded here
   because they add features. Confirm whether any should be added in a later PRD.
3. **Customer message label.** The reference shows a "Your Prompt" label above the customer's
   question. This PRD drops it as an unnecessary label. Confirm, or ask for it back.
4. **Font licensing and loading.** Manrope and IBM Plex Mono are open-licensed. Confirm the team
   accepts self-hosting them, and that loading them does not send data to a third party.
5. **Dark mode.** Is it needed for the MVP? If yes, define a second gradient and palette in
   section 6, with its own contrast checks.
6. **Escalation email address.** Section 7 needs the real support address from the support
   content. None is defined yet (PRD §10, escalation mechanism).
7. **Quick-start topics.** Confirm "Setup", "API usage", and "Errors" with the support team so
   each chip matches real content.
8. **Design sign-off.** The gradient, glass, and "one memorable thing" are the reference's
   direction plus this PRD's proposal. The design owner must confirm them before implementation.

## 13. Dependencies and Process

- Constitution v1.2.1, Principles IV and VI, apply to every change.
- Implementation follows a spec, plan, and task list created from this PRD, using the
  `frontend-design` skill for visual decisions. The skill is available, and the team should
  confirm it is the reference before work starts.
- Before merge: run `npm run lint`, `npm run format:check`, and `npm run build` in `frontend/`,
  and complete a keyboard pass, a reduced-motion check, and a contrast check over the gradient.
