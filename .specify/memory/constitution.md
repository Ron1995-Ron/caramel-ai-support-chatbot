<!--
SYNC IMPACT REPORT (temporary; remove before committing)

Version change: 1.2.1 → 1.2.2 (PATCH: Principle VI clarifies that a visual choice pinned by the
PRD or a reference screen overrides the `frontend-design` skill; no new requirement)

Source: prd.md, frontend/ (current UI), and the user's request to improve the existing UI with
front-end design guidance.

Modified principles (old title → new title):
- None. Principles I through V are unchanged.

Added content:
- Principle VI. Accessible and Consistent UI Design (new). Covers accessibility, visible
  streaming and error states, one source of design tokens, and no change to behavior or scope.
- Technology and Architecture Constraints: the frontend bullet now points to Principle VI for
  visual and interaction rules.
- Development Workflow: UI changes MUST be checked against Principles IV and VI before merge.
- Governance: compliance review now covers Principles I through VI.

Added sections: none
Removed sections: none

Deferred items / TODOs:
- TODO(KNOWLEDGE_GROUNDING): Static system prompt vs. retrieval over a document store is
  an open PRD question (§6, §10). Principle III and the architecture section are written to
  hold for either approach.
- TODO(CHAT_RETENTION_POLICY): Retention period for chat transcripts is undefined (PRD §7,
  §10). Principle V requires that a policy be set before transcripts are stored.
- TODO(ESCALATION_MECHANISM): Ticketing, email, or live-agent queue is undefined (PRD §10).
- TODO(CUSTOMER_AUTH): Whether customers must authenticate before chatting is undefined
  (PRD §10). Principle II does not depend on it.
- TODO(TRAFFIC_VOLUME): Expected traffic volume is undefined (PRD §10).
- TODO(LATENCY_TARGET): The first-token target ("within a few seconds") is marked "to be
  validated" in PRD §7. Principle IV uses "a few seconds" until a number is set.
- Ratification date 2026-11-14 is the first adoption date. No earlier date was recorded.
-->

# Caramel AI Tech Support Chat Assistant Constitution

## Core Principles

### I. Product Scope Follows the PRD

`prd.md` is the source of truth for scope, requirements, and open questions. Features MUST
map to a functional requirement (FR-1 to FR-7) or to a non-functional requirement in PRD
§7. Requirements marked Must in the PRD MUST be implemented before the MVP ships. Requirements
marked Should (FR-6, FR-7) MAY be deferred only by a recorded decision in `prd.md`.
Out-of-scope items (account-specific actions such as plan changes or refunds, voice,
multi-language support, and cross-session history) MUST NOT be built in the MVP. A change to
scope MUST update `prd.md` before code is written. Rationale: the MVP boundary is what keeps
the assistant from taking account actions it is not authorized to take.

### II. Secrets Stay on the Backend

The Google AI Studio API key MUST be read from `.env` at the project root as `GOOGLE_API_KEY`,
and only by the backend. The key MUST NOT be sent to the frontend, included in a frontend
bundle, returned by any endpoint, or written to logs. `.env` MUST be listed in `.gitignore`
and MUST NOT be committed. Any credential that appears in a tracked file MUST be moved to an
environment variable before the repository is shared. Rationale: the key grants paid
inference access and cannot be rotated safely once leaked.

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

Logs MUST NOT contain personal data from chat messages, such as names, emails, account
identifiers, or payment details. Logging MUST be limited to what is needed for operations
(for example request ID, latency, status, and error type). No chat transcript MAY be stored
until a retention policy is defined and recorded in this constitution or in `prd.md`.
Rationale: the PRD leaves retention and compliance open, so the safe default is to collect
less.

### VI. Accessible and Consistent UI Design

Visual direction for the chat UI MUST follow the `frontend-design` skill, except where the
product PRD or a supplied reference screen pins a visual choice. A pinned choice governs, and
the skill guides every other visual decision. Every UI change
MUST keep the chat usable without a mouse and readable by assistive technology. Interactive controls (send, escalate, thumbs up/down, session actions) MUST be
reachable and operable by keyboard, MUST have an accessible name, and MUST show a visible
focus state. Text and interactive elements MUST meet WCAG 2.1 AA contrast in both light and
dark themes, if both are offered. New messages, streaming updates, and error messages MUST be
announced to assistive technology through a live region. Colors, spacing, typography, and
radii MUST come from a single set of design tokens, and components MUST NOT hard-code values
that bypass it. A visual change MUST NOT alter chat behavior, the escalation path, or the
data shown to the customer, and MUST NOT display the API key, personal data, or provider
error details. Rationale: a support tool is used under stress, often by customers who rely on
keyboard or screen readers, and a visual redesign must not break Principles IV or V.

## Technology and Architecture Constraints

- Frontend: React with TypeScript. The UI covers the message list, input box, streaming
  display, escalation action, chat history for the current session (FR-6), and thumbs up/down
  feedback (FR-7). Visual and interaction rules are set by Principle VI.
- Backend: Java with Spring Boot. It provides the streaming chat endpoints, session handling
  for multi-turn memory (FR-3), and escalation logic.
- AI layer: Spring AI, with a chat client for `gemma-4-26b-a4b-it` served through Google AI
  Studio.
- Knowledge grounding is not yet decided (static system prompt or retrieval over a document
  store). Until it is decided, shared interfaces MUST NOT assume either approach. Grounding
  logic MUST sit behind a boundary that can be replaced later.
- Each layer MUST NOT reach into another layer's internals. The frontend talks to the
  backend only, and never to the LLM provider directly.

## Development Workflow

- `CLAUDE.md` and `prd.md` MUST be read before design decisions are made.
- A change that touches secrets, streaming, the fallback or escalation path, or logging MUST
  be checked against Principles II, IV, and V before merge.
- A change to the UI MUST be checked against Principles IV and VI before merge, including a
  keyboard-only pass and a contrast check of any new colors.
- The success metrics in PRD §8 (resolution without escalation, thumbs-up ratio, escalation
  rate and reasons, median time to first response) MUST be instrumented before launch. They
  MUST use only the data allowed by Principle V.
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

Compliance review: every PR and review MUST check the changes against Principles I through VI.
A change that breaks a principle MUST either be fixed or be accompanied by an amendment.
Any added complexity MUST be justified in the PR description.

**Version**: 1.2.2 | **Ratified**: 2026-11-14 | **Last Amended**: 2026-11-14
