---

description: "Task list for the Caramel AI Support Chat Assistant"
---

# Tasks: Caramel AI Support Chat Assistant

**Input**: Design documents from `/specs/001-support-chat-assistant/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/chat-api.yaml](contracts/chat-api.yaml), [quickstart.md](quickstart.md)

**Tests**: No test tasks are included. The spec does not request tests or TDD. Behavior is checked by the scenarios in [quickstart.md](quickstart.md) (final phase). Add test tasks later if you want them.

**Organization**: Tasks are grouped by user story so each story can be built and checked on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: The user story the task serves (US1 to US5)
- Backend base package: `com.caramelai.support.chat`, under `backend/src/main/java/com/caramelai/support/chat/` (called `<pkg>/` below)

## Path Conventions

- Web app per plan.md: `backend/` (Java, Spring Boot) and `frontend/` (React, TypeScript)
- Neither directory exists yet. Phase 1 creates both.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create both projects and protect the secret before any code touches it

- [X] T001 Add `.env` and `.env.local` to a new `.gitignore` at the repository root (Principle II). Also ignore `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, and `.venv/`.
- [X] T002 [P] Create `.env.example` at the repository root containing the single line `GOOGLE_API_KEY=<your-key>` and no real value
- [X] T003 Scaffold the Spring Boot backend in `backend/` (Java 21+, Maven or Gradle, current stable Spring Boot) with dependencies: Spring Web (with SSE support), Validation, Spring AI Google GenAI provider (Spring AI 2.0.x). Verify the exact artifact names and versions against current Spring AI documentation before pinning, and record the chosen versions in `CLAUDE.md`.
- [X] T004 [P] Scaffold the React frontend in `frontend/` with Vite, React 18+, TypeScript 5.x (strict mode on)
- [X] T005 [P] Configure backend formatting and static checks in `backend/` (for example Spotless or Checkstyle) and record the lint command in `CLAUDE.md`
- [X] T006 [P] Configure ESLint and Prettier in `frontend/` and record the lint and dev commands in `CLAUDE.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared pieces every user story needs

**CRITICAL**: No user story work can begin until this phase is complete

- [X] T007 Implement the startup config in `backend/src/main/java/com/caramelai/support/chat/config/EnvConfig.java`: read `GOOGLE_API_KEY` from the `.env` file at the project root (Spring Boot does not read `.env` by default, so add a small loader or a dotenv library) and FAIL FAST at startup with a clear message when the key is missing. The message MUST NOT print the key (Principle II).
- [X] T008 [P] Create the `Session` model in `<pkg>/session/Session.java` with fields `id` (UUID, server-generated), `createdAt`, `lastActivityAt`, `status` (enum `active`, `escalated`, `expired`), and an ordered `messages` list, per [data-model.md](data-model.md)
- [X] T009 [P] Create the `Message` model in `<pkg>/session/Message.java` with fields `id` (UUID), `sessionId`, `role` (enum `customer`, `assistant`, set by the server, never by the client), `content`, `status` (enum `complete`, `failed`), `createdAt`. Customer `content` MUST be "1 to 4,000 characters, not blank".
- [X] T010 Implement `SessionStore` in `<pkg>/session/SessionStore.java`: in-memory (concurrent map), create/get/append message, and a scheduled sweep that removes a session "30 minutes after" its `lastActivityAt` (idle timeout, research.md R3). Updating a session MUST refresh `lastActivityAt`. Depends on T008, T009.
- [X] T011 [P] Implement error handling in `<pkg>/api/ApiExceptionHandler.java`: map errors to RFC 9457 `Problem` responses with the fields `type`, `title`, `status`, `detail`, `code` from [contracts/chat-api.yaml](contracts/chat-api.yaml). Include a `SessionNotFound` mapping to 404.
- [X] T012 [P] Implement logging safety in `<pkg>/config/RequestLoggingFilter.java`: assign a request ID per request and log only request ID, session ID, latency, status, and error type. The filter and all other code MUST NOT log message content or the API key (Principles II and V, FR-012).
- [X] T013 [P] Define the `KnowledgeSource` interface in `<pkg>/knowledge/KnowledgeSource.java` (returns the support content to give the model) and implement `StaticPromptKnowledgeSource` in `<pkg>/knowledge/StaticPromptKnowledgeSource.java` that loads `backend/src/main/resources/support-content.md`. Callers MUST depend on the interface only, so retrieval can replace it later (research.md R2).
- [X] T014 [P] Create `backend/src/main/resources/support-content.md` with clearly marked PLACEHOLDER Caramel AI support content (a few sample FAQ entries) and a note at the top that the real docs, FAQs, and known errors must replace it before launch (PRD §9). Do not invent real pricing or API details in the placeholder.
- [X] T015 [P] Configure CORS or a dev proxy so the frontend dev server can reach the backend, in `backend/src/main/java/com/caramelai/support/chat/config/WebConfig.java` and `frontend/vite.config.ts`. The key MUST NOT appear in any frontend file.
- [X] T016 [P] Create shared frontend types in `frontend/src/state/types.ts` for `Message` (id, role, content, status, createdAt, optional rating) and the SSE event payloads from the contract (`token` with `delta`, `done` with `messageId`, `error` with `code` and `message`)

**Checkpoint**: Foundation ready. User story work can begin.

---

## Phase 3: User Story 1 - Get a streamed answer to a technical question (Priority: P1) MVP

**Goal**: A customer starts a session, sends a question, and sees the answer stream in. Provider failures show a clear error.

**Independent Test**: Run quickstart.md steps 1 to 3 and step 7. Start a session, send a supported question, and see a progressive answer. Then break the key and see a clear error in the chat.

### Implementation for User Story 1

- [X] T017 [US1] Implement the system prompt in `backend/src/main/resources/prompts/support-system-prompt.st` (or a constant in `<pkg>/assistant/SystemPrompt.java`): the Caramel AI support persona; answer only from the supplied support content; say when the content does not cover a question; answer in English (spec Edge Cases). The escalation and account-action rules are added in US3.
- [X] T018 [US1] Implement `AssistantService` in `<pkg>/assistant/AssistantService.java`: use Spring AI's chat client configured for `gemma-4-26b-a4b-it` on Google AI Studio with the streaming method, include the support content from `KnowledgeSource` and the system prompt, return a `Flux` of token strings. Set a request timeout. Config goes in `<pkg>/config/AiConfig.java` and `backend/src/main/resources/application.yml`. Depends on T007, T013, T017.
- [X] T019 [US1] Map provider failures in `<pkg>/assistant/AssistantService.java`: when the provider is unavailable, errors, or times out, emit a terminal error with code `PROVIDER_UNAVAILABLE` and the message "The assistant could not answer right now. Please try again, or ask for a human agent." and mark the assistant message `failed` (FR-008, Principle IV). No exception detail or key may reach the client.
- [X] T020 [US1] Implement `POST /api/sessions` in `<pkg>/api/SessionController.java`, returning 201 with `{ "sessionId": "<uuid>" }` per [contracts/chat-api.yaml](contracts/chat-api.yaml). Depends on T010.
- [X] T021 [US1] Implement `POST /api/sessions/{sessionId}/messages` in `<pkg>/api/MessageController.java`: validate the body (blank content returns 400, content over 4,000 characters returns 413 with code `MESSAGE_TOO_LONG`), store the customer message, stream `token` events, then exactly one `done` (with `messageId`) or `error` event as `text/event-stream`. Unknown or expired session returns 404. Depends on T011, T018, T019, T020.
- [X] T022 [P] [US1] Implement the SSE reader in `frontend/src/services/chatApi.ts`: `createSession()` and `sendMessage(sessionId, content, handlers)` using `fetch` with a POST body (not `EventSource`), parsing `token`, `done`, and `error` events. A stream that ends without `done` or `error` MUST be treated as an error.
- [X] T023 [P] [US1] Create `frontend/src/components/ChatInput.tsx`: text box and send button, blocks sending empty or whitespace-only text and shows a short reason, enforces a 4,000 character limit with a visible message, and is disabled while a reply is streaming
- [X] T024 [P] [US1] Create `frontend/src/components/MessageBubble.tsx` and `frontend/src/components/MessageList.tsx`: render customer and assistant messages, append streamed tokens to the in-progress assistant bubble, and show a visible error state for a failed message
- [X] T025 [US1] Create `frontend/src/state/useChat.ts` (session creation on load, send flow, token appending, error handling that always ends the loading state) and `frontend/src/pages/ChatPage.tsx` composing T023 and T024. A failed request MUST never leave the UI blank or stuck loading (FR-008). Depends on T022, T023, T024.
- [X] T026 [US1] Wire up `frontend/src/main.tsx` to render `ChatPage` and confirm the frontend build output contains no API key (search the build output for the variable name and any value)

**Checkpoint**: User Story 1 works on its own. This is the MVP.

---

## Phase 4: User Story 2 - Ask follow-up questions in the same conversation (Priority: P1)

**Goal**: The assistant takes earlier messages in the session into account, and a long session keeps working.

**Independent Test**: Run quickstart.md step 4. Ask a question, then a follow-up that depends on the answer, and confirm the answer uses the earlier context.

### Implementation for User Story 2

- [X] T027 [US2] Implement `ConversationWindow` in `<pkg>/session/ConversationWindow.java`: from a session's messages, select the most recent exchanges that fit a configurable token budget (configure in `application.yml`, with an approximate character-based estimate is acceptable), always keeping the newest customer message and dropping the oldest exchanges first (spec User Story 2, scenario 2). Depends on T010.
- [X] T028 [US2] Update `AssistantService` in `<pkg>/assistant/AssistantService.java` to send the windowed history as prior user and assistant messages to the model, in order. Failed assistant messages MUST NOT be included. Depends on T027, T018.
- [X] T029 [US2] Update `MessageController` in `<pkg>/api/MessageController.java` to pass the session's windowed history to `AssistantService` and to store the completed assistant message in the session after `done`. Depends on T028, T021.

**Checkpoint**: User Stories 1 and 2 both work.

---

## Phase 5: User Story 3 - Escalate to a human agent (Priority: P1)

**Goal**: A customer can reach a human by asking in chat or using an escalation button. The assistant admits when it does not know and refuses account actions.

**Independent Test**: Run quickstart.md steps 5 and 6. Ask something unknown and see escalation offered. Ask for a person and get a ticket reference. Ask for a refund and see it refused.

### Implementation for User Story 3

- [X] T030 [US3] Extend the system prompt in `backend/src/main/resources/prompts/support-system-prompt.st` (or `SystemPrompt.java`): when unsure or the content does not cover the question, say so and offer escalation in the same reply (FR-004, FR-005); never claim to perform account actions such as plan changes or refunds, say it cannot and offer escalation (FR-009); never invent product behavior, pricing, or API details (FR-010); when support content conflicts, state the uncertainty and offer escalation (spec Edge Cases)
- [X] T031 [P] [US3] Create the `EscalationRequest` model in `<pkg>/escalation/EscalationRequest.java` with `id`, `sessionId`, `reason` (enum `customer_request`, `assistant_unable`), `ticketRef`, `ticketPayload`, `status` (enum `created`, `failed`), `createdAt`. The `ticketPayload` MUST contain only the session reference, the reason, and the customer's latest message, and MUST NOT contain the full transcript, until `CHAT_RETENTION_POLICY` is recorded (plan.md, Constitution Check; data-model.md).
- [X] T032 [P] [US3] Define the `TicketingClient` interface in `<pkg>/escalation/TicketingClient.java` and a `LoggingTicketingClient` stand-in in `<pkg>/escalation/LoggingTicketingClient.java` that returns a generated ticket reference and logs only the reference and reason (never the message text). Mark the class and `README` note clearly that the real ticketing system is not chosen yet (research.md R7).
- [X] T033 [US3] Implement `EscalationService` in `<pkg>/escalation/EscalationService.java`: build the payload per T031, call `TicketingClient`, set the session status to `escalated`, keep the session usable afterward. When the ticketing client fails, record status `failed` and surface a clear error (FR-008). Depends on T010, T031, T032.
- [X] T034 [US3] Implement `POST /api/sessions/{sessionId}/escalations` in `<pkg>/api/EscalationController.java`: 201 with `{ "ticketRef", "status": "created" }`, 404 for an unknown session, 503 when ticketing is unavailable, per [contracts/chat-api.yaml](contracts/chat-api.yaml). Depends on T033.
- [X] T035 [US3] Implement `EscalationIntentDetector` in `<pkg>/escalation/EscalationIntentDetector.java`: a simple phrase-based check (for example "human", "agent", "real person", "talk to someone", "support team") on the customer's message. In `MessageController`, when it matches, create the escalation through `EscalationService` and stream a confirmation containing the ticket reference as `token` events followed by `done`, without calling the model. The contract stays unchanged. This is a planning default (flag for review). Depends on T033, T021.
- [X] T036 [P] [US3] Create `frontend/src/components/EscalationButton.tsx`: always visible in the chat, one click plus an optional confirm (no more than two actions, SC-005), calls `POST /api/sessions/{sessionId}/escalations`, and shows the returned ticket reference or a clear error. Add `requestEscalation` to `frontend/src/services/chatApi.ts`. Depends on T034.
- [X] T037 [US3] Add the `EscalationButton` to `frontend/src/pages/ChatPage.tsx` and render the confirmation as a normal assistant message in `frontend/src/state/useChat.ts`. Depends on T036, T025.

**Checkpoint**: User Stories 1 to 3 work. All Must requirements are covered.

---

## Phase 6: User Story 4 - Review the current session's history (Priority: P2)

**Goal**: Every message in the current session is visible, in order.

**Independent Test**: Run quickstart.md step 9. Send several messages, scroll to see all of them in order, then reload and confirm a new empty session starts.

### Implementation for User Story 4

- [X] T038 [US4] Implement `GET /api/sessions/{sessionId}/messages` in `<pkg>/api/MessageController.java`: return the session's messages in order with `id`, `role`, `content`, `status`, `createdAt`, and `rating` when present, per [contracts/chat-api.yaml](contracts/chat-api.yaml). 404 for an unknown or expired session. Depends on T029.
- [X] T039 [US4] Add `getMessages(sessionId)` to `frontend/src/services/chatApi.ts` and update `frontend/src/state/useChat.ts` so the message list always shows all messages from the current session in order, and reloads them from the server if the in-memory view is lost while the session is still valid. On page reload a NEW session starts and old messages are not shown (FR-011). Depends on T038, T025.

**Checkpoint**: Stories 1 to 4 work.

---

## Phase 7: User Story 5 - Rate responses (Priority: P2)

**Goal**: A customer can give or change a thumbs up or down on any assistant response.

**Independent Test**: Run quickstart.md step 8. Rate a response up, change it to down, and confirm there is one rating, not two.

### Implementation for User Story 5

- [X] T040 [P] [US5] Create the `Feedback` model in `<pkg>/feedback/Feedback.java` with `messageId`, `rating` (enum `up`, `down`, required), `recordedAt`. "One feedback record per assistant message." No free text is stored (research.md R8).
- [X] T041 [US5] Implement `FeedbackService` in `<pkg>/feedback/FeedbackService.java`: record or update the rating for a message; the message MUST be an assistant message in the same session, otherwise reject (422). Changing the rating updates the existing record and `recordedAt`. Depends on T010, T040.
- [X] T042 [US5] Implement `PUT /api/sessions/{sessionId}/messages/{messageId}/feedback` in `<pkg>/api/FeedbackController.java`: body `{ "rating": "up" | "down" }`, 204 on success, 404 for an unknown session, 422 for a non-assistant message, per [contracts/chat-api.yaml](contracts/chat-api.yaml). Depends on T041.
- [X] T043 [P] [US5] Create `frontend/src/components/FeedbackButtons.tsx` (thumbs up and down on each assistant message, highlights the current rating, confirms to the customer that it was recorded) and add `sendFeedback` to `frontend/src/services/chatApi.ts`. Depends on T042.
- [X] T044 [US5] Render `FeedbackButtons` in `frontend/src/components/MessageBubble.tsx` for completed assistant messages only, and keep the rating in `frontend/src/state/useChat.ts`. Depends on T043.

**Checkpoint**: All five user stories work.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Constitution requirements and end-to-end validation

- [X] T045 Instrument the PRD §8 success metrics in `<pkg>/metrics/ChatMetrics.java` (Micrometer or simple counters): share of sessions resolved without escalation, thumbs-up ratio, escalation rate by reason, and median time from customer message to first token. Use counts and timings only, with no message text (Development Workflow, SC-006). Depends on T029, T033, T041.
- [X] T046 [P] Review all log statements across `backend/src/main/java/` and confirm none can print message content, names, emails, or the API key (Principles II and V, FR-012). Fix any that can.
- [ ] T047 [P] Move the GitHub MCP bearer token out of `.mcp.json` into an environment variable before the repository is shared (Principle II, Development Workflow). Do not copy the token into any other file.
- [X] T048 [P] Update `CLAUDE.md` "Current state" and add the real backend and frontend build, run, and lint commands recorded in T003, T005, and T006
- [X] T049 Run every scenario in [quickstart.md](quickstart.md) (steps 1 to 11) against the running app, record pass or fail for each, and fix any failures. Include the first-token timing check against SC-001 and the key and log checks (steps 10 and 11).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies. Start immediately.
- **Foundational (Phase 2)**: Depends on Setup. BLOCKS all user stories.
- **User Stories (Phase 3 to 7)**: All depend on Foundational.
  - US1 is the base for the others (they extend `MessageController`, `AssistantService`, `useChat.ts`).
  - US2, US3, US4, US5 each depend on US1 files but not on each other, except where noted in task text (T037 needs T025, T039 needs T025 and T038 needs T029).
- **Polish (Phase 8)**: Depends on the stories you want shipped. T045 needs US2, US3, and US5.

### User Story Dependencies

- **US1 (P1)**: After Foundational. No dependency on other stories.
- **US2 (P1)**: After US1 (changes the same service and controller).
- **US3 (P1)**: After US1. Its controller and service files are separate from US2, but T035 edits `MessageController`, so do it after T029.
- **US4 (P2)**: After US2 (T038 builds on T029).
- **US5 (P2)**: After US1. Backend tasks are independent of US2 to US4.

### Within Each User Story

- Models before services, services before endpoints, backend endpoints before the frontend calls that use them
- Tasks that edit the same file (for example `MessageController.java`) are not marked [P]

### Parallel Opportunities

- Setup: T002, T004, T005, T006 can run together
- Foundational: T008, T009, T011, T012, T013, T014, T015, T016 can run together; T010 waits on T008 and T009
- US1: T022, T023, T024 can run together while the backend tasks proceed
- US3: T031, T032, T036 can run together
- US5: T040 and T043 can start once their inputs exist; backend and frontend work can overlap after T042

---

## Parallel Example: User Story 1

```text
# Frontend pieces, launched together once Phase 2 is done:
Task: "Implement the SSE reader in frontend/src/services/chatApi.ts"
Task: "Create frontend/src/components/ChatInput.tsx"
Task: "Create frontend/src/components/MessageBubble.tsx and MessageList.tsx"

# Meanwhile, backend (sequential):
T017 -> T018 -> T019 -> T020 -> T021
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (Setup) and Phase 2 (Foundational)
2. Complete Phase 3 (US1)
3. STOP and check: quickstart.md steps 1 to 3 and 7
4. Demo the streamed answer and the error handling

Note that the PRD marks FR-1 to FR-5 as Must. US1, US2, and US3 together cover them, so the MVP is not shippable until Phase 5 is done (constitution Principle I).

### Incremental Delivery

1. Setup + Foundational
2. US1, check, demo
3. US2, check
4. US3, check (Must requirements complete)
5. US4 and US5 (Should requirements), check each
6. Polish, then the full quickstart run

---

## Notes

- [P] tasks touch different files and have no unfinished dependency
- Stop at any checkpoint to check the story on its own
- Open items that block real launch, not development: the real support content (T014), the real ticketing system (T032), the transcript retention policy (T031), and the version pinning (T003)
- Commit after each task or logical group
