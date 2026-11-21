# Research: Customer Sign-In for the Support Chat

Each entry gives the decision, its rationale, and the alternatives considered.

## R1. Authentication architecture

**Decision**: Use Spring Security as the single authentication layer for `/api/**`. Each sign-in creates a server-side **authenticated session record** (`auth_session`). Access checks read the signed access credential, then confirm that its session record is still active. Sessions are stateless to Spring (no `HttpSession`). Unauthenticated requests to protected endpoints get HTTP 401 with a ProblemDetail body (code `UNAUTHENTICATED`), using the existing `ApiExceptionHandler` pattern. Authenticated but not permitted gets 403.

**Rationale**: AUTH-4 and AUTH-16 require sign-out and password reset to end access at once. A record checked on each request is the only way to revoke access immediately. Spring Security already supplies the filter chain, CSRF handling, and password encoders, which fits Principle VII.

**Alternatives considered**: Stateless signed tokens with no server record (cannot revoke before expiry, so a signed-out credential would still work for up to 15 minutes; fails AUTH-4). Spring `HttpSession` with a database session store (couples auth to servlet session handling and complicates the cookie rules in R4).

## R2. Password storage and policy

**Decision**: Store passwords with Spring Security's BCrypt encoder at strength 12. Tune the strength so a sign-in stays under the performance target on the target hardware (SC-003). Reject passwords shorter than 12 characters and longer than 72 bytes. The length rule applies to the value as typed, including spaces (spec Edge Cases).

**Rationale**: BCrypt is one of the two algorithms Principle VII allows, and Spring provides it without native libraries. The 72-byte limit is required because BCrypt silently ignores any bytes past 72. Rejecting longer input is safer than truncating it without telling the customer.

**Alternatives considered**: Argon2id (allowed, but needs an extra native-library dependency for the same security level in this context). A higher BCrypt strength (slower sign-in, past the SC-003 target).

## R3. Access and refresh credentials

**Decision**:
- **Access credential**: a short-lived signed token (HMAC-SHA256, key from the environment, at least 256 bits). Claims: account ID, session ID, expiry 15 minutes from issue. It is checked on every request, and the session record must also be active (R1).
- **Refresh credential**: a random 256-bit opaque value. Only its SHA-256 hash is stored, in `refresh_token`, linked to the session. Each use **rotates** it: the old value is marked used and a new one is issued. Using an already-used refresh credential is treated as theft, and the whole session is revoked (reuse detection).
- **Idle timeout (FR-009)**: a session ends after 15 minutes without activity. Activity is any authenticated request. The session record stores `last_activity_at`, which is written at most once a minute to limit database writes. A refresh is refused when the session has been idle for more than 15 minutes.
- **Absolute cap (FR-009)**: `max_expires_at` is 30 days after the last password entry. A refresh is refused after that, and the customer must sign in again.

**Rationale**: The access credential is short-lived, so a leaked copy expires quickly. Rotation with reuse detection means a stolen refresh credential is detected the next time the real customer uses theirs. Storing only hashes means a database leak does not reveal usable credentials. Keeping the idle check on the session record (not on the access credential alone) is what makes FR-009's "expire after about 15 minutes of inactivity" true. Without it, a silent refresh would extend the session indefinitely.

**Interpretation to confirm**: a customer who closes the browser and returns more than 15 minutes later signs in again. This follows FR-009 as written. It is stricter than many products. It is noted in the final report for the user to confirm.

**Alternatives considered**: A 15-minute access credential with a refresh that works for 30 days regardless of activity (silently extends sessions, contrary to FR-009's idle rule). A long-lived single bearer token (cannot rotate, and a leak is valid for its whole life).

## R4. Cookies and CSRF

**Decision**:
- Access and refresh credentials are set as `HttpOnly`, `SameSite=Lax` cookies. They are `Secure` outside the local development profile.
- The access cookie has `Path=/`. The refresh cookie has `Path=/api/auth`, so it is sent only to the auth endpoints.
- CSRF protection uses Spring Security's cookie-based token repository. The token is in a readable cookie (not a session credential) and must be sent back in a header on every state-changing request. This includes sign-in, sign-up, and refresh, because those endpoints also use cookies.
- The frontend and API share one origin in development (through the Vite proxy) and in production, so no cross-origin cookie handling is needed.

**Rationale**: `HttpOnly` keeps credentials out of page scripts, as Principle VII requires. `SameSite=Lax` blocks most cross-site sends. The narrow refresh path limits where the refresh credential is sent. The CSRF header covers requests that `SameSite` alone does not stop.

**Alternatives considered**: Credentials in `localStorage` (forbidden by Principle VII). `SameSite=Strict` (breaks the link from a verification email into the app).

## R5. Transactional email provider

**Decision**: Send email through the provider's HTTP API, using the existing `RestClient`, behind an `EmailSender` port. The default vendor is Postmark. The API token and the verified sender address are read from the environment. The sender domain needs SPF and DKIM records set up before launch. Each send has a 5-second timeout.

**Rationale**: An HTTP API returns explicit success and failure results, which Reliability (PRD 002 §7) needs. Sign-up can then be atomic: the account is saved only if the verification email is accepted. The port keeps the vendor replaceable.

**Atomic sign-up**: the account, its verification token, and the email send happen in one transaction. If the send fails, the transaction rolls back, and the customer gets a clear error and can retry. Rare case: the email is accepted but the commit fails. The customer's link then points to a token that does not exist, so it is rejected, and they can request a new one (AUTH-14).

**Registered address on sign-up (AUTH-13)**: sign-up always returns the same "check your email" response. If the address is already registered, a short notice goes to that address instead of a verification link. The response does not change.

**Alternatives considered**: SMTP through Spring Mail (works with any vendor, but a failed send is harder to report precisely). Sending the email asynchronously (lets sign-up succeed while the email is lost, which fails Reliability).

## R6. Commonly breached passwords (AUTH-8)

**Decision**: A bundled list of common breached passwords, loaded from the classpath and checked offline. Matching is case-insensitive on the typed value. The list source and its license are confirmed during implementation.

**Rationale**: No password leaves the system, which fits Principles II and V. There is no third-party call on the sign-up path, so no extra failure mode.

**Alternatives considered**: The k-anonymity range API from a breach-checking service (sends a hash prefix to a third party on every sign-up, and adds a dependency that can fail). Revisit it if the bundled list proves too small.

## R7. Sign-in lockout and rate limiting (AUTH-9)

**Decision**: Failed sign-ins are counted in memory per account and per client address, in a sliding 15-minute window. Five failures lock that key for 15 minutes (assumption A2, pending support-team confirmation). Keys are a keyed hash of the email address or client address, so the raw values are never kept. Client addresses are taken from the connection, or from a forwarding header only when the proxy is trusted.

**Response**: a locked attempt gets the same generic error text, whether or not the account exists. The text says to wait 15 minutes or reset the password (spec US5), and it is identical for every address. Keying by email hash means a lock also applies to unknown addresses, so the lock reveals nothing.

**Rationale**: In-memory counters fit the single-instance MVP (research.md R9). Hashing keys keeps personal data out of memory dumps and out of logs (Principle V).

**Trade-off**: customers behind a shared address (a company NAT) share the per-source limit. The threshold is configuration, so the support team can change it.

**Alternatives considered**: Database-backed counters (durable across restarts, but adds a write on every failed sign-in with no current need). A separate rate-limiting service (out of proportion for one instance).

## R8. Retention job (FR-023)

**Decision**: A scheduled job runs daily. It deletes accounts with no successful sign-in in the last 30 days. For an account that has never signed in, the 30 days run from account creation. Deleting an account removes its verification and reset tokens, its authenticated sessions, and its refresh credentials, through foreign-key cascades. It also drops the account's in-memory chat sessions. Transcripts are not stored beyond their session anyway (R9).

**Rationale**: The job applies the decided 30-day rule with no manual step. Cascading deletes make the removal complete in one place.

**Open**: the real ticketing system has its own retention rules, which are outside this feature. Escalation tickets carry the account email (R9), so the ticketing retention policy must be set before launch (TODO(ESCALATION_MECHANISM) in the constitution).

**Alternatives considered**: Manual deletion (misses the rule under load and depends on someone remembering). A database-level scheduled event (ties the rule to one database product).

## R9. Chat session ownership and escalation identity (AUTH-6, AUTH-10)

**Decision**:
- Each in-memory `Session` gets an `ownerAccountId`, set when the session is created by a signed-in customer.
- Every session, message, feedback, and escalation operation looks up the session through an ownership check. A session owned by someone else returns 404, not 403, so its existence is not revealed (spec Edge Cases, AUTH-6).
- Escalation tickets gain the account ID and the account's email (AUTH-10). The stand-in ticketing client logs only the reference and reason, so the new fields do not reach logs. This must be kept true, and a test checks it (T-L1).
- Chat sessions stay in memory and keep their 30-minute idle expiry. No chat data is written to the database, so Principle V's retention rule is met without transcript storage.

**Rationale**: Enforcing ownership in the lookup means no endpoint can skip it. Keeping chat in memory limits the number of places personal data is stored.

**Known gap**: no real ticketing system is configured. Escalations still go to a stand-in client, so no human receives them until one is chosen (see `LoggingTicketingClient`).

**Alternatives considered**: Persisting chat sessions in the database (more retention to manage, beyond this feature's scope). Filtering results after the fact (risks forgetting a path, which Principle VII rules out).

## R10. Frontend routing and guards

**Decision**: Add React Router for the auth screens and the chat route. Routes: `/sign-up`, `/sign-in`, `/verify-email`, `/forgot-password`, `/reset-password`, and `/chat`. The route guard calls `GET /api/auth/me` on load. A 401 redirects to `/sign-in` and keeps the intended path. Access and refresh credentials are never read in JavaScript.

The verification and reset pages read the one-time token from the URL and send it to the API at once. They then remove it from the address bar with `history.replaceState`, and they set `Referrer-Policy: no-referrer`, to limit leakage.

**Rationale**: Routes give each auth screen its own URL, which keyboard and screen-reader users need (Principle VI). The guard runs on load, so an expired session is caught before the chat is shown (AUTH-12).

**Alternatives considered**: Conditional rendering with no router (no URLs for each screen, and the verification link cannot land on a page). Storing tokens in state for the guard (forbidden by Principle VII).

## R11. Database and migrations

**Decision**: PostgreSQL (the current supported major version, confirmed at scaffolding). Spring Data JPA for persistence. Flyway migrations under `db/migration`. Emails are stored in lowercase, normalized form, with a unique index. Local development runs PostgreSQL in a container through a compose file. Persistence tests use Testcontainers with PostgreSQL.

**Rationale**: PostgreSQL is a safe, widely supported default for relational account data. Flyway keeps the schema reproducible. Testing against the same database engine avoids the dialect surprises an in-memory substitute would hide.

**Alternatives considered**: H2 for tests (different SQL behavior from production). A document database (the data is relational and the uniqueness rules need transactions).

## Summary of new configuration (names only)

Read by the backend from `.env` or the environment. None goes to the frontend or into logs.

- `AUTH_TOKEN_SIGNING_KEY`: at least 256 bits, base64
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `POSTMARK_SERVER_TOKEN`: email provider credential
- `EMAIL_FROM_ADDRESS`: verified sender address
- `APP_PUBLIC_URL`: base URL used in email links
