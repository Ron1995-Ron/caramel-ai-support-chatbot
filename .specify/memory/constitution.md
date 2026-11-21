<!--
SYNC IMPACT REPORT (temporary; remove before committing)

Version change: 1.3.0 → 1.3.1 (PATCH: the PRD file reference changed from prd.md to 001-prd.md; wording only, no principle changed; the file was renamed to 001-prd.md)
Version change: 1.2.2 → 1.3.0 (MINOR: new Principle VII added; Principles I and II and the
Technology, Workflow, and Governance sections materially expanded for user authentication)

Source: 002-prd-user-auth.md (AUTH-1 to AUTH-13, API outline, NFRs), 001-prd.md §10 (open question on
customer authentication), and the user's request to update the constitution from the auth PRD.

Modified principles (old title → new title):
- I. Product Scope Follows the PRD → unchanged title. Now also accepts requirements from a numbered
  PRD (002-prd-user-auth.md) once 001-prd.md references it.
- II. Secrets Stay on the Backend → unchanged title. Scope widened from the Google API key to all
  backend secrets: token signing key and email provider credentials.
- V. Minimal Personal Data in Logs → unchanged title. Adds passwords and auth/verification/reset
  tokens to the list of values that MUST NOT be logged.

Added content:
- Principle VII. Authenticated Access and Account Data (new). Covers required sign-in for chat,
  feedback, and escalation endpoints; per-user session ownership; password hashing; cookie-based
  session tokens; single-use hashed verification and reset tokens; minimal account data; and no
  account-level actions unlocked by sign-in.
- Technology and Architecture Constraints: identity bullet (Spring Security, persistent user store,
  transactional email provider).
- Development Workflow: changes to authentication, sessions, or account data MUST be checked against
  Principles II, V, and VII.
- Governance: compliance review now covers Principles I through VII.

Added sections: none
Removed sections: none

Deferred items / TODOs:
- TODO(KNOWLEDGE_GROUNDING): Static system prompt vs. retrieval over a document store is an open PRD
  question (§6, §10). Principle III and the architecture section are written to hold for either approach.
- TODO(CHAT_RETENTION_POLICY): Retention period for chat transcripts is undefined (PRD §7, §10).
  Principle V requires that a policy be set before transcripts are stored. 002-prd-user-auth.md §10
  also asks for retention and deletion rules for user-linked transcripts.
- TODO(ESCALATION_MECHANISM): Ticketing, email, or live-agent queue is undefined (PRD §10).
- TODO(USER_STORE): Database and migration tool for user records are undefined (002 PRD §10). Principle
  VII does not depend on the choice.
- TODO(EMAIL_PROVIDER): Transactional email provider and sender domain are undefined (002 PRD §10).
- TODO(TRAFFIC_VOLUME): Expected traffic volume is undefined (PRD §10).
- TODO(LATENCY_TARGET): The first-token target ("within a few seconds") is marked "to be validated" in
  PRD §7. Principle IV uses "a few seconds" until a number is set.
- Resolved: TODO(CUSTOMER_AUTH) from 1.2.2. Customers MUST authenticate before chatting, per
  002-prd-user-auth.md. 001-prd.md §10 has not been updated yet (see Next Actions in the summary).
- Ratification date 2026-11-14 is the first adoption date. No earlier date was recorded.
-->

# Caramel AI Tech Support Chat Assistant Constitution

## Core Principles

### I. Product Scope Follows the PRD

`001-prd.md` is the source of truth for scope, requirements, and open questions. Features MUST
map to a functional requirement (FR-1 to FR-7), to a non-functional requirement in PRD
§7, or to a requirement in a numbered PRD that `001-prd.md` references (for example
`002-prd-user-auth.md`, AUTH-1 to AUTH-13). Requirements marked Must in the PRD MUST be
implemented before the MVP ships. Requirements marked Should (FR-6, FR-7, AUTH-11 to AUTH-13)
MAY be deferred only by a recorded decision in `001-prd.md`. Out-of-scope items (account-specific
actions such as plan changes or refunds, voice, multi-language support, and cross-session
history) MUST NOT be built in the MVP. A change to scope MUST update `001-prd.md` before code is
written, and a numbered PRD takes effect for scope only after `001-prd.md` references it.
Rationale: the MVP boundary is what keeps the assistant from taking account actions it is not
authorized to take.

### II. Secrets Stay on the Backend

The Google AI Studio API key MUST be read from `.env` at the project root as `GOOGLE_API_KEY`,
and only by the backend. The same rule applies to every other backend secret, including the
session token signing key and email provider credentials, which MUST be read from `.env` or the
environment by the backend only. No secret MUST be sent to the frontend, included in a frontend
bundle, returned by any endpoint, or written to logs. `.env` MUST be listed in `.gitignore`
and MUST NOT be committed. Any credential that appears in a tracked file MUST be moved to an
environment variable before the repository is shared. Rationale: the key grants paid inference
access, a leaked signing key lets anyone forge sessions, and none of these can be rotated safely
once leaked.

### III. Grounded and Honest Answers

The assistant MUST NOT invent product behavior, pricing, or API details. When the assistant
is unsure, or the knowledge it was given does not cover the question, it MUST say so and
MUST offer escalation to a human agent (FR-4, FR-5). Answers MUST be based on Caramel AI
support content supplied to the model. The system prompt MUST define the Caramel AI support
persona and these rules. Rationale: a confident wrong answer about billing or API behavior
costs more than an honest hand-off.

### IV. Reliable Streaming with No Silent Failures

Assistant responses MUST be streamed to the UI as they are generated (FR-2). The first token
MUST reach the UI within a few seconds of a user message (PRD §7). The target is to be
validated, and it MUST be measured during testing. When the LLM provider is unavailable,
slow beyond the timeout, or returns an error, the user MUST see a clear error message in the
chat. A failed request MUST NOT leave the UI blank or stuck in a loading state. Rationale: the
PRD requires graceful errors, and silent failures look like the assistant has ignored the
customer.

### V. Minimal Personal Data in Logs

Logs MUST NOT contain personal data from chat messages or accounts, such as names, emails,
account identifiers, payment details, passwords, or session, verification, and reset tokens.
Logging MUST be limited to what is needed for operations (for example request ID, latency,
status, and error type). No chat transcript MAY be stored until a retention policy is defined
and recorded in this constitution or in `001-prd.md`. Rationale: the PRD leaves retention and
compliance open, so the safe default is to collect less.

### VI. Accessible and Consistent UI Design

Visual direction for the chat UI MUST follow the `frontend-design` skill, except where the
product PRD or a supplied reference screen pins a visual choice. A pinned choice governs, and
the skill guides every other visual decision. Every UI change MUST keep the chat usable without
a mouse and readable by assistive technology. Interactive controls (send, escalate, thumbs
up/down, session actions) MUST be reachable and operable by keyboard, MUST have an accessible
name, and MUST show a visible focus state. Text and interactive elements MUST meet WCAG 2.1 AA
contrast in both light and dark themes, if both are offered. New messages, streaming updates,
and error messages MUST be announced to assistive technology through a live region. Colors,
spacing, typography, and radii MUST come from a single set of design tokens, and components MUST
NOT hard-code values that bypass it. A visual change MUST NOT alter chat behavior, the escalation
path, or the data shown to the customer, and MUST NOT display the API key, personal data, or
provider error details. Rationale: a support tool is used under stress, often by customers who
rely on keyboard or screen readers, and a visual redesign must not break Principles IV, V, or VII.

### VII. Authenticated Access and Account Data

Every chat, feedback, and escalation endpoint MUST require a valid authenticated session and
MUST reject unauthenticated requests with HTTP 401 (AUTH-5). Every session, message, and
feedback record MUST be bound to the authenticated user, and the backend MUST enforce that a
user can read and write only their own sessions, returning 403 or 404 otherwise (AUTH-6).
Authorization MUST be checked on the backend, never only in the UI.

Passwords MUST be stored only as salted adaptive hashes (BCrypt or Argon2). Plain-text
passwords MUST NOT be stored, logged, or returned by any endpoint. Session tokens MUST be
delivered only in `HttpOnly`, `Secure` (outside local development), `SameSite` cookies and MUST
NOT be stored in browser `localStorage`. State-changing cookie-authenticated requests MUST be
protected against CSRF.

Email verification and password reset tokens MUST be random, single-use, stored hashed, and
expire (24 hours for verification, 30 minutes for reset). Authentication and account error
messages MUST NOT reveal whether an email address is registered. Account data MUST be limited to
email address, password hash, verification status, and timestamps. Sign-in MUST NOT unlock any
account-specific action (Principle I). Rationale: each chat costs paid inference, escalations
depend on knowing who the customer is, and account data is personal data that the MVP must not
collect or expose beyond what authentication needs.

## Technology and Architecture Constraints

- Frontend: React with TypeScript. The UI covers the message list, input box, streaming
  display, escalation action, chat history for the current session (FR-6), thumbs up/down
  feedback (FR-7), and the sign-up, sign-in, verify-email, password-reset, and route-guard
  screens defined in AUTH-1 to AUTH-13. Visual and interaction rules are set by Principle VI.
- Backend: Java with Spring Boot. It provides the streaming chat endpoints, session handling
  for multi-turn memory (FR-3), escalation logic, and authentication through Spring Security
  (Principle VII).
- Identity: a persistent user store for accounts and one-time tokens (database choice pending),
  and a transactional email provider configured through environment variables. The chat session
  store stays in memory until a persistence decision is recorded.
- AI layer: Spring AI, with a chat client for `gemma-4-26b-a4b-it` served through Google AI
  Studio.
- Knowledge grounding is not yet decided (static system prompt or retrieval over a document
  store). Until it is decided, shared interfaces MUST NOT assume either approach. Grounding
  logic MUST sit behind a boundary that can be replaced later.
- Each layer MUST NOT reach into another layer's internals. The frontend talks to the
  backend only, and never to the LLM provider or the email provider directly.

## Development Workflow

- `CLAUDE.md` and `001-prd.md` MUST be read before design decisions are made.
- A change that touches secrets, streaming, the fallback or escalation path, or logging MUST
  be checked against Principles II, IV, and V before merge.
- A change that touches authentication, sessions, cookies, or account data MUST be checked
  against Principles II, V, and VII before merge.
- A change to the UI MUST be checked against Principles IV, VI, and VII before merge, including
  a keyboard-only pass and a contrast check of any new colors.
- The success metrics in PRD §8 (resolution without escalation, thumbs-up ratio, escalation
  rate and reasons, median time to first response) and the sign-up and sign-in metrics in
  002-prd-user-auth.md §8 MUST be instrumented before launch. They MUST use only the data
  allowed by Principle V.
- Build, lint, and test commands MUST be recorded in `CLAUDE.md` once the project has them.
  Until then, changes MUST be verified by reading and, where possible, by running the app.
- Before the repository is shared, the GitHub MCP bearer token in `.mcp.json` MUST be moved
  to an environment variable (see Principle II).

## Governance

This constitution supersedes other practices in this repository where they conflict.

Amendments: any change to this file MUST be proposed as its own change, state the reason
and the principles affected, and be reviewed by the project maintainers before it is merged.
The Sync Impact Report MUST accompany each amendment and is removed before commit.

Versioning policy (semantic versioning):
- MAJOR: a principle is removed or redefined in a way that breaks existing behavior, or a
  requirement is dropped.
- MINOR: a principle or section is added, or existing guidance is materially expanded.
- PATCH: wording, typo, or clarification changes that do not alter meaning.

Compliance review: every PR and review MUST check the changes against Principles I through VII.
A change that breaks a principle MUST either be fixed or be accompanied by an amendment.
Any added complexity MUST be justified in the PR description.

**Version**: 1.3.1 | **Ratified**: 2026-11-14 | **Last Amended**: 2026-11-21
