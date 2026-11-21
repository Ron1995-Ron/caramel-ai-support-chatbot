# Tasks: Customer Sign-In for the Support Chat

**Input**: Design documents from `specs/003-spec-auth/`

**Prerequisites**: [plan.md](plan.md) (required), [spec.md](spec.md) (user stories), [research.md](research.md), [data-model.md](data-model.md), [contracts/auth-api.yaml](contracts/auth-api.yaml), [quickstart.md](quickstart.md)

**Tests**: Test tasks are included only where [plan.md](plan.md) requires them (persistence tests, log-scan gate T-L1, frontend Vitest tests) or where a must-have requirement has no other automated check (ownership, lockout). Other behavior is verified by the scenarios in [quickstart.md](quickstart.md).

**Organization**: Tasks are grouped by user story (US1 to US5, in priority order) so each story can be built and checked on its own.

**Defaults in effect** (recorded in [plan.md](plan.md) and [prd-auth-3.md](../../prd-auth-3.md) section 14): maximum sign-in 30 days from last password entry (FR-009); account data deleted 30 days after no successful sign-in (FR-023); PostgreSQL with Flyway; Postmark as the email vendor, to be confirmed in research R5; idle timeout of 15 minutes read strictly (FR-009). The idle timeout (15 minutes, strict reading) and the inactivity definition (no successful sign-in for 30 days) are confirmed by the user.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 to US5, for user story phases only
- Paths are relative to the repository root

Backend base package: `backend/src/main/java/com/caramelai/support/chat/` (abbreviated `chat/` below).
Frontend base: `frontend/src/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependencies, local database, and configuration names

- [ ] T001 Add Spring Security, Spring Data JPA, Flyway with the PostgreSQL support module, the PostgreSQL JDBC driver, and test-scope Testcontainers (PostgreSQL and JUnit 5) to `backend/pom.xml`
- [ ] T002 Create `backend/compose.yaml` that starts only a PostgreSQL service for local development (no app container)
- [ ] T003 [P] Add datasource, JPA, and Flyway settings to `backend/src/main/resources/application.yml`, reading `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` from the environment with no defaults that contain secrets
- [ ] T004 [P] Add placeholder names only to `.env.example`: `AUTH_TOKEN_SIGNING_KEY`, `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `POSTMARK_SERVER_TOKEN`, `EMAIL_FROM_ADDRESS`, `APP_PUBLIC_URL` (Principle II; no values)
- [ ] T005 [P] Confirm `.gitignore` still covers `.env` and `.env.*` and that `.env.example` is the only env file tracked (check only; no change expected)
- [ ] T006 [P] Add the React Router dependency to `frontend/package.json` (pin at install time, R10)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, configuration, security chain, and token and email plumbing that every story uses. No story work starts until this phase is done.

**CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T007 Write Flyway migration `backend/src/main/resources/db/migration/V1__account.sql` creating table `account` with: `email` text, up to 254 characters, "Stored lowercase and trimmed. Unique. Required."; `password_hash` text, BCrypt hash, "Never returned, never logged."; `email_verified_at` timestamp, nullable, "Set once, when the verification link is used. Verification is one-way."; `created_at` timestamp, set on creation; `last_sign_in_at` timestamp, nullable, "Set on each successful sign-in." Add a unique index on `email` (data-model.md, Account)
- [ ] T008 Write Flyway migration `backend/src/main/resources/db/migration/V2__tokens_and_sessions.sql` creating `verification_token`, `password_reset_token`, `auth_session`, and `refresh_token` as specified in data-model.md: `token_hash` unique; `account_id` and `session_id` foreign keys with cascade delete; `auth_session.max_expires_at` is "`signed_in_at` + 30 days... Never extended."; `refresh_token.used_at` nullable; `auth_session.revoke_reason` enum of `SIGNED_OUT`, `PASSWORD_RESET`, `REFRESH_REUSE`
- [ ] T009 Write Flyway migration `backend/src/main/resources/db/migration/V3__indexes.sql` adding indexes on `verification_token.account_id`, `password_reset_token.account_id`, `auth_session.account_id`, and `refresh_token.session_id` (foreign-key lookups and cascade deletes), and on `account.last_sign_in_at` for the daily retention query (plan.md Project Structure, data-model.md Retention)
- [ ] T010 [P] Create the `Account` JPA entity in `chat/auth/account/Account.java` with the fields in T007 and the normalization rule "Stored lowercase and trimmed" applied in a single static method
- [ ] T011 [P] Create `chat/auth/account/AccountRepository.java` with lookup by normalized email and by `last_sign_in_at` for the retention job
- [ ] T012 [P] Create `VerificationToken.java` and `PasswordResetToken.java` entities in `chat/auth/token/` with `expires_at`, `used_at`, and `token_hash` per data-model.md
- [ ] T013 [P] Create `chat/auth/token/OneTimeTokenRepository.java` for both token types, with lookup by `token_hash`
- [ ] T014 [P] Create `AuthSession.java` and `RefreshToken.java` entities in `chat/auth/session/` with `signed_in_at`, `max_expires_at`, `last_activity_at`, `revoked_at`, `revoke_reason`, `used_at` per data-model.md
- [ ] T015 [P] Create `chat/auth/session/AuthSessionRepository.java` and `RefreshTokenRepository.java`
- [ ] T016 [P] Create `chat/config/AuthProperties.java` as a configuration record with: access lifetime 15 minutes, idle timeout 15 minutes, absolute cap 30 days, verification link lifetime 24 hours, reset link lifetime 30 minutes, lockout threshold 5 failures, lockout window 15 minutes, lockout duration 15 minutes, `APP_PUBLIC_URL`, cookie `Secure` flag (false only in the local profile)
- [ ] T017 [P] Create `chat/auth/token/TokenGenerator.java` that returns random 256-bit values encoded as URL-safe text, and `TokenHasher.java` that returns SHA-256 of a value (research R3: only hashes are stored)
- [ ] T018 [P] Create `chat/auth/AuthException.java` carrying a stable `code`, an HTTP status, and a customer-safe `detail`, using the codes listed in `contracts/auth-api.yaml` (Problem schema)
- [ ] T019 Extend `chat/api/ApiExceptionHandler.java` to map `AuthException` to a ProblemDetail body with the `code`, `status`, and `detail` fields from the contract
- [ ] T020 Create `chat/auth/session/AccessTokenService.java` that issues and checks an HMAC-SHA256 access token with claims `sub` (account ID), `sid` (session ID), and expiry 15 minutes; the key comes from `AUTH_TOKEN_SIGNING_KEY` and must be at least 256 bits (data-model and research R3)
- [ ] T021 Create `chat/auth/session/AuthCookies.java` setting the access cookie (`Path=/`) and refresh cookie (`Path=/api/auth`), both `HttpOnly` and `SameSite=Lax`, `Secure` unless the local profile is active (Principle VII, research R4)
- [ ] T022 Create `chat/config/SecurityConfig.java` with a stateless filter chain, a 401 entry point returning code `UNAUTHENTICATED`, a 403 handler, and CSRF using a readable cookie token repository (research R1 and R4). Every path under `/api/**` requires authentication except the auth endpoints listed in the contract
- [ ] T023 Create `chat/auth/session/AuthenticationFilter.java` that reads the access cookie, validates it with T020, loads the `AuthSession`, rejects it when revoked, idle for more than 15 minutes, or past `max_expires_at`, updates `last_activity_at` at most once a minute, and sets the request principal (data-model: "Access rule")
- [ ] T024 Register `AuthenticationFilter` in `chat/config/SecurityConfig.java` before the authorization check (depends on T022, T023)
- [ ] T025 Create `chat/auth/CurrentAccount.java`, a port that returns the signed-in account ID and throws `AuthException` `UNAUTHENTICATED` when none exists. Chat controllers use this port and never read tokens directly (plan Structure Decision)
- [ ] T026 Create `chat/auth/account/PasswordPolicy.java` enforcing "at least 12 characters", "at most 72 bytes" (counted in bytes; spaces kept as typed), and the breached-password check, with codes `PASSWORD_TOO_SHORT`, `PASSWORD_TOO_LONG`, `PASSWORD_BREACHED` (spec FR-006, research R2)
- [ ] T027 [P] Add the bundled breached-password list at `backend/src/main/resources/auth/breached-passwords.txt`. Confirm its source and license first (research R6). Create `chat/auth/account/BreachedPasswords.java` that loads it from the classpath and checks case-insensitively
- [ ] T028 Create the BCrypt `PasswordEncoder` bean at strength 12 in `chat/config/SecurityConfig.java` (research R2)
- [ ] T029 [P] Create `chat/auth/email/EmailSender.java` (port) and `EmailMessage.java` (record of recipient, subject, text body)
- [ ] T030 Create `chat/auth/email/PostmarkEmailSender.java` implementing T029 with `RestClient`, a 5-second timeout, `POSTMARK_SERVER_TOKEN` and `EMAIL_FROM_ADDRESS` from the environment, and a failure that surfaces as `AuthException` `EMAIL_UNAVAILABLE` (503). Do not log message bodies or recipients (Principle V, research R5)
- [ ] T031 [P] Create `chat/auth/lockout/ClientAddress.java` that returns the connection address, using the forwarding header only when the proxy is listed as trusted in `AuthProperties` (research R7)
- [ ] T032 [P] Create `chat/auth/lockout/LoginLockout.java`: in-memory counters keyed by a keyed hash of the email address or client address, a sliding 15-minute window, and a 15-minute lock after 5 failures (research R7, assumption A2)
- [ ] T033 Verify `chat/config/RequestLoggingFilter.java` does not log request bodies, cookies, or the `X-XSRF-TOKEN` header for `/api/auth/**` (Principle V). Change it if it does

**Checkpoint**: Foundation ready. Schema, security chain, token, email, and lockout plumbing exist. User stories can now proceed.

---

## Phase 3: User Story 1 - Create an account and verify email (Priority: P1) MVP

**Goal**: A visitor creates an account, gets a verification email, confirms the address, and can resend the email if the link is lost or expired. Chat is blocked until verification.

**Independent Test**: Quickstart S1 and S2. Sign up, confirm the address, and check the chat opens. Check the chat stays closed before confirmation, and that a resent email works while the earlier link does not.

### Tests for User Story 1 (plan-required persistence test)

- [ ] T034 [US1] Write a Testcontainers PostgreSQL test in `backend/src/test/java/com/caramelai/support/chat/auth/account/AccountPersistenceTest.java` that applies V1 to V3 migrations and checks that a duplicate `email` is rejected and that deleting an account cascades to its tokens and sessions

### Implementation for User Story 1

- [ ] T035 [US1] Create `chat/auth/account/SignUpService.java`. Normalize the email. For a new address: create the account and a verification token (stored as a hash, `expires_at` 24 hours after creation), then send the verification email, all in one transaction. If the send fails, the transaction rolls back and the customer gets `EMAIL_UNAVAILABLE` (research R5, FR-024). For a registered address: send the owner a notice and return the same response. Do not reveal which case happened (FR-018)
- [ ] T036 [US1] Create `chat/auth/token/VerificationService.java` with `confirm(tokenValue)`. Reject a value that is unknown, used, or past `expires_at`, with `TOKEN_INVALID` or `TOKEN_EXPIRED` (410). On success set `used_at` and `account.email_verified_at`. Verification is one-way (data-model: Account state)
- [ ] T037 [US1] Create `chat/auth/token/ResendVerificationService.java`: for an unverified account, set `used_at` on each active earlier verification token (superseded), create a new token, and send the email. For a verified or unknown address, do nothing but return the same response (FR-005, AUTH-14)
- [ ] T038 [US1] Create `chat/auth/email/AuthEmailTemplates.java` with the verification email (link to `APP_PUBLIC_URL` + `/verify-email` carrying the token) and the already-registered notice. The templates must not log their contents (Principle V)
- [ ] T039 [US1] Create request and response records in `chat/auth/api/AuthDtos.java`. `SignUpRequest.email`: "up to 254 characters"; `SignUpRequest.password`: "at least 12 characters", "at most 72 bytes", counted in bytes, spaces kept. Use Bean Validation with a custom byte-length check for the password
- [ ] T040 [US1] Create `chat/auth/api/AuthController.java` with `GET /api/auth/csrf` (204, sets the XSRF cookie), `POST /api/auth/signup` (202), `POST /api/auth/verify-email` (204 or 410), and `POST /api/auth/resend-verification` (202), matching `contracts/auth-api.yaml`
- [ ] T041 [US1] Permit the US1 endpoints in `chat/config/SecurityConfig.java` (`/api/auth/csrf`, `/api/auth/signup`, `/api/auth/verify-email`, `/api/auth/resend-verification`) and keep CSRF enforced on their POSTs
- [ ] T042 [US1] Enforce the verified-account rule for chat: `CurrentAccount.requireVerified()` in `chat/auth/CurrentAccount.java` returns `EMAIL_NOT_VERIFIED` (403) for unverified accounts (AUTH-2)
- [ ] T043 [P] [US1] Create `frontend/src/services/authApi.ts` with functions for `csrf`, `signUp`, `verifyEmail`, and `resendVerification`. Each sends the `X-XSRF-TOKEN` header read from the readable XSRF cookie, and never reads or stores a session credential (Principle VII)
- [ ] T044 [P] [US1] Create `frontend/src/pages/SignUpPage.tsx`: labeled email and password fields, a visible note on the password rule, error text in a live region (`role="alert"`), keyboard-operable, visible focus (Principle VI)
- [ ] T045 [P] [US1] Create `frontend/src/pages/VerifyEmailPage.tsx`: reads `token` from the URL, sends it once to `verifyEmail`, calls `history.replaceState` to remove it from the address bar, and sets `Referrer-Policy: no-referrer` for this page (research R10). Shows the expired-link message with a resend button
- [ ] T046 [P] [US1] Create `frontend/src/pages/VerifyPendingPage.tsx` shown after sign-up and when the customer is unverified, with a resend control and the same response text whether or not the email is registered

**Checkpoint**: User Story 1 is functional. Quickstart S1 and S2 can be run.

---

## Phase 4: User Story 2 - Sign in, sign out, and use more than one device (Priority: P1)

**Goal**: A verified customer signs in, chats, and signs out. A session stays active while the customer is active, ends after 15 minutes of inactivity, and multiple devices stay independent.

**Independent Test**: Quickstart S4, S5, and S6. Sign in on two browsers and sign out on one, and check the other still works. Leave the tab idle for more than 15 minutes and check the redirect.

### Implementation for User Story 2

- [ ] T047 [US2] Create `chat/auth/session/SignInService.java`. Normalize the email and check lockout before comparing the password. On a wrong password or unknown email, record a failure and return the same `INVALID_CREDENTIALS` response. Compare the password against a dummy hash for unknown emails so timing does not reveal existence. Return `EMAIL_NOT_VERIFIED` (403) only after a correct password on an unverified account (plan, data-model)
- [ ] T048 [US2] In `chat/auth/session/SignInService.java`, on success create an `AuthSession` with `signed_in_at` and `last_activity_at` set to now, `max_expires_at` set to `signed_in_at` + 30 days, issue a refresh token (stored as a hash), set `last_sign_in_at` on the account, and return the access and refresh cookies (R3)
- [ ] T049 [US2] Create `chat/auth/session/RefreshService.java`: look up the refresh token by hash. If it was already used, revoke the session with `REFRESH_REUSE`. Refuse when `last_activity_at` is more than 15 minutes old or `max_expires_at` has passed. Otherwise mark the old token used, issue a new refresh token and access token (rotation, R3)
- [ ] T050 [US2] Create `chat/auth/session/LogoutService.java` that revokes only the current session with `SIGNED_OUT` and clears both cookies (AUTH-4, AUTH-15)
- [ ] T051 [US2] Add `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`, and `GET /api/auth/me` to `chat/auth/api/AuthController.java`, matching `contracts/auth-api.yaml`. `GET /me` returns `accountId`, `email`, and `verified`
- [ ] T052 [US2] Permit `/api/auth/login` and `/api/auth/refresh` in `SecurityConfig`. `/api/auth/logout` and `/api/auth/me` require an authenticated session
- [ ] T053 [P] [US2] Add `login`, `refresh`, `logout`, and `me` to `frontend/src/services/authApi.ts`, using the CSRF header on the POSTs
- [ ] T054 [US2] Create `frontend/src/auth/AuthContext.tsx` that holds only the current account (email, verified flag) and status. It calls `GET /api/auth/me` on load. It never stores a credential (Principle VII)
- [ ] T055 [US2] Create `frontend/src/auth/RequireAuth.tsx` route guard that redirects signed-out visitors to `/sign-in` and keeps the intended path to return to after sign-in (AUTH-12)
- [ ] T056 [US2] Create `frontend/src/auth/useSessionRenewal.ts` that calls `POST /api/auth/refresh` before the access credential expires while the tab is active, and on a 401 redirects to `/sign-in` with the message that the session ended (FR-009, AUTH-12)
- [ ] T057 [P] [US2] Create `frontend/src/pages/SignInPage.tsx` with labeled fields, live-region errors, a link to forgot password, and a resend-verification action when `EMAIL_NOT_VERIFIED` is returned (Principle VI)
- [ ] T058 [US2] Update `frontend/src/App.tsx` and `frontend/src/main.tsx` to add React Router routes for `/sign-up`, `/sign-in`, `/verify-email`, `/forgot-password`, `/reset-password`, `/chat`, and `/`, with `/chat` wrapped in `RequireAuth` and `/` redirecting to `/chat` (research R10)
- [ ] T059 [P] [US2] Create `frontend/src/components/AccountMenu.tsx` showing the signed-in email and a sign-out button, placed in the chat header. Sign-out calls `POST /api/auth/logout` and returns to `/sign-in` (AUTH-12). Keyboard operable, visible focus, accessible name
- [ ] T060 [US2] Make `frontend/src/services/chatApi.ts` treat a 401 response as session end: redirect to `/sign-in` with the ended-session message, and show `EMAIL_NOT_VERIFIED` as the chat-blocked message. Send `X-XSRF-TOKEN` on POST requests
- [ ] T061 [US2] Make `frontend/src/pages/ChatPage.tsx` create the chat session through the signed-in path only after `RequireAuth` passes, and keep the existing streaming behavior unchanged (Principles IV and VI)

**Checkpoint**: User Stories 1 and 2 work. Sign-in, sign-out, multi-device, and idle expiry can be checked.

---

## Phase 5: User Story 3 - Recover a forgotten password (Priority: P2)

**Goal**: A customer requests a reset link, sets a new password, and every device on the account is signed out.

**Independent Test**: Quickstart S9 and S10. Sign in on two devices, request a reset, set a new password, and check both devices are signed out. Check the response is the same for a registered and an unregistered email.

### Implementation for User Story 3

- [ ] T062 [US3] Create `chat/auth/account/PasswordResetService.java` with `requestReset(email)`. For a registered address, supersede earlier active reset tokens, create a new one (stored as a hash, `expires_at` 30 minutes after creation), and send the reset email. For any address return the same response (FR-013, AUTH-13)
- [ ] T063 [US3] In `chat/auth/account/PasswordResetService.java`, add `reset(tokenValue, newPassword)`. Check the token is unused and not expired (`TOKEN_EXPIRED` 410 or `TOKEN_INVALID`). Apply `PasswordPolicy` (FR-015). Store the BCrypt hash, set `used_at`, and revoke every active `AuthSession` for the account with `PASSWORD_RESET` (FR-016, AUTH-16)
- [ ] T064 [US3] Add the reset email template to `chat/auth/email/AuthEmailTemplates.java`, linking to `APP_PUBLIC_URL` + `/reset-password` with the token
- [ ] T065 [US3] Add `POST /api/auth/forgot-password` and `POST /api/auth/reset-password` to `chat/auth/api/AuthController.java`. Reset returns 204 on success, 410 for an expired or used link, and 422 for a password rule failure, matching the contract
- [ ] T066 [US3] Permit `/api/auth/forgot-password` and `/api/auth/reset-password` in `SecurityConfig`, with CSRF still enforced
- [ ] T067 [P] [US3] Add `forgotPassword` and `resetPassword` to `frontend/src/services/authApi.ts`
- [ ] T068 [P] [US3] Create `frontend/src/pages/ForgotPasswordPage.tsx` showing the same confirmation whether or not the email is registered (AUTH-13)
- [ ] T069 [P] [US3] Create `frontend/src/pages/ResetPasswordPage.tsx`: reads `token` from the URL, sends it with the new password, removes it from the address bar with `history.replaceState`, sets `Referrer-Policy: no-referrer`, and shows the expired-link message with a link to request another (research R10)

**Checkpoint**: User Stories 1 to 3 work. Quickstart S9 and S10 can be run.

---

## Phase 6: User Story 4 - Protected chat, feedback, and escalation (Priority: P1)

**Goal**: Chat, feedback, and escalation work only for signed-in, verified customers. Each customer sees only their own sessions. Escalation tickets carry the customer's account ID and email.

**Independent Test**: Quickstart S8 and S13. Requests with no session get 401, a session owned by another customer gets 404, and a valid session's escalation includes the account ID and email.

### Tests for User Story 4 (requirement AUTH-6 has no other automated check)

- [ ] T070 [US4] Write `backend/src/test/java/com/caramelai/support/chat/api/SessionOwnershipTest.java` checking that a second customer gets 404 `SESSION_NOT_FOUND` for messages, feedback, and escalation on a session they do not own, and that the response matches an unknown session ID

### Implementation for User Story 4

- [ ] T071 [US4] Add `ownerAccountId` to `chat/session/Session.java`, set when the session is created, and expose it through an accessor
- [ ] T072 [US4] Add `createOwnedBy(accountId)` and `getOwnedBy(sessionId, accountId)` to `chat/session/SessionStore.java`. `getOwnedBy` throws `SessionNotFoundException` for a missing, expired, or differently owned session (AUTH-6, research R9)
- [ ] T073 [US4] Update `chat/api/SessionController.java` so `POST /api/sessions` requires `CurrentAccount.requireVerified()` and creates an owned session. Remove the anonymous path (research R9)
- [ ] T074 [US4] Update `chat/api/MessageController.java` (list and send) to use `getOwnedBy` with the current account ID, so no path reaches a session without the ownership check
- [ ] T075 [P] [US4] Update `chat/api/FeedbackController.java` to use `getOwnedBy` for every feedback operation
- [ ] T076 [P] [US4] Update `chat/api/EscalationController.java` to use `getOwnedBy`, and pass the account ID and email to the escalation service
- [ ] T077 [US4] Add `accountId` (UUID) and `customerEmail` (text) to `TicketPayload` in `chat/escalation/EscalationRequest.java`. Both are required for every escalation (AUTH-10, data-model: Escalation Ticket Payload). The payload still excludes the transcript
- [ ] T078 [US4] Update `chat/escalation/EscalationService.java` `escalate(...)` to accept the account (ID and email) and populate `TicketPayload` (AUTH-10)
- [ ] T079 [P] [US4] Verify `chat/escalation/LoggingTicketingClient.java` logs only the reference and reason, not the email or account ID (Principle V). Change it if it does
- [ ] T080 [P] [US4] Update `frontend/src/services/chatApi.ts` so `requestEscalation` and `sendFeedback` send the CSRF header (the 401 and 403 handling is in T060)
- [ ] T081 [US4] Update `frontend/src/state/useChat.ts` to show `EMAIL_NOT_VERIFIED` and `SESSION_NOT_FOUND` as clear customer messages, without showing raw error detail (Principles IV and V)

**Checkpoint**: User Stories 1 to 4 work. Chat, feedback, and escalation are protected and owned.

---

## Phase 7: User Story 5 - Protection against repeated sign-in attempts (Priority: P2)

**Goal**: Repeated failed sign-ins for one account or from one source are temporarily blocked with a clear, non-revealing message.

**Independent Test**: Quickstart S11. Five wrong passwords lock the account for 15 minutes, the lock also applies per source, and it lifts after the window.

### Tests for User Story 5

- [ ] T082 [US5] Write `backend/src/test/java/com/caramelai/support/chat/auth/lockout/LoginLockoutTest.java` checking: the sixth attempt is blocked, the lock applies to unknown email addresses too (no enumeration), the per-source lock applies across accounts, and the lock lifts after 15 minutes

### Implementation for User Story 5

- [ ] T083 [US5] In `chat/auth/session/SignInService.java`, check `LoginLockout` before the password comparison, record failures for both the account key and the source key, and clear the account key on success (FR-017)
- [ ] T084 [US5] Return HTTP 429 with the code `ACCOUNT_LOCKED` and the same generic text for a locked attempt, whether or not the email is registered, in `chat/auth/api/AuthController.java` and the `AuthException` mapping
- [ ] T085 [US5] Make the lockout threshold and durations configurable through `AuthProperties` in `chat/config/AuthProperties.java` so the support team can change the proposed values (assumption A2)

**Checkpoint**: All five user stories work.

---

## Phase 8: Retention and Cleanup (FR-023, cross-cutting)

**Purpose**: Apply the decided 30-day retention rules. This phase has no user-story label because it serves the whole feature.

- [ ] T086 Create `chat/auth/retention/InactiveAccountCleanup.java` as a daily `@Scheduled` job that deletes accounts with no successful sign-in for 30 days. For accounts that never signed in, the 30 days run from `created_at` (prd-auth-3.md section 14, research R8)
- [ ] T087 In `chat/auth/retention/InactiveAccountCleanup.java`, delete expired verification tokens, password reset tokens, and auth sessions that are revoked or expired, once they are past their useful life
- [ ] T088 Add `SessionStore.removeOwnedBy(accountId)` in `chat/session/SessionStore.java`, called from the cleanup job when an account is deleted, so its in-memory chat sessions are dropped at the same time (research R8)
- [ ] T089 Make the retention window and the daily schedule configurable in `AuthProperties` in `chat/config/AuthProperties.java` so a test profile can shorten them (quickstart S15)

---

## Phase 9: Polish and Cross-Cutting Concerns

**Purpose**: Gate checks, documentation, accessibility, and the verification run

- [ ] T090 Write `backend/src/test/java/com/caramelai/support/chat/auth/LogHygieneTest.java`, the log-scan gate T-L1: run sign-up, sign-in, reset, and escalation, then fail if a password, token value, full email address, account ID, session ID, or cookie value appears in the captured log (Principle V, quickstart S14)
- [ ] T091 [P] Update `specs/001-support-chat-assistant/contracts/chat-api.yaml` to add the 401, 403, and 404 responses for protected endpoints, and to remove the anonymous session creation path (plan, research R9)
- [ ] T092 [P] Record the local commands for the database, the auth tests, and the frontend auth screens in `CLAUDE.md` (Development Workflow, constitution)
- [ ] T093 Run the keyboard-only pass and the contrast check for every new screen and color, as Principle VI requires, and record the result in `specs/003-spec-auth/checklists/accessibility.md`
- [ ] T094 Run `mvn spotless:check`, `mvn test`, `npm run lint`, `npm run format:check`, and `npm run build`. Fix any failures and record the output
- [ ] T095 Run `specs/003-spec-auth/quickstart.md` scenarios S1 to S17 against the running app and record pass or fail for each, as spec 001 did (T049 there)
- [ ] T096 Obtain the maintainer review of the constitution PATCH 1.3.1 in `.specify/memory/constitution.md` (Governance requires review before merge), and confirm the Sync Impact Report entry
- [ ] T097 [P] Add Vitest and React Testing Library to `frontend/package.json` (plan.md Testing) and write tests for the sign-up and sign-in pages and for the `RequireAuth` route guard (redirect to `/sign-in`, and return to the intended path after sign-in)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup. Blocks all user stories.
- **US1 (Phase 3)**: Depends on Foundational. Needed by US2, because sign-in needs verified accounts.
- **US2 (Phase 4)**: Depends on US1 (accounts and verification).
- **US3 (Phase 5)**: Depends on US2 (sessions are revoked on reset, and the sign-in page exists).
- **US4 (Phase 6)**: Depends on US2 (needs `CurrentAccount` with sessions). Independent of US3.
- **US5 (Phase 7)**: Depends on US2 (lockout is part of sign-in).
- **Retention (Phase 8)**: Depends on US1 to US4 (uses accounts, tokens, sessions, and in-memory chat sessions).
- **Polish (Phase 9)**: Depends on the stories being shipped.

### Within Each User Story

- Tests written and failing before implementation, where a test task exists
- Entities and repositories before services
- Services before controllers
- Backend endpoints before the frontend calls that use them

### Parallel Opportunities

- Setup: T003, T004, T005, T006 run in parallel
- Foundational: T010 to T018, T027, T029, T031, T032 run in parallel once T007 to T009 exist
- US1 frontend: T043, T044, T045, T046 run in parallel
- US2 frontend: T053, T057, T059 run in parallel
- US3 frontend: T067, T068, T069 run in parallel
- US4: T075, T076, T079, T080 run in parallel

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (Setup) and Phase 2 (Foundational)
2. Complete Phase 3 (US1)
3. Stop and check quickstart S1 and S2

Note that US1 on its own does not let a customer chat, since sign-in (US2) is the gate to chat. The minimum to make chat usable is Phases 1 to 4 and Phase 6 (US4), which together cover FR-001 to FR-012 and FR-019.

### Incremental Delivery

1. Setup and Foundational
2. US1: sign-up and verification
3. US2: sign-in, sign-out, sessions
4. US4: protected chat and escalation identity (the security boundary)
5. US3: password reset
6. US5: lockout
7. Retention, then Polish and the full quickstart run

---

## Notes

- [P] tasks touch different files and have no unfinished dependency
- Commit after each task or logical group, but only when the user asks
- Stop at any checkpoint to check that story on its own
- Open items that block launch, not development: the real ticketing system (the stand-in client still creates no real tickets), the breached-password list license (T027), and the Postmark sender domain's SPF and DKIM records (R5)
