# Quickstart: Customer Sign-In Validation

This guide runs the feature end to end and checks each requirement. It assumes spec 001 runs as described in CLAUDE.md. Implementation details are in tasks.md, not here.

## Prerequisites

- Java 21, Maven, Node.js, and a container runtime (for local PostgreSQL)
- A project-root `.env` with `GOOGLE_API_KEY` (existing), plus the new names from research.md (Summary of new configuration). Use values for local testing only. Do not commit `.env`.
- A Postmark sandbox server token, or a local mail capture, for verification and reset emails
- A verified sender address for the email provider (for local testing, the sandbox address is enough)

## Setup

1. Start PostgreSQL locally. The compose file starts only the database, not the app:
   ```text
   docker compose -f backend/compose.yaml up -d postgres
   ```
2. Start the backend from `backend/` (port 8083). Flyway applies the migrations at startup:
   ```text
   mvn spring-boot:run
   ```
3. Start the frontend from `frontend/` (port 5176):
   ```text
   npm run dev
   ```
4. Open `http://localhost:5176/sign-up`.

## Scenarios

Each scenario lists the requirement it checks and the expected outcome. Record pass or fail for each, as spec 001 did.

**S1. Sign up and verify (US1, AUTH-1, AUTH-2)**
1. Sign up with a new email and a 12-character password that is not on the breached list.
2. Expected: "Check your email" appears. The email arrives with a verification link.
3. Open `/chat` before verifying. Expected: redirected to sign-in. Sign in with the unverified account. Expected: "verify your email" message, and chat is blocked.
4. Open the verification link. Expected: confirmed. Sign in, and the chat opens.

**S2. Expired, used, and superseded links (FR-004, FR-005)**
1. Open the same verification link a second time. Expected: "link no longer valid" message (410).
2. Request a new verification email, then open the first link. Expected: rejected, because the new email superseded it.

**S3. Password rules (FR-006, AUTH-8)**
1. Sign up with an 11-character password. Expected: 422 with `PASSWORD_TOO_SHORT`.
2. Sign up with a password from the breached list. Expected: 422 with `PASSWORD_BREACHED`.
3. Sign up with a 73-byte password. Expected: 422 with `PASSWORD_TOO_LONG`.

**S4. Sign in, chat, sign out (US2, AUTH-3, AUTH-4)**
1. Sign in with the verified account. Send a chat message. Expected: streamed answer, as in spec 001.
2. Sign out. Send a chat request. Expected: 401 `UNAUTHENTICATED`.
3. Replay the previous access cookie in a new request (for example with `curl`). Expected: 401. Sign-out revokes the session on the server.

**S5. Multiple devices (AUTH-15)**
1. Sign in on two browsers. Sign out on one. Expected: the other still chats.

**S6. Idle timeout and renewal (FR-009, AUTH-11, AUTH-12)**
1. Stay active. Expected: the session renews silently while the tab is active.
2. Leave the tab idle for more than 15 minutes, then send a message. Expected: redirected to sign-in, with the message that the session has ended.
3. Test profile only: the idle timeout can be shortened with the auth idle-timeout setting, so this check runs in a few seconds, not 15 minutes.

**S7. Cap at 30 days (FR-009)**
1. Test profile only: set the absolute cap to a short value. Expected: a refresh after the cap is refused, and the customer must sign in again.

**S8. Ownership (AUTH-6)**
1. Sign in as customer A, create a chat session, and note its ID.
2. Sign in as customer B and request A's session by ID (messages, feedback, escalation). Expected: 404 `SESSION_NOT_FOUND` for every request. The response is the same as for a session that does not exist.

**S9. Password reset (AUTH-7, AUTH-16)**
1. Sign in on two browsers. Request a password reset.
2. Open the reset link and set a new password. Expected: both browsers are signed out on their next request (401).
3. Sign in with the new password. Expected: success. Sign in with the old password. Expected: 401.
4. Reuse the reset link. Expected: 410.

**S10. Enumeration (AUTH-13)**
1. Sign up with a registered email. Request a reset for an unregistered email. Compare the responses for each request. Expected: identical responses, and the registered owner receives a notice.

**S11. Lockout (AUTH-9)**
1. Make five wrong-password sign-in attempts for one account. Expected: the sixth attempt gets 429 with the generic text, and the correct password is also refused for 15 minutes.
2. Make five wrong-password attempts from one address, each for a different account (including an unregistered address). Expected: further sign-ins from that address are locked, across accounts.
3. Wait 15 minutes, or restart the backend (counters are in memory). Sign in correctly. Expected: success.

**S12. Email outage (FR-020, FR-024)**
1. Set an invalid email provider token, then sign up. Expected: a clear "could not send email, try again" error (503 `EMAIL_UNAVAILABLE`). No account row exists afterwards: check the `account` table.
2. Restore the token. Sign up again. Expected: success.

**S13. Escalation identity (AUTH-10)**
1. As a signed-in verified customer, ask for a human. Expected: the confirmation message with a ticket reference.
2. Check the backend log for that request. Expected: neither the account ID nor the email appears in the log (Principle V).

**S14. No secrets or personal data in logs (Principle V, T-L1)**
1. Run S1 to S13, then search the backend log for the test password, the test email, a token value from an email, and a cookie value. Expected: none found.
2. The automated log-scan test (T-L1) runs in `mvn test` and does the same check.

**S15. Retention (FR-023, R8)**
1. Test profile only: set the inactivity window to a few minutes and run the retention job. Expected: the inactive account, its tokens, its sessions, and its in-memory chat sessions are removed. An active account is kept.

**S16. Keyboard and accessibility (Principle VI)**
1. Complete sign-up, sign-in, verify, forgot, and reset using the keyboard only. Expected: every field and button is reachable, has a visible focus ring, and has a label.
2. Trigger an error on each form. Expected: the message is announced through a live region.
3. Check the contrast of any new colors against the design tokens. Expected: WCAG 2.1 AA.

**S17. Chat unchanged (Principles IV and VI)**
1. Run spec 001's quickstart steps 1 to 3 and 7 while signed in. Expected: unchanged behavior, including first token timing.

## Expected Results Summary

- All 17 scenarios pass, or each failure is recorded with the output.
- `mvn test` passes, including the Testcontainers persistence tests and the log-scan test.
- `mvn spotless:check`, `npm run lint`, `npm run format:check`, and `npm run build` pass.
- The gates in plan.md (T-G1, T-G2) are closed before this run is treated as release-ready.
