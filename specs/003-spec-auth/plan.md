# Implementation Plan: Customer Sign-In for the Support Chat

**Branch**: `003-spec-auth` (no branch created) | **Date**: 2026-11-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from [spec.md](spec.md), with product context in [prd-auth-3.md](../../prd-auth-3.md), the earlier auth PRD [002-prd-user-auth.md](../../002-prd-user-auth.md), and the governing rules in [constitution.md](../../.specify/memory/constitution.md) (v1.3.0).

## Summary

Customers create an account, verify their email, and sign in before using the support chat. Each chat session, message, feedback record, and escalation belongs to one signed-in account, and the backend enforces that. Sign-in uses a short-lived access credential and a rotating refresh credential, both in cookies the page cannot read. Sign-out, password reset, and expiry end sessions on the server. Account data lives in a PostgreSQL database with migrations. The chat itself stays in memory, as it is today.

The chat streaming, the assistant, and the escalation logic keep their current behavior. The changes are an identity layer in front of them, the account and session storage, a route guard and auth screens in the frontend, and an account identifier and email on escalation tickets.

## Decisions made for this plan

These are the recommended defaults the user chose. Each one is recorded here so it can be reviewed.

| Decision | Choice | Source |
|----------|--------|--------|
| Maximum continuous sign-in | 30 days from last password entry (FR-009) | Spec, recommended default |
| Account and transcript retention | Account data deleted after 30 days with no successful sign-in; transcripts deleted 30 days after session end (FR-023) | Spec, recommended default |
| Idle timeout | 15 minutes of inactivity ends a session; a returning customer signs in again (FR-009, strict reading) | Confirmed by the user |
| Inactivity for deletion | No successful sign-in for 30 days (FR-023) | Confirmed by the user |
| Database | PostgreSQL with Flyway migrations | Plan, recommended default |
| Transactional email | HTTP API provider, Postmark as the default vendor, verified sender domain | Plan, recommended default. Confirm in research.md R5. |

## Technical Context

**Language/Version**: Java 21 with Spring Boot 4.1.2 (backend, as pinned in CLAUDE.md). TypeScript with React 19 and Vite 8 (frontend).

**Primary Dependencies**: Existing: Spring Web MVC, Spring AI 2.0.2 with Google GenAI, Spring Validation. New: Spring Security (authentication, CSRF, password hashing), Spring Data JPA and Flyway (persistence and migrations), a PostgreSQL JDBC driver, and React Router (frontend routes and the route guard). The email client uses the existing `RestClient`, so it adds no new library.

**Storage**: PostgreSQL for accounts, verification and reset tokens, authenticated sessions, and refresh tokens. Chat sessions, messages, and feedback stay in memory (research.md R9).

**Testing**: JUnit 5 and Spring Boot test slices for the backend. Testcontainers with PostgreSQL for the persistence and migration tests. Vitest and React Testing Library for the auth screens and the route guard. The end-to-end run is in quickstart.md.

**Target Platform**: Linux server for the backend and PostgreSQL. Modern desktop and mobile browsers.

**Project Type**: Web application (frontend and backend), with a new relational database.

**Performance Goals**: Sign-in completes in under 1 second at the median, excluding the deliberate hashing cost and any lockout delay (SC-003). The first chat token target from spec 001 is unchanged.

**Constraints**: Secrets (token signing key, database password, email provider token) are read by the backend from `.env` or the environment only (Principle II). Auth cookies are `HttpOnly`, `SameSite=Lax`, and `Secure` outside local development (Principle VII). Account and chat data are not logged (Principle V). Single backend instance for the MVP, since lockout counters and chat sessions are in memory (research.md R7, R9).

**Scale/Scope**: Designed for one backend instance serving tens of concurrent signed-in customers. Traffic volume is still undefined (PRD §10). Scaling out needs a shared store for lockout counters and chat sessions, which is a later decision.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|-----------|-------|--------|
| I. Product Scope Follows the PRD | Every FR maps to AUTH-1 to AUTH-16 (prd-auth-3.md) and to the 001 FRs. Account actions, MFA, SSO, and social login are excluded. The constitution says a numbered PRD takes effect for scope only after the PRD file references it. The constitution, CLAUDE.md, and the PRD references now use `001-prd.md` (constitution 1.3.1, PATCH). | PASS (gate T-G1 closed) |
| II. Secrets Stay on the Backend | New secrets: token signing key, database credentials, email provider token. All are read by the backend only, placed in `.env` or the environment, and never returned or logged. `.env.example` gets placeholder names only. The GitHub MCP token has already been moved to an environment variable. | PASS (conditional on `.env.example` placeholders and the `.gitignore` check) |
| III. Grounded and Honest Answers | The assistant and its system prompt are unchanged. | PASS (not affected) |
| IV. Reliable Streaming, No Silent Failures | Unauthenticated chat requests get a 401 before any stream starts. Email outage on sign-up, resend, or reset returns a clear error (FR-024). Streaming behavior is unchanged. | PASS |
| V. Minimal Personal Data in Logs | Logs use request ID, status, latency, and error type only. Never account IDs, session IDs, email, password, token, or IP address. Lockout counters are in memory and not logged. | PASS (enforced by a log-scan test, T-L1) |
| V. Retention policy before storing data | The 30-day rules in FR-023 are decided, but the constitution requires them to be recorded in the constitution or `001-prd.md` before any account data is stored. Transcripts stay in memory and expire within 30 minutes idle, so they are already compliant. | PASS (retention recorded in prd-auth-3.md section 14, referenced from 001-prd.md; gate T-G2 closed) |
| VI. Accessible and Consistent UI Design | Auth screens use the existing design tokens and the keyboard and focus rules. Error text uses a live region. Labels are on every field. A keyboard-only pass and a contrast check are required before merge. | PASS (by design; verified in implementation) |
| VII. Authenticated Access and Account Data | Covered in research.md R1 to R4 and R9. Every chat, feedback, and escalation endpoint requires a session (401). Ownership is checked on the backend (404 for other customers' sessions). Passwords use BCrypt. Tokens are random, single-use, and stored hashed. Cookies are `HttpOnly`. CSRF is enforced on cookie-authenticated writes. Account data is limited to email, hash, verification status, and timestamps. Sign-in unlocks no account actions. | PASS |

**Development Workflow check**: Changes to authentication, sessions, cookies, and account data are checked against Principles II, V, and VII (this table is that check). Any UI change is checked against IV, VI, and VII.

**Post-design re-check (after Phase 1)**: No new violations were found by the design. Gates T-G1 and T-G2 are closed. Gate T-L1 (the log-scan test) is a build-time task (tasks T090), and it must pass before code is merged.

## Gates and Tasks Before Code

- **T-G1 (closed)**: The PRD references now point to `001-prd.md`. The constitution gets a PATCH (1.3.1) with a Sync Impact Report entry. It still needs maintainer review before it is merged, as Governance requires.
- **T-G2 (closed)**: The FR-023 retention rules are recorded in `prd-auth-3.md` section 14, and `001-prd.md` points to that PRD. Principle V is met before any account row is written.
- **T-L1**: Add a test that fails if a password, token, or full email address appears in log output during sign-up, sign-in, reset, and escalation.

## Project Structure

### Documentation (this feature)

```text
specs/003-spec-auth/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── auth-api.yaml    # Phase 1 output (auth endpoints, and the auth errors on chat endpoints)
├── checklists/
│   └── requirements.md
└── tasks.md             # Generated by /speckit-tasks (not by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/caramelai/support/chat/
│   ├── auth/            # NEW. Sign-up, verification, sign-in, sign-out, refresh, reset, lockout
│   │   ├── account/     # Account entity, repository, password policy, breached-password check
│   │   ├── token/       # Single-use verification and reset tokens (hashed), refresh token rotation
│   │   ├── session/     # Authenticated session records, access token issuing and checking
│   │   ├── email/       # Email port and the transactional email adapter
│   │   ├── lockout/     # In-memory failed-attempt counters per account and per source
│   │   ├── retention/   # Scheduled deletion of inactive accounts
│   │   └── api/         # /api/auth/* controllers and DTOs
│   ├── config/          # Security configuration, cookie settings, CSRF, datasource
│   ├── api/             # EXISTING. Session, message, feedback, and escalation controllers now take the signed-in account
│   ├── session/         # EXISTING. Session gains an owner account ID. Lookups check ownership.
│   └── escalation/      # EXISTING. Ticket payload gains account ID and email (AUTH-10)
├── src/main/resources/db/migration/   # NEW. Flyway migrations (V1 accounts, V2 auth, V3 indexes)
└── src/test/java/...    # Unit, slice, Testcontainers persistence, and log-scan tests

frontend/
├── src/
│   ├── auth/            # NEW. Auth context, route guard, CSRF header handling
│   ├── pages/           # NEW: SignUpPage, SignInPage, VerifyPendingPage, VerifyEmailPage, ForgotPasswordPage, ResetPasswordPage. ChatPage stays.
│   ├── components/      # EXISTING. Adds the signed-in email and sign-out control to the header.
│   ├── services/        # EXISTING chatApi.ts. NEW authApi.ts. Sends credentials and the CSRF header.
│   └── state/           # EXISTING. No tokens stored here (Principle VII).
└── src/                 # Tests colocated with components and pages
```

**Structure Decision**: Web application (Option 2), as in spec 001. Auth is a new backend package, `auth`, with the existing chat packages depending on it only through a small "current account" interface. This keeps the chat code free of token logic and keeps Principle VII checks in one place. The frontend keeps all tokens in cookies, so state holds only the signed-in email and status.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| PostgreSQL added to a stack that had no database | Accounts, one-time tokens, and sessions must survive a restart and be shared by later instances (FR-023, AUTH-6, AUTH-16) | In-memory accounts would lose every customer on restart, which breaks sign-in and the 30-day retention rule |
| Server-side authenticated session record, checked on each request | AUTH-4 and AUTH-16 require a sign-out or reset to end access immediately. A stateless access token would stay valid for up to 15 minutes. | Stateless tokens with no record cannot revoke access on sign-out, so they fail AUTH-4 |

No other violations.
