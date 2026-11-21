# Quickstart: Validate the Support Chat Assistant

This guide proves the MVP works end to end. Implementation details are in tasks.md and the
code, not here. Endpoints are defined in [contracts/chat-api.yaml](contracts/chat-api.yaml).
Entities are defined in [data-model.md](data-model.md).

## Prerequisites

- A Google AI Studio API key with access to `gemma-4-26b-a4b-it`.
- A `.env` file at the project root containing `GOOGLE_API_KEY=<your-key>`. Confirm `.env`
  is listed in `.gitignore` before you run anything (Principle II).
- Java 21 or later, Node.js LTS, and the build tools pinned at scaffolding.

## 1. Start the backend

Run the backend from `backend/` with the project's run command (recorded in CLAUDE.md once the
project exists). It must fail fast with a clear message if `GOOGLE_API_KEY` is missing.

**Expected**: The server starts on its configured port. The log shows the startup line and no
key value.

## 2. Start the frontend

Run the frontend from `frontend/` with its dev-server command. Open the URL it prints.

**Expected**: A new, empty chat window with an input box.

## 3. Scenario: streamed answer (User Story 1, FR-001, FR-002, FR-004)

1. Type a supported question, for example "How do I create an API key?", and send it.
2. Watch the assistant reply.

**Expected**: The answer appears word by word before it is complete. The first words appear
within 5 seconds (SC-001). The answer only uses support content. If a sentence cannot be
supported, the assistant says so.

## 4. Scenario: follow-up in the same session (User Story 2, FR-003)

1. Ask "What are the rate limits?". Then ask "And for the second tier?".

**Expected**: The second answer refers to the first answer's context and does not ask the
customer to repeat it.

## 5. Scenario: unknown question and escalation (User Story 3, FR-005, FR-010)

1. Ask a question the support content does not cover, for example a made-up product feature.

**Expected**: The reply says the assistant does not know and offers escalation in the same
reply. It does not invent a feature, price, or API detail (SC-003).

2. Ask "I want to talk to a person."

**Expected**: The customer is told a support ticket was created and given its reference. The
ticket contains the session reference, the reason, and the latest message only, with no full
transcript (plan.md, Constitution Check).

## 6. Scenario: account action refused (FR-009, SC-004)

1. Ask "Please refund my last invoice."

**Expected**: The assistant says it cannot perform account actions and offers escalation. It
does not claim a refund was made.

## 7. Scenario: provider failure (FR-008, SC-002)

1. Stop the network or set `GOOGLE_API_KEY` to an invalid value, then restart the backend.
2. Send any question.

**Expected**: A clear error message appears in the chat, for example "The assistant could not
answer right now." The chat is not blank and does not stay in a loading state. Restore the
correct key afterward.

## 8. Scenario: feedback (User Story 5, FR-007)

1. Give a thumbs up to an answer. Then change it to thumbs down.

**Expected**: The rating changes to thumbs down and is not duplicated.

## 9. Scenario: history and session end (User Story 4, FR-006, FR-011)

1. Send three messages. Scroll the chat.

**Expected**: All messages appear in order.

2. Reload the page.

**Expected**: A new session starts and the earlier conversation is not shown (cross-session
history is out of scope).

## 10. Privacy check (Principle V, FR-012)

1. Send a message containing a fake name and email, for example "My name is Test User,
   test@example.com".
2. Search the backend log output for the name and the email.

**Expected**: Neither appears in the logs. The log entries contain only request ID, session ID,
latency, status, and error type.

## 11. Key check (Principle II)

1. Search the frontend build output and all API responses for the value of `GOOGLE_API_KEY`.

**Expected**: The key does not appear anywhere.
