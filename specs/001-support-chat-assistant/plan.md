# Implementation Plan: Caramel AI Support Chat Assistant

**Branch**: `001-support-chat-assistant` (no branch created) | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from [spec.md](spec.md), with the product context in [prd.md](../../prd.md) and the governing rules in [constitution.md](../../.specify/memory/constitution.md).

## Summary

A web chat where an anonymous customer asks LLM inferencing support questions and receives
streamed answers from `gemma-4-26b-a4b-it` through Google AI Studio. The assistant keeps
context for the session, says when it is unsure, and creates a support ticket when a human is
needed. The backend holds the API key, owns session state, and streams tokens to the React UI
over Server-Sent Events.

## Technical Context

**Language/Version**: Java 21+ (backend); TypeScript 5.x with React 18+ (frontend). Exact
versions are pinned when the projects are scaffolded.

**Primary Dependencies**: Spring Boot (current 3.x or 4.x line), Spring AI 2.0.x with the Google
GenAI provider, React, a build tool (Vite) for the frontend. Versions are to be verified at
scaffolding time.

**Storage**: In-memory only for the MVP. Sessions, messages, and feedback live in backend
memory with an idle timeout. No database. This follows the anonymous, no-cross-session scope
(FR-011) and Principle V (no transcript storage without a retention policy). See research.md
R3.

**Testing**: JUnit 5 with Spring Boot test slices and Testcontainers-free unit tests for the
backend. Vitest with React Testing Library for the frontend. An end-to-end smoke test driven by
quickstart.md.

**Target Platform**: Linux server for the backend. Modern desktop and mobile browsers for the
UI.

**Project Type**: Web application (frontend + backend).

**Performance Goals**: First response part within 5 seconds for at least 95% of messages
(SC-001). Streamed tokens delivered as they arrive, with no client-side buffering of the whole
answer.

**Constraints**: Google AI Studio key read only by the backend from `GOOGLE_API_KEY` in `.env`
(Principle II). Input capped at 4,000 characters (spec Assumptions). Single backend instance
for the MVP, because sessions are in memory (see research.md R3). No personal data in logs
(Principle V).

**Scale/Scope**: Traffic volume unknown (PRD §10). Designed for a single instance serving tens
of concurrent sessions. Scaling beyond that needs a shared session store, which is a later
decision.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|-----------|-------|--------|
| I. Product Scope Follows the PRD | Every FR maps to PRD FR-1..FR-7 or PRD §7 NFRs. Out-of-scope items (account actions, voice, multi-language, cross-session history) are excluded. | PASS |
| II. Secrets Stay on the Backend | Key read from `.env` by the backend only. API routes never return it. `.env` must be in `.gitignore` (task to verify). | PASS (conditional on the `.gitignore` check) |
| III. Grounded and Honest Answers | The system prompt defines the persona and the "say when unsure, offer escalation" rule. Knowledge is behind a `KnowledgeSource` boundary (research.md R2). | PASS |
| IV. Reliable Streaming, No Silent Failures | SSE streaming with a timeout. Provider errors arrive as an explicit `error` event that the UI renders. | PASS |
| V. Minimal Personal Data in Logs | Logs carry request ID, session ID, latency, status, and error type only. | PASS |
| V. No transcript storage without a retention policy | Spec's escalation wording says the ticket includes the session transcript. That would store a transcript in a third-party system with no retention policy. | **CONFLICT: resolved in plan** |

**Resolution for the conflict**: Until `CHAT_RETENTION_POLICY` is recorded in the constitution
or `prd.md`, the escalation ticket contains the session reference, the escalation reason, and
the customer's latest message only. It does not contain the full transcript. In-memory session
history used for multi-turn context is held only for the session's life and is not persisted
(see research.md R3). This is a planning decision, not a spec change. The spec's escalation
wording (spec.md, Decisions) should be updated to match, or the retention policy should be set
so the transcript can be included. Both are a user decision.

**Post-design re-check (after Phase 1)**: All gates PASS with the resolution above. No
unjustified complexity, so Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-support-chat-assistant/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── chat-api.yaml    # Phase 1 output (HTTP + SSE contract)
├── checklists/
│   └── requirements.md
└── tasks.md             # Created later by /speckit-tasks, not by /speckit-plan
```

### Source Code (repository root)

```text
backend/
├── src/main/java/.../chat/
│   ├── api/             # REST + SSE controllers, request/response DTOs, error mapping
│   ├── session/         # Session store (in-memory), idle expiry, message history window
│   ├── assistant/       # ChatClient wrapper, system prompt, streaming, provider errors
│   ├── knowledge/       # KnowledgeSource interface + static-prompt implementation
│   ├── escalation/      # EscalationService + TicketingClient interface
│   ├── feedback/        # Feedback recording (rating per message, no text)
│   └── config/          # Spring AI / Google AI Studio config, .env loading, timeouts
└── src/test/java/.../   # Unit + slice tests

frontend/
├── src/
│   ├── components/      # MessageList, MessageBubble, ChatInput, FeedbackButtons, EscalationButton
│   ├── services/        # chatApi (fetch-based SSE reader), session handling
│   ├── pages/           # ChatPage
│   └── state/           # Session + message state (no API key, no secrets)
└── tests/
```

**Structure Decision**: Web application (Option 2). The PRD names separate React and Spring Boot
layers, and Principle II requires the key to stay on the backend. The two directories are
separate projects. Neither exists yet, so they are created in the implementation phase.

## Complexity Tracking

No violations that need justification. The in-memory store and the single-instance limit are
deliberate MVP simplifications and are recorded in research.md (R3), not as complexity.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | | |
