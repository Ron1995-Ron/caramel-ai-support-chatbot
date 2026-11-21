# Feature Specification: Caramel AI Support Chat Assistant

**Feature Branch**: `001-support-chat-assistant` (no branch created)

**Created**: 2026-11-14

**Status**: Draft

**Input**: User description: "Feature description not provided; derived from 001-prd.md (MVP scope)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Get a streamed answer to a technical question (Priority: P1)

A Caramel AI customer opens the chat, asks a question about LLM inferencing (setup, API usage,
errors, or billing-adjacent technical questions), and sees the answer appear as it is written
rather than all at once.

**Why this priority**: This is the core value of the product. Without it, nothing else matters.

**Independent Test**: Start a new session, send a question covered by Caramel AI support
content, and confirm that an answer appears progressively and is relevant to the question.

**Acceptance Scenarios**:

1. **Given** a customer has started a new chat session, **When** they send a supported
   technical question, **Then** the assistant's answer begins to appear in the chat before the
   full answer is complete.
2. **Given** a customer sends a question that is covered by Caramel AI support content,
   **When** the answer is complete, **Then** the answer addresses the question and does not
   contain product behavior, pricing, or API details that are not in the support content.
3. **Given** a customer sends an empty message, **When** they submit it, **Then** the message
   is not sent and the customer is told why.

---

### User Story 2 - Ask follow-up questions in the same conversation (Priority: P1)

A customer continues the conversation with follow-up questions that depend on earlier
messages, such as "what about the second option?" or "does that work with streaming?".

**Why this priority**: Multi-turn memory is a Must requirement (FR-3). Without it, customers
must repeat context and abandon the conversation.

**Independent Test**: In one session, ask a question and then a follow-up that refers to the
first answer. Confirm the follow-up is answered in the context of the first exchange.

**Acceptance Scenarios**:

1. **Given** a customer has asked a question and received an answer, **When** they ask a
   follow-up that refers to that answer, **Then** the assistant's reply takes the earlier
   exchange into account.
2. **Given** a session has been open for a long time with many messages, **When** the customer
   keeps asking questions, **Then** the assistant continues to respond and does not drop the
   most recent exchanges.

---

### User Story 3 - Escalate to a human agent (Priority: P1)

A customer asks to speak to a human, or the assistant cannot resolve the issue. The customer
is offered and can start an escalation to a human support agent.

**Why this priority**: Escalation is a Must requirement (FR-5). It is the safety net for every
case the assistant cannot answer, and the PRD goal of reducing human load depends on it.

**Independent Test**: Ask for a human agent in a session and confirm that an escalation is
started with the conversation context available to the agent. Separately, ask a question the
assistant cannot answer and confirm that escalation is offered.

**Acceptance Scenarios**:

1. **Given** a customer is in a session, **When** they ask for a human agent, **Then** the
   assistant starts the escalation and confirms it to the customer.
2. **Given** the assistant does not know the answer to a question, **When** it responds,
   **Then** it says it is unsure and offers escalation in the same reply.
3. **Given** a customer asks for an account-specific action (for example a plan change or a
   refund), **When** the assistant responds, **Then** it states it cannot perform the action
   and offers escalation. It does not claim to have performed the action.

---

### User Story 4 - Review the current session's history (Priority: P2)

A customer scrolls back through the messages in the current session.

**Why this priority**: Visible history is a Should requirement (FR-6). It helps the customer
follow long conversations but is not needed to answer a first question.

**Independent Test**: Send several messages, then confirm all of them, in order, are visible in
the chat window for the same session.

**Acceptance Scenarios**:

1. **Given** a session has several exchanges, **When** the customer views the chat, **Then**
   every message from the current session is shown in order.

---

### User Story 5 - Rate responses (Priority: P2)

A customer gives a thumbs up or thumbs down to an individual assistant response.

**Why this priority**: Feedback is a Should requirement (FR-7) and feeds the customer
satisfaction success metric in the PRD.

**Independent Test**: Give a thumbs up or down to one response and confirm the rating is
recorded against that response.

**Acceptance Scenarios**:

1. **Given** an assistant response is shown, **When** the customer selects thumbs up or thumbs
   down, **Then** the rating is recorded for that response and the customer sees that it was
   recorded.
2. **Given** a customer has already rated a response, **When** they select a different rating,
   **Then** the rating for that response is updated, not duplicated.

---

### Edge Cases

- What happens when the AI service is unavailable or returns an error? The customer sees a
  clear error message in the chat. The chat is not left blank or in a loading state.
- What happens when the response is slower than the timeout? The customer sees a clear message
  that the answer could not be produced in time, and can retry or escalate.
- What happens when the customer's message is very long? The message is accepted up to a
  stated limit (see Assumptions); beyond that, the customer is told the message is too long.
- What happens when the customer asks in a language other than English? The assistant responds
  in English and says that other languages are not supported in this version. Multi-language
  support is out of scope.
- What happens when the customer asks about information in the support content that conflicts
  with another part of it? The assistant states the uncertainty and offers escalation rather
  than picking one answer silently.
- What happens when the customer closes the chat and returns later? A new session starts. Past
  conversations are not shown, because cross-session history is out of scope.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Customers MUST be able to start a new chat session and send text messages in it.
- **FR-002**: Assistant responses MUST be shown progressively as they are generated, and the
  first part MUST appear within the first-response target in SC-001.
- **FR-003**: The assistant MUST take earlier messages in the same session into account for
  the duration of that session.
- **FR-004**: Answers MUST be based on Caramel AI support content. When the assistant does not
  know, or the content does not cover the question, it MUST say so.
- **FR-005**: Customers MUST be able to request a human agent at any time in a session. The
  assistant MUST offer escalation whenever it cannot resolve an issue.
- **FR-006**: The current session's messages MUST be visible in the chat, in order (Should).
- **FR-007**: Customers MUST be able to give a thumbs up or thumbs down to each assistant
  response, and each rating MUST be recorded against that response (Should).
- **FR-008**: When the AI service is unavailable, slow beyond its timeout, or returns an error,
  the customer MUST see a clear error message in the chat. A failed request MUST NOT produce a
  blank or stuck display.
- **FR-009**: The assistant MUST NOT perform account-specific actions such as plan changes or
  refunds. When asked, it MUST say it cannot do so and offer escalation.
- **FR-010**: The assistant MUST NOT invent product behavior, pricing, or API details.
- **FR-011**: Conversations MUST NOT be available across sessions. Cross-session history is out
  of scope.
- **FR-012**: Operational logs MUST NOT contain personal data from customer messages.

### Key Entities

- **Session**: One continuous chat conversation. Holds its messages and is ended when the
  customer leaves or starts a new chat. Attributes: start time, status, escalation state.
- **Message**: One customer message or one assistant response within a session. Attributes:
  author (customer or assistant), content, order within the session, timestamp.
- **Feedback**: A thumbs up or down attached to one assistant message. Attributes: rating,
  message reference, time recorded.
- **Escalation Request**: A request for a human agent raised within a session. Attributes:
  session reference, reason (customer request or assistant unable to resolve), time raised,
  status.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For at least 95% of customer messages, the first part of the response appears in
  the chat within 5 seconds of sending. (Proposed target; the PRD says "within a few seconds,
  to be validated".)
- **SC-002**: In test runs covering the Must scenarios, 100% of provider failures show a clear
  error message in the chat, with no blank or stuck display.
- **SC-003**: In test runs of questions the assistant cannot answer from support content, 100%
  of replies say the assistant is unsure and offer escalation.
- **SC-004**: In test runs of account-specific requests, 0 responses claim that an account
  action was performed.
- **SC-005**: Customers can start an escalation from any point in a session in no more than
  two actions.
- **SC-006**: The share of conversations resolved without escalation, the thumbs-up ratio, the
  escalation rate with reasons, and the median time to first response are all recorded for
  every session, without storing personal data in logs. Targets for these are set after launch.

## Assumptions

- The MVP is English-only and web-based.
- Caramel AI support content (docs, FAQs, known issues) is available in usable form before
  launch.
- A support ticketing system exists and is reachable from the assistant (PRD §9). Escalations
  are created as tickets (see Decisions).
- Customer messages have a maximum length of 4,000 characters. This is a reasonable default
  and is not in the PRD.
- Sessions are not persisted across customer visits. This follows the PRD's out-of-scope list
  and the Principle V default.
- Feedback ratings are recorded per response, and each one can be changed by the customer.
- Traffic volume is not yet known. Performance targets are set for the MVP scale and are
  revisited when volume is known (PRD §10).
- The chat UI follows standard web chat conventions and does not need a custom design system
  for this spec.

## Decisions

- **Escalation mechanism (decided: ticket)**: When a customer requests a human agent, or the
  assistant cannot resolve an issue, the assistant creates a support ticket containing the
  session transcript. The customer is told the ticket reference and that a human will follow
  up asynchronously. No real-time handoff is required for the MVP (FR-005, SC-005).
- **Customer authentication (decided: anonymous)**: Customers chat without logging in. Each
  session is anonymous, so escalation tickets are not linked to a customer account. Personal
  data in messages is handled under FR-012 and Principle V.
