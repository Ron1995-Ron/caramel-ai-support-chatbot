# Research: Caramel AI Support Chat Assistant

Each entry gives the decision, its rationale, and the alternatives considered.

## R1. Streaming transport

**Decision**: Server-Sent Events over HTTP. The client sends `POST /api/sessions/{id}/messages`
with the message body, and the response is `text/event-stream` with `token`, `done`, and
`error` events.

**Rationale**: FR-002 needs progressive display. SSE is one-way, which matches the need, and
it works through standard HTTP infrastructure. The browser's native `EventSource` only supports
GET, so the client reads the stream with `fetch`. That keeps the message in a POST body and
avoids putting the message in a URL.

**Alternatives considered**: WebSockets (bidirectional, not needed; more connection state to
manage). Polling (adds latency and fails FR-002). Returning the full answer at once (fails
FR-002 and SC-001).

## R2. Knowledge grounding behind a boundary

**Decision**: For the MVP, the Caramel AI support content is supplied through a
`KnowledgeSource` interface. The first implementation loads a static support-content file into
the system prompt. A retrieval implementation can be added later without changing callers.

**Rationale**: The PRD leaves static prompt vs. retrieval undecided (§6, §10). The constitution
says shared interfaces MUST NOT assume either approach (Technology and Architecture
Constraints). A static prompt is the smallest thing that satisfies FR-004 and is easy to
replace.

**Alternatives considered**: Hard-coding a static prompt into the chat service (couples the
service to the approach; the constitution forbids). Building retrieval now (adds an index and
an embedding pipeline before the content format is known; out of proportion for the MVP).

**Open**: The maintainers still need to decide the long-term approach. Tracked as
`TODO(KNOWLEDGE_GROUNDING)`.

## R3. Session state and retention

**Decision**: Sessions, messages, and feedback are held in backend memory. Each session expires
after 30 minutes of inactivity, and expiry removes its messages. Multi-turn context uses a
window of the most recent exchanges, capped by a token budget.

**Rationale**: FR-003 needs context within a session. FR-011 says nothing carries across
sessions, and Principle V says no transcript is stored until a retention policy exists. In
memory with expiry meets both: it is needed for the session and not persisted. The 30-minute
idle timeout is a proposed default and is listed as an assumption.

**Consequences**: A single backend instance serves the MVP. Restarting the backend ends all
sessions. Multiple instances would need a shared store, which is a later decision that also
needs the retention policy.

**Alternatives considered**: A database for sessions (would persist transcripts before a
retention policy exists; blocked by Principle V). Client-only history (the server would have to
receive the whole transcript on every request, and the UI would own context the backend needs
for the model).

## R4. Keeping the Google AI Studio key on the backend

**Decision**: The backend reads `GOOGLE_API_KEY` from `.env` at the project root. Spring AI
calls Google AI Studio with it. The key is never in any response, the frontend build, or logs.
`.env` is listed in `.gitignore`.

**Rationale**: Principle II and PRD §7 require this. A server-side call keeps the key out of
the browser.

**Implementation note**: Spring Boot does not read `.env` files by default. The backend
needs a small loader or a dotenv-style library. The choice is made at scaffolding time, and the
loader MUST fail fast with a clear message when the key is missing.

**Alternatives considered**: Exporting the key in the shell only (works, but makes the
documented setup in PRD §9 easy to get wrong). Reading the key from a client-side config (fails
Principle II outright).

## R5. Model and provider access

**Decision**: Use Spring AI's chat client configured for `gemma-4-26b-a4b-it` through Google AI
Studio. Use the streaming chat method for responses. Configure the request timeout and map
provider errors to the `error` event.

**Rationale**: PRD §6 fixes the model, provider, and framework. Principle IV requires a clear
error on provider failure or timeout.

**Alternatives considered**: Calling the Google REST API directly (duplicates what Spring AI
already provides and gives up the framework choice in PRD §6).

**To verify at implementation**: Whether the chosen Spring AI version exposes the streaming and
timeout options as described. The exact property names are checked against the version
pinned at scaffolding.

## R6. Assistant behavior rules

**Decision**: A system prompt defines the Caramel AI support persona and these rules: answer
only from supplied support content; say when the content does not cover the question; offer
escalation in that case; do not claim to perform account actions (FR-009); answer in English
(spec Edge Cases).

**Rationale**: Principle III and FR-004, FR-005, FR-009, FR-010 need these rules to hold in
the model's behavior. The rules are part of the prompt and are checked by the acceptance tests
(SC-003, SC-004).

**Alternatives considered**: Filtering answers after generation with a second model call
(adds latency and cost against the first-token target; defer unless tests show the prompt
alone is insufficient).

## R7. Escalation as a ticket

**Decision**: Escalation creates a ticket through a `TicketingClient` interface. The ticket
holds the session reference, the escalation reason, and the customer's latest message. It does
not hold the full transcript until a retention policy is recorded (see plan.md, Constitution
Check).

**Rationale**: Spec Decisions chose a ticket (asynchronous). Principle V forbids transcript
storage without a retention policy, and a ticket is storage in a third-party system.

**Alternatives considered**: Including the full transcript (violates Principle V until the
retention policy exists). Live queue or email (rejected by the user's decision in the spec).

**Open**: The ticketing system itself is not named in the PRD. The `TicketingClient`
implementation is left until it is chosen.

## R8. Feedback storage

**Decision**: Store a rating (up or down) per assistant message, keyed by message ID, with the
time recorded. Store no free text. Allow the rating to be changed.

**Rationale**: FR-007 and the satisfaction metric in SC-006. Keeping the record to an ID and a
rating avoids storing customer text under Principle V.

**Alternatives considered**: Free-text feedback (adds personal-data risk the MVP does not need).

## Resolved unknowns

All NEEDS CLARIFICATION items from Technical Context are resolved. The spec's two
clarifications were decided by the user (ticket escalation, anonymous sessions). The
remaining open items are `KNOWLEDGE_GROUNDING`, `CHAT_RETENTION_POLICY`, the ticketing system
choice, and the exact versions to pin.
