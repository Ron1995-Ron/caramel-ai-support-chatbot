# PRD: User Authentication for the Caramel AI Tech Support Chat Assistant

## 1. Objective

Let customers sign in before chatting with the Caramel AI support assistant, so that each chat session belongs to a verified user. This resolves the open question in `001-prd.md`: "Is authentication required for customers before chatting?"

## 2. Users

- **Customers** of Caramel AI who want to use the support chat. They need a simple, secure way to prove who they are.
- **Support agents** who receive escalations and need to know which customer an escalated chat came from.

## 3. Goals

- Tie each chat session and each escalation to an authenticated customer identity.
- Protect the chat API from anonymous abuse (the LLM calls cost money).
- Keep sign-in quick enough that it does not discourage customers from asking for help.
- Store as little personal data as possible.

## 4. Scope

### In scope (MVP)

- Sign up with email and password.
- Email verification before the first chat.
- Sign in and sign out.
- Password reset by email.
- Authenticated sessions (access token plus refresh) for the web UI.
- Protecting the chat, feedback, and escalation endpoints so they require sign-in.
- Rate limiting and lockout on sign-in attempts.
- Attaching the authenticated user ID to chat sessions and escalation requests.

### Out of scope (MVP)

- Social login (Google, GitHub) and enterprise SSO.
- Multi-factor authentication (planned for a later release).
- Role management beyond a single "customer" role.
- Linking to Caramel AI billing or API-key accounts.
- Account deletion and profile editing screens.
- Cross-session chat history (still out of scope per `001-prd.md`).

## 5. Functional Requirements

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
| AUTH-9 | Repeated failed sign-ins for one account or from one IP are rate limited and temporarily locked. | Must |
| AUTH-10 | Escalation requests include the authenticated user's ID and email so an agent can follow up. | Must |
| AUTH-11 | Access sessions expire after inactivity and can be renewed with a refresh token without re-entering credentials. | Should |
| AUTH-12 | The UI shows the signed-in email and a sign-out action, and redirects to the sign-in page when the session expires. | Should |
| AUTH-13 | Sign-in and sign-up error messages do not reveal whether an email is already registered. | Should |

## 6. Technical Requirements

| Layer | Choice |
|-------|--------|
| Frontend | React + TypeScript: sign-up, sign-in, verify-email, forgot/reset-password pages and a route guard around the chat page |
| Backend | Java + Spring Boot with Spring Security |
| Session tokens | Short-lived access token (about 15 minutes) and rotating refresh token, both delivered as `HttpOnly`, `Secure`, `SameSite=Lax` cookies. Tokens are not stored in `localStorage`. |
| Password storage | BCrypt or Argon2 hashing via Spring Security's `PasswordEncoder`. Plain-text passwords are never stored or logged. |
| User store | A persistent store for users, verification tokens, and reset tokens (database choice to be confirmed, see Open Questions). The MVP chat session store stays in memory. |
| Email delivery | An SMTP or transactional email provider, configured through environment variables. |
| CSRF | CSRF protection enabled for cookie-authenticated state-changing requests. |

### API outline

| Endpoint | Purpose |
|----------|---------|
| `POST /api/auth/signup` | Create an account and send the verification email |
| `POST /api/auth/verify-email` | Confirm an email address with a one-time token |
| `POST /api/auth/login` | Sign in and set session cookies |
| `POST /api/auth/refresh` | Rotate the refresh token and issue a new access token |
| `POST /api/auth/logout` | End the session and clear cookies |
| `POST /api/auth/forgot-password` | Send a password reset email |
| `POST /api/auth/reset-password` | Set a new password with a one-time token |
| `GET /api/auth/me` | Return the current user (ID and email) |

Existing `/api` chat endpoints stay as they are but require an authenticated session.

## 7. Non-Functional Requirements

- **Security:** Verification and reset tokens are random, single-use, stored hashed, and expire (24 hours for verification, 30 minutes for reset). All auth traffic must use HTTPS outside local development. Secrets such as the token signing key and SMTP credentials are read from `.env` by the backend only. They must never reach the frontend, be committed, or be logged. This follows the same rule as `GOOGLE_API_KEY`.
- **Privacy:** Store only email, password hash, verification status, and timestamps. Never log passwords, tokens, or full email addresses. Existing rules about personal data in chat logs still apply.
- **Reliability:** If the email provider is down, sign-up and reset requests return a clear error. They must not fail silently.
- **Performance:** Sign-in completes in under 1 second at the median, excluding intentional hashing cost.
- **Usability:** A new customer can go from the landing page to their first chat message in under 3 minutes, including email verification.
- **Accessibility:** Auth forms are keyboard accessible and have labeled fields and clear error text.

## 8. Success Metrics

- Sign-up completion rate (accounts created that reach verified status).
- Share of verified users who send at least one chat message.
- Sign-in failure rate and lockout count (abuse signal).
- Number of unauthenticated requests rejected on protected endpoints.
- Password reset completion rate.
- No change in chat thumbs-up ratio attributable to the sign-in step.

## 9. Assumptions and Dependencies

- A database is available for user records (the MVP currently has no persistent store).
- An email provider and a sender domain are available for verification and reset emails.
- The frontend and backend are served from the same site (or a proxy), so cookie-based sessions work. The dev server proxies `/api` to the backend.
- Caramel AI has not yet decided whether support customers should reuse existing Caramel AI platform accounts.

## 10. Open Questions

- Should customers sign in with existing Caramel AI platform accounts (SSO) instead of separate support-chat accounts?
- Which database and migration tool should hold user records?
- Which email provider and sender domain will be used?
- Is anonymous chat still needed for pre-sales or outage situations, perhaps with stricter rate limits?
- Is MFA required at launch for any customer tier?
- What are the retention and deletion requirements for user accounts and for chat transcripts linked to a user (GDPR or similar)?
- What lockout thresholds and durations are acceptable to the support team?
