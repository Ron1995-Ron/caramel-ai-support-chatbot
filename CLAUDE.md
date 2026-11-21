# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

The chat MVP is implemented per `specs/001-support-chat-assistant/` (spec, plan, tasks). `backend/` holds the Spring Boot app and `frontend/` holds the React app. The real support content, ticketing system, and transcript retention policy are still open (see the notes at the end of `specs/001-support-chat-assistant/tasks.md`).

User authentication is in MVP scope per `002-prd-user-auth.md` but is not implemented yet. No auth code exists in `backend/` or `frontend/`, and there is no `specs/002-*` feature spec yet. Chat endpoints are currently open; they must require a session once auth lands.

### Pinned versions

- Backend: Java 21, Spring Boot 4.1.2, Spring AI 2.0.2 (`spring-ai-starter-model-google-genai`), Maven.
- Frontend: React 19, TypeScript 7, Vite 8, oxlint, Prettier 3.

### Commands

Backend (run from `backend/`):

- Run both servers and open Chrome: `./run.sh` from the repo root (backend on 8083, frontend on 5176; logs go to `.run-logs/`).
- Run the backend alone: `mvn spring-boot:run` (reads `GOOGLE_API_KEY` from the root `.env`; fails fast if it is missing). The port is 8083. Override with `SERVER_PORT=8084` if 8083 is taken.
- Test: `mvn test`
- Format check: `mvn spotless:check`. Fix formatting: `mvn spotless:apply`.

Frontend (run from `frontend/`):

- Dev server: `npm run dev` on port 5176 (proxies `/api` to `http://localhost:8083`, or to `VITE_API_TARGET` if set, see `vite.config.ts`)
- Build: `npm run build`
- Lint: `npm run lint`
- Format check: `npm run format:check`. Fix formatting: `npm run format`.

Read `001-prd.md` before making design decisions. It is the source of truth for scope, requirements, and open questions. For authentication decisions, read `002-prd-user-auth.md` (AUTH-1 to AUTH-13).

## Planned product

Caramel AI Tech Support Chat Assistant: a customer-facing chat assistant that answers technical support questions about LLM inferencing for Caramel AI. It is in scope for a web chat UI with streamed replies, multi-turn session memory, an escalation path to a human agent, and customer accounts (sign-up with email verification, sign-in, password reset) so each chat belongs to a verified user. Account-specific actions (plan changes, refunds), voice, multi-language support, and cross-session history are out of scope for the MVP.

## Planned architecture

The PRD specifies three layers, plus identity:

- **Frontend (React + TypeScript):** chat UI, message list, input box, streaming display, escalation action, thumbs up/down feedback, and auth screens (sign-up, sign-in, verify email, forgot/reset password) with a route guard around chat.
- **Backend (Java + Spring Boot):** streaming chat endpoints, session handling, escalation logic, and Spring Security for `/api/auth/*` and for protecting chat, feedback, and escalation endpoints. Escalations must carry the authenticated user's ID and email.
- **AI layer (Spring AI):** a chat client for `gemma-4-26b-a4b-it` served through Google AI Studio. The system prompt defines the Caramel AI support persona and its rules.
- **Identity (planned):** a persistent user store (database not chosen yet) and a transactional email provider. Auth tokens are short-lived access and rotating refresh tokens in `HttpOnly` cookies.

Knowledge grounding is not decided yet. Options are a static system prompt or retrieval over a document store (PRD open question). Until that is settled, avoid building one approach into shared interfaces.

## Constraints that shape the code

- The Google AI Studio API key is read from `.env` at the project root as `GOOGLE_API_KEY`. It is read by the backend only. It must never reach the frontend, be committed (`.env` must be in `.gitignore`), or be written to logs. The same applies to the auth token signing key and email provider credentials once added.
- Do not let the assistant invent product behavior, pricing, or API details. It must say when it is unsure and offer escalation (FR-4, FR-5, accuracy NFR).
- Stream responses to the UI as they are generated (FR-2). Return a clear error message when the LLM provider is unavailable. Do not fail silently.
- Keep personal data out of logs. Never log passwords, session or verification/reset tokens, or full email addresses. A retention policy for chat transcripts is still undefined.
- Passwords are stored only as adaptive hashes (BCrypt or Argon2). Auth cookies are `HttpOnly`, `Secure` outside local dev, and `SameSite`. Do not store tokens in `localStorage`.
- Auth does not unlock account-specific actions. Those remain out of MVP scope.

## Repository notes

- `.mcp.json` (gitignored) configures a GitHub MCP server. Its bearer token is read from the `GITHUB_MCP_TOKEN` environment variable, not stored in the file. Do not write the token into any other file, and keep it out of the repo.
