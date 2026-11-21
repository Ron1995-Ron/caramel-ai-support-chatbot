# PRD: User Authentication for the Caramel AI Tech Support Chat Assistant (v3)

**Status**: Draft

**Date**: 2026-11-21

**Builds on**: [002-prd-user-auth.md](002-prd-user-auth.md). Where the two differ, this document governs. Traceable to [spec 003-spec-auth](specs/003-spec-auth/spec.md).

## 1. Objective

Let customers sign in before chatting with the Caramel AI support assistant, so that each chat session belongs to a verified user. This resolves the open question in [001-prd.md](001-prd.md): "Is authentication required for customers before chatting?"

## 2. Users

- **Customers** of Caramel AI who want to use the support chat. They need a simple, secure way to prove who they are.
- **Support agents** who receive escalations and need to know which customer an escalated chat came from.

## 3. Goals

- Tie each chat session and each escalation to an authenticated customer identity.
- Protect the chat API from anonymous abuse, since LLM calls cost money.
- Keep sign-in quick enough that it does not discourage customers from asking for help.
- Recover customers who are stuck, either because their verification email expired or because they forgot their password.
- Store as little personal data as possible.

## 4. What changed from v2

**Resolved:**

- Customers must sign in before chatting. Anonymous chat is not offered (constitution Principle VII).
- Customers use separate support-chat accounts. Reuse of Caramel AI platform accounts (SSO) is out of MVP scope.
- Multi-factor authentication is not required at launch.

**Added (gaps in v2 that would leave customers stuck):**

- Resend verification email (AUTH-14).
- Signed in on more than one device at a time (AUTH-15).
- A successful password reset ends all sessions for that account (AUTH-16).

**Proposed, awaiting support-team confirmation:**

- Lockout thresholds (AUTH-9).
- Session timing targets (section 9).

**Still open:** database, email provider, maximum continuous sign-in length, and account and transcript retention rules (section 10).

## 5. Scope

### In scope (MVP)

- Sign up with email and password.
- Email verification before the first chat, including resending the verification email.
- Sign in and sign out.
- Password reset by email, which ends all existing sessions for the account.
- Authenticated sessions (access token plus refresh) for the web UI, usable on more than one device.
- Protecting the chat, feedback, and escalation endpoints so they require sign-in.
- Rate limiting and lockout on sign-in attempts.
- Attaching the authenticated user ID to chat sessions and escalation requests.

### Out of scope (MVP)

- Social login (Google, GitHub) and enterprise SSO.
- Multi-factor authentication (planned for a later release).
- Role management beyond a single "customer" role.
- Linking to Caramel AI billing or API-key accounts.
- Account deletion and profile editing screens.
- Cross-session chat history (still out of scope per [001-prd.md](001-prd.md)).

## 6. Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| AUTH-1 | A visitor can create an account with an email address and a password. | Must |
| AUTH-2 | The system sends a verification email; an unverified account cannot start a chat. | Must |
| AUTH-3 | A verified user can sign in with email and password and receive an authenticated session. | Must |
| AUTH-4 | A signed-in user can sign out, which ends the session on the server. | Must |
| AUTH-5 | Chat, feedback, and escalation endpoints reject requests without a valid session (HTTP 401). | Must |
| AUTH-6 | A user can only read and write their own chat sessions (HTTP 403/404 for others' sessions). | Must |
| AUTH-7 | A user can request a password reset link by email and set a new password. | Must |
| AUTH-8 | Passwords must be at least 12 characters and are checked against a list of commonly breached passwords. | Must |
| AUTH-9 | Repeated failed sign-ins for one account or from one source are rate limited and temporarily locked. Proposed default: 5 failures within 15 minutes leads to a 15-minute lock. Pending support-team confirmation. | Must |
| AUTH-10 | Escalation requests include the authenticated user's ID and email so an agent can follow up. | Must |
| AUTH-11 | Access sessions expire after about 15 minutes of inactivity and can be renewed with a refresh token without re-entering credentials. | Should |
| AUTH-12 | The UI shows the signed-in email and a sign-out action, and redirects to the sign-in page when the session expires. | Should |
| AUTH-13 | Sign-in, sign-up, and reset error messages do not reveal whether an email is already registered. | Should |
| AUTH-14 | A customer whose verification email was not received, or whose link has expired, can request a new verification email. Each earlier link stops working when a new one is sent. | Must |
| AUTH-15 | A customer can be signed in on more than one browser or device at the same time. Signing out on one device does not end the others. | Should |
| AUTH-16 | A successful password reset ends all existing sessions for that account. | Must |

## 7. Technical Requirements

| Layer | Choice |
|-------|--------|
| Frontend | React + TypeScript: sign-up, sign-in, verify-email, forgot/reset-password pages and a route guard around the chat page |
| Backend | Java + Spring Boot with Spring Security |
| Session tokens | Short-lived access token (about 15 minutes) and rotating refresh token, both delivered as `HttpOnly`, `Secure`, `SameSite=Lax` cookies. Tokens are not stored in `localStorage`. |
| Password storage | BCrypt or Argon2 hashing via Spring Security's `PasswordEncoder`. Plain-text passwords are never stored or logged. |
| User store | A persistent store for users, verification tokens, and reset tokens (database to be chosen, see section 10). The MVP chat session store stays in memory. |
| Email delivery | An SMTP or transactional email provider, configured through environment variables (provider to be chosen, see section 10). |
| CSRF | CSRF protection enabled for cookie-authenticated state-changing requests. |
| Secrets | The token signing key and email provider credentials are read from `.env` by the backend only, following the same rule as `GOOGLE_API_KEY` (constitution Principle II). |

### API outline

| Endpoint | Purpose |
|----------|---------|
| `POST /api/auth/signup` | Create an account and send the verification email |
| `POST /api/auth/verify-email` | Confirm an email address with a one-time token |
| `POST /api/auth/resend-verification` | Send a new verification email and invalidate earlier links (AUTH-14) |
| `POST /api/auth/login` | Sign in and set session cookies |
| `POST /api/auth/refresh` | Rotate the refresh token and issue a new access token |
| `POST /api/auth/logout` | End the session and clear cookies |
| `POST /api/auth/forgot-password` | Send a password reset email |
| `POST /api/auth/reset-password` | Set a new password with a one-time token and end all sessions for the account (AUTH-16) |
| `GET /api/auth/me` | Return the current user (ID and email) |

Existing `/api` chat endpoints stay as they are but require an authenticated session.

## 8. Token and Link Lifetimes

| Item | Lifetime | Single-use |
|------|----------|-----------|
| Access token | About 15 minutes of inactivity | No |
| Refresh token | Rotates on every use. Maximum continuous sign-in length is open (section 10). | Yes, rotated |
| Verification link | 24 hours | Yes |
| Password reset link | 30 minutes | Yes |

## 9. Non-Functional Requirements

- **Security:** Verification and reset tokens are random, stored hashed, single-use, and expire per section 8. All auth traffic must use HTTPS outside local development. Secrets such as the token signing key and SMTP credentials are read from `.env` by the backend only. They must never reach the frontend, be committed, or be logged.
- **Privacy:** Store only email, password hash, verification status, and timestamps. Never log passwords, tokens, session credentials, or full email addresses. Existing rules about personal data in chat logs still apply.
- **Reliability:** If the email provider is down, sign-up, resend, and reset requests return a clear error. They must not fail silently.
- **Performance:** Sign-in completes in under 1 second at the median, excluding intentional hashing cost and any deliberate lockout delay.
- **Usability:** A new customer can go from the landing page to their first chat message in under 3 minutes, including email verification. Proposed target, to be validated with users.
- **Accessibility:** Auth forms are keyboard accessible and have labeled fields and clear error text.

## 10. Success Metrics

- Sign-up completion rate (accounts created that reach verified status). Proposed target: 80% within 24 hours.
- Share of verified users who send at least one chat message.
- Sign-in failure rate and lockout count (abuse signal).
- Number of unauthenticated requests rejected on protected endpoints.
- Password reset completion rate. Proposed target: 80% of reset links used within their lifetime, to be confirmed.
- No change in chat thumbs-up ratio attributable to the sign-in step.

## 11. Assumptions and Dependencies

- A database is available for user records. The MVP currently has no persistent store.
- An email provider and a sender domain are available for verification and reset emails.
- The frontend and backend are served from the same site (or a proxy), so cookie-based sessions work. The dev server proxies `/api` to the backend.
- Customers have separate support-chat accounts. No SSO with Caramel AI platform accounts.
- Multiple concurrent sessions per account are allowed (AUTH-15).

## 12. Open Questions

| Question | Status | Owner / next step |
|----------|--------|-------------------|
| Should customers sign in with existing Caramel AI platform accounts (SSO)? | Resolved for MVP: no. Revisit after launch. | Product |
| Is anonymous chat needed for pre-sales or outage situations? | Resolved for MVP: no. | Product |
| Is MFA required at launch for any customer tier? | Resolved for MVP: no. | Product |
| Lockout thresholds and durations (AUTH-9) | Proposed default. Needs support-team confirmation. | Support team |
| Maximum continuous sign-in length (refresh lifetime) | Open. Must be set before planning. | Product and security |
| Which database and migration tool hold user records? | Open. Planning decision. | Engineering |
| Which email provider and sender domain are used? | Open. Planning decision. | Engineering and ops |
| Retention and deletion rules for user accounts and for chat transcripts linked to a user (GDPR or similar) | Decided, see section 14 (recommended defaults, confirmed by the user). | Product |

## 13. Traceability

- Requirements AUTH-1 to AUTH-13 carry over unchanged in intent from [002-prd-user-auth.md](002-prd-user-auth.md). AUTH-9 now has a proposed default, and AUTH-11 and AUTH-13 are unchanged.
- Requirements AUTH-14 to AUTH-16 are new in this version. In [spec 003-spec-auth](specs/003-spec-auth/spec.md) they are numbered requirements: AUTH-14 is FR-005, AUTH-15 is FR-010, and AUTH-16 is FR-016.
- The constitution's auth TODOs (user store, email provider, retention) map to the open items in section 12.

## 14. Data Retention

Decided 2026-11-21. These rules satisfy constitution Principle V, which requires a retention policy in the constitution or the main PRD before data is stored.

- **Account data** (email, password hash, verification status, timestamps) is deleted once the account has had **no successful sign-in for 30 days**. An account that has never signed in is deleted 30 days after it was created.
- **Authenticated sessions, refresh credentials, and verification and reset tokens** are deleted with their account. Expired ones are removed by the same daily job.
- **Chat transcripts** are held in memory only and expire after 30 minutes of idle time, which is sooner than the 30-day limit. Any future storage of transcripts must delete them within 30 days of the session ending.
- **Escalation tickets** are created in the ticketing system, which has its own retention settings. Those settings are outside this PRD and must be set before launch. Each ticket carries the customer's account ID and email.
- **Customer-requested deletion** is out of MVP scope (section 5).

Maximum continuous sign-in (FR-009) is 30 days from the last password entry. Sessions also end after 15 minutes of inactivity (AUTH-11).

