# Feature Specification: Caramel AI Support Chat UI, Version 2

**Feature Branch**: `002-chat-ui-v2` (no branch created)

**Created**: 2026-11-14

**Status**: Draft

**Input**: User description: "Improve the existing chat UI using the front-end design guidance, as specified in prd-ui-2.md"

**Source**: [prd-ui-2.md](../../prd-ui-2.md) is the detailed product requirements for this feature. This spec states the user-facing outcomes. Visual values and design tokens are in the PRD and are not repeated here.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Read a streamed answer comfortably (Priority: P1)

A developer asks a technical question on a phone or a narrow browser window. The answer appears as it is written, in a readable card. Code, commands, and error codes stand apart from the prose, so the developer can copy the exact fix without reading the whole answer again.

**Why this priority**: Reading the answer is the core job of the screen. Every other story depends on it. Without it, the redesign has no value.

**Independent Test**: Ask a question whose answer includes a code sample. The answer streams into one card, the code appears in its own block, and the block scrolls sideways when the line is long. Nothing on the screen jumps as each word arrives.

**Acceptance Scenarios**:

1. **Given** a customer has sent a question, **When** the assistant starts answering, **Then** the customer sees a waiting indicator in the answer's position, and the first words replace it as they arrive.
2. **Given** an answer contains a code sample, **When** the answer is complete, **Then** the code is shown in its own visually distinct block that does not wrap identifiers and scrolls sideways for long lines.
3. **Given** the assistant is still writing an answer, **When** new words arrive, **Then** the text grows in place and the newest text stays in view.
4. **Given** a screen reader user is on the page, **When** an answer finishes, **Then** the complete answer is announced once, not once per word.

---

### User Story 2 - Start from a suggested topic (Priority: P2)

A customer who does not know what to ask sees three starting topics: setup, API usage, and errors. Choosing one places a starter question in the message box. The customer can edit it, then send it or write their own question.

**Why this priority**: It lowers the effort to start, which matters most for a first-time visitor. It is useful but not required for a customer who already knows what to ask.

**Independent Test**: Open an empty chat. Choose "API usage". The message box contains the starter question, nothing has been sent, and the customer can change the text before sending.

**Acceptance Scenarios**:

1. **Given** the chat is empty, **When** the customer views the screen, **Then** three topic options and a short prompt are shown.
2. **Given** the customer chooses a topic, **When** the choice is made, **Then** the message box is filled with the starter question and no message is sent.
3. **Given** the customer has edited the starter question, **When** they send it, **Then** the edited text is the message that is sent.

---

### User Story 3 - See when the assistant is unsure, and reach a person (Priority: P3)

When the assistant cannot answer confidently, it says so in plain words, and the offer to connect a person stands apart from the rest of the answer. The customer can accept the offer with one action. If the assistant or its service is unavailable, the customer sees a clear message and can try again. No technical error details are shown.

**Why this priority**: This is the escalation path the PRD requires as Must. It is the screen's most important safety behavior, so it is distinct, but it applies only when the assistant is unsure or fails.

**Independent Test**: Trigger an uncertain answer. The offer appears as a distinct block with a "Connect me" action, and choosing it produces a ticket reference. Make the service unavailable. An error message with "Try again" appears, and it contains no technical detail.

**Acceptance Scenarios**:

1. **Given** the assistant ends an answer with the escalation offer, **When** the answer is shown, **Then** the offer appears as a visually distinct block with its own action, separate from the answer text.
2. **Given** the escalation offer is visible, **When** the customer chooses "Connect me", **Then** the same escalation action as the header control runs, and the result appears in the chat.
3. **Given** the assistant service is unavailable, **When** the customer sends a message, **Then** an error message says the answer could not be given, that the message was not lost, and offers "Try again", and no provider error text is shown.
4. **Given** an uncertain answer is shown, **When** it is compared with a confident answer, **Then** the difference is clear without relying on color alone.
5. **Given** a ticket cannot be created, **When** the customer tries to escalate, **Then** the customer is told that the ticket was not created, and is offered a retry and the support email, without an invented address.

---

### User Story 4 - Rate an answer with one tap (Priority: P4)

After an answer is complete, the customer can rate it as helpful or not helpful. The rating is shown as selected, and the customer can change it.

**Why this priority**: Ratings measure answer quality (PRD §8), but they do not change what the customer can get done.

**Independent Test**: Rate an answer as not helpful, then as helpful. The selection moves, the chosen rating is shown as selected, and a confirmation appears.

**Acceptance Scenarios**:

1. **Given** an answer is complete, **When** the customer views it, **Then** the helpful and not-helpful controls are shown, each with a name a screen reader announces.
2. **Given** the customer chooses a rating, **When** it is recorded, **Then** that control shows as selected and "Feedback recorded" appears.
3. **Given** a rating is already chosen, **When** the customer chooses the other rating, **Then** the rating changes to the new choice.

---

### User Story 5 - Use the screen on any common device and with a keyboard (Priority: P5)

The whole screen, including the header, the message box, and the answers, is usable at narrow phone widths and at desktop widths. Every control can be reached and used with a keyboard, shows where the focus is, and respects the customer's motion setting. Text is readable against the background.

**Why this priority**: These are quality requirements that apply to all stories. They are ranked last only because the other stories must exist before they can be checked.

**Independent Test**: Open the screen at 360px and at 1440px width. Complete the main task using only the keyboard. Turn on reduced motion and confirm that no animation plays.

**Acceptance Scenarios**:

1. **Given** the screen is 360px wide, **When** the customer uses it, **Then** the transcript and message box are fully usable with no sideways page scrolling.
2. **Given** the screen is 1440px wide, **When** the customer uses it, **Then** the chat stays in a centered column of readable width.
3. **Given** a keyboard-only customer, **When** they move through the screen, **Then** every control receives a visible focus indicator in a sensible order.
4. **Given** the customer has asked for reduced motion, **When** the screen updates, **Then** no decorative animation plays.
5. **Given** the screen is shown in a browser that cannot blur the background, **When** the customer reads the cards, **Then** the text is still readable.

### Edge Cases

- A customer sends a message that is blank or only spaces: the send is refused with a plain message, and nothing is sent.
- A customer's message is longer than the allowed limit: the send is refused with a message that states the limit.
- The customer sends a second message while an answer is still streaming: the second message is not sent until the first answer completes.
- A quick-start topic is chosen while the customer has unsent text in the message box: the unsent text is replaced by the starter question, and the customer is not sent anything.
- An answer contains a code block that is not closed: the rest of the text is still shown, and nothing is hidden.
- The customer is rating an answer when the rating fails: the customer sees a plain message that the rating was not saved, and can try again.
- The screen is viewed in a browser with no background blur or with reduced motion turned on: the layout and meaning are unchanged.
- An answer contains text that looks like markup: it is shown as plain text, not interpreted.
- A customer refers to an earlier conversation: no earlier conversation is shown, consistent with the session scope in the MVP.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The chat screen MUST present assistant answers in visually distinct cards that stream text in place without moving other content on screen. (PRD UI-5)
- **FR-002**: Code, commands, and error codes in answers MUST be shown in a separate block that scrolls sideways for long lines and never breaks identifiers across lines. (PRD UI-11)
- **FR-003**: The chat screen MUST show three starter topics on an empty chat: setup, API usage, and errors. Choosing one MUST fill the message box with a starter question and MUST NOT send it. (PRD section 5, 7)
- **FR-004**: When an assistant answer ends with the escalation offer, the offer MUST be shown as a distinct block with a "Connect me" action that runs the same escalation as the header control. (PRD UI-7, FR-4, FR-5)
- **FR-005**: The escalation control MUST remain visible in the header for the whole session. (PRD UI-6, FR-5)
- **FR-006**: When the assistant service is unavailable, the chat MUST show a clear error card with a "Try again" action. The card MUST NOT show any provider error text. (PRD UI-8, Principle IV)
- **FR-007**: A failed ticket creation MUST be reported in the chat, with a retry action and a support contact, without inventing a contact address. (PRD section 7)
- **FR-008**: Customers MUST be able to rate each completed answer as helpful or not helpful, change the rating, and see the chosen rating and a confirmation. (PRD FR-7, PRD UI-13)
- **FR-009**: The transcript MUST show only the current session. No earlier conversations are shown. (PRD UI-9, FR-6 scope)
- **FR-010**: New assistant messages and errors MUST be announced to screen readers. A completed answer MUST be announced once. (PRD UI-4)
- **FR-011**: The screen MUST be usable from 360px wide upward, with no sideways page scrolling. (PRD UI-10)
- **FR-012**: Every control MUST be usable with a keyboard and MUST show a visible focus indicator. (PRD UI-2)
- **FR-013**: Decorative motion MUST be turned off when the customer asks for reduced motion. (PRD UI-12)
- **FR-014**: Text MUST meet a contrast standard against the background it is shown on, including the background behind translucent cards. (PRD UI-3, Principle VI)
- **FR-015**: Translucent surfaces MUST remain readable in browsers that cannot blur the background. (PRD UI-15)
- **FR-016**: The visual design MUST use one defined set of colors, sizes, and corner radii. (PRD UI-1, Principle VI)
- **FR-017**: No personal data beyond what the chat already shows may be added to the screen. (PRD UI-14, Principle V)
- **FR-018**: This feature MUST NOT change the assistant's answers, the escalation logic, the session handling, or the streaming behavior. (PRD section 1)
- **FR-019**: Account actions, attachments, stopping an answer mid-way, copying answers, a recent-chats list, and onboarding screens MUST NOT be added in this feature. (PRD section 11)

### Key Entities *(include if feature involves data)*

- **Message**: One customer question or one assistant answer, with its text, its state (waiting, streaming, complete, or failed), and an optional customer rating. This feature does not change what a message contains.
- **Starter topic**: One of the three fixed topics (setup, API usage, errors) and the starter question it fills into the message box.
- **Escalation offer**: The assistant's standard offer to connect a person, shown as its own block when it ends an answer.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In usability testing, at least 9 of 10 developers find the answer and any code in it without help, within 30 seconds of reading the answer.
- **SC-002**: At least 9 of 10 customers who want a person can find and use the escalation action within 15 seconds of starting the chat.
- **SC-003**: A customer can go from an empty chat to a rated answer in four or fewer actions (choose a topic or type a question, send, read, rate).
- **SC-004**: Keyboard-only testers complete the main task (ask, read, rate, escalate) with no mouse use.
- **SC-005**: The screen is fully usable at 360px and 1440px widths, with no sideways page scrolling and no overlapping content.
- **SC-006**: Every line of text meets the contrast standard against its real background in the three checked areas of the screen.
- **SC-007**: Screen reader testers hear each completed answer announced once and hear each error announced immediately.
- **SC-008**: When the assistant is unavailable, 100% of failed sends show an error with a retry action, and no technical error text is visible in testing.

## Assumptions

- The backend, the streaming protocol, the session model, and the escalation logic from feature 001 stay as they are.
- The three starter topics (setup, API usage, errors) are placeholders until the support team confirms them (PRD open question 7).
- The escalation sentence the assistant uses is the existing standard sentence. Its exact wording is unchanged.
- The brand mark is a neutral placeholder until the design owner supplies the Caramel AI mark (PRD open question 1).
- The support email address shown on escalation failure comes from the approved support content. Until that content names one, the chat shows only the retry action (PRD open question 6).
- Self-hosted fonts are used if the team accepts self-hosting (PRD open question 4). Without that decision, a system font is used.
- Dark mode is out of scope for this feature (PRD open question 5).
- The visual direction, including the gradient background and glass surfaces, is the approved reference direction. The design owner confirms the direction before implementation starts (PRD open question 8).
- Customers are anonymous and the MVP does not require sign-in (PRD open question on authentication, prd.md section 10).
- Traffic volume is not known. Success criteria are measured per user test, not per load (PRD section 10).
