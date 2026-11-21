# Feature Specification: Customer Sign-In for the Support Chat (Version 3)

**Feature Branch**: `003-spec-auth` (no branch created)

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: none provided. Source taken from the most recent product document: [prd-auth-3.md](../../prd-auth-3.md) (AUTH-1 to AUTH-16).

**Relationship to other specs**: This spec is the only auth spec. It replaces the earlier 003-user-auth and 004-user-auth-v3 drafts, which were built from earlier PRDs and have been deleted. The v3 changes are three gaps closed (resend verification, multiple devices, reset ends sessions) and the lockout default. Keep the AUTH identifiers for traceability. Governed by [constitution.md](../../.specify/memory/constitution.md) Principle VII.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create an account and verify email (Priority: P1)

A new customer creates an account with their email address and a password, then confirms the address from the verification email. Only after confirmation can they start a chat. If the email never arrives or the link has expired, they can ask for a new one.

**Why this priority**: Every other story depends on a verified customer identity. Without it, no chat can be tied to a person.

**Independent Test**: Create an account, confirm the address, and check that the chat opens. Then check that the chat stays closed before confirmation, and that a resent email works while the earlier link does not.

**Acceptance Scenarios**:

1. **Given** a visitor with no account, **When** they submit a valid email and a password of at least 12 characters that is not on the commonly breached list, **Then** an unverified account is created and a verification email is sent to that address.
2. **Given** an unverified account, **When** the customer tries to start a chat, **Then** the chat is blocked and the customer is told to verify their email.
3. **Given** a verification email less than 24 hours old, **When** the customer opens its link, **Then** the address is confirmed and the customer can start a chat.
4. **Given** a verification link that has already been used or is more than 24 hours old, **When** the customer opens it, **Then** they see a clear message that the link is no longer valid and can request a new one.
5. **Given** an unverified customer who never received their email or whose link expired, **When** they request a new verification email, **Then** a new link is sent and every earlier link stops working.
6. **Given** a password of 11 characters, or one that appears on the commonly breached list, **When** the customer signs up, **Then** sign-up is refused with a message that names the rule that failed.

---

### User Story 2 - Sign in, sign out, and use more than one device (Priority: P1)

A verified customer signs in, chats, and signs out. While they stay active, the session continues without asking for their password again. A customer may be signed in on several devices at once, and signing out on one does not end the others.

**Why this priority**: Signing in is the gate to chatting. Signing out lets a customer end access on a shared device. Multiple devices is a common expectation for a support tool.

**Independent Test**: Sign in on two browsers, sign out on one, and confirm that the other still works. Then sign out on the second and confirm that neither can be used.

**Acceptance Scenarios**:

1. **Given** a verified account, **When** the customer signs in with the correct email and password, **Then** they reach the chat with their session active.
2. **Given** a signed-in customer, **When** they sign out, **Then** that session ends on the server, so reusing its credentials does not restore access.
3. **Given** a customer signed in on two devices, **When** they sign out on one, **Then** the other device stays signed in.
4. **Given** a signed-in customer who has been active within the last 15 minutes, **When** their short-lived access expires, **Then** the session renews without asking for credentials again.
5. **Given** a signed-in customer who has been idle long enough for the session to expire, **When** they send a chat message, **Then** they are redirected to sign-in with a message that the session has ended.

---

### User Story 3 - Recover a forgotten password (Priority: P2)

A customer who forgot their password requests a reset link by email, sets a new password, and signs in. Setting the new password signs out every device on that account.

**Why this priority**: Without this, a customer who forgets their password is locked out with no way back in. It is required for the MVP but does not block the first sign-in.

**Independent Test**: Sign in on two devices, request a reset, set a new password, and confirm that both devices are signed out and the new password works.

**Acceptance Scenarios**:

1. **Given** a registered email address, **When** the customer requests a password reset, **Then** a reset link is emailed and the response looks the same as for an unregistered address.
2. **Given** a reset link less than 30 minutes old that has not been used, **When** the customer sets a new password that meets the password rules, **Then** the password changes, the link cannot be used again, and every existing session on the account ends.
3. **Given** a reset link more than 30 minutes old or already used, **When** the customer opens it, **Then** they see a clear message that the link is no longer valid and can request another.

---

### User Story 4 - Protected chat, feedback, and escalation (Priority: P1)

Chat, feedback, and escalation work only for signed-in customers. Each customer can see and act only on their own chat sessions. An escalation tells a support agent who the customer is.

**Why this priority**: This is the security boundary for the whole product. It also gives support agents the identity they need to follow up.

**Independent Test**: Try chat, feedback, and escalation with no session, with a session for another customer's chat, and with a valid session. Only the last should succeed, and its escalation should contain the customer's identity.

**Acceptance Scenarios**:

1. **Given** no signed-in session, **When** someone sends a chat message, leaves feedback, or requests escalation, **Then** the request is refused as unauthorized.
2. **Given** customer A's chat session, **When** customer B tries to read or write it, **Then** the request is refused and nothing about A's session is revealed.
3. **Given** a signed-in customer who asks for a human agent, **When** the escalation is sent, **Then** it includes the customer's account ID and email address.

---

### User Story 5 - Protection against repeated sign-in attempts (Priority: P2)

When someone repeatedly fails to sign in to one account, or repeatedly fails from one source, further attempts are blocked for a short period. Legitimate customers see a clear message and can recover.

**Why this priority**: It protects customer accounts and the cost of LLM calls from abuse. It can ship shortly after the core sign-in flow, but it is a Must requirement for the MVP.

**Independent Test**: Make repeated failed sign-in attempts against one account and from one source, and confirm the block takes effect and then lifts.

**Acceptance Scenarios**:

1. **Given** repeated failed sign-ins for one account, **When** the threshold is reached, **Then** sign-in to that account is temporarily blocked and the customer is told to wait or reset their password.
2. **Given** repeated failed sign-ins from one source across different accounts, **When** the threshold is reached, **Then** further sign-ins from that source are temporarily blocked.
3. **Given** a temporary block has run its course, **When** the customer signs in with correct credentials, **Then** sign-in succeeds.

---

### Edge Cases

- A visitor signs up with an email address that already has an account. The response must not reveal that the address is registered (FR-018).
- The email service is unavailable during sign-up, resend, or password reset. The customer sees a clear error and can retry; nothing fails silently (FR-024).
- A customer requests a new verification email. The earlier link stops working even if it has not expired (FR-005).
- A customer resets their password while signed in on several devices. All of those sessions end (FR-016).
- A session expires while the customer is typing a message. They are redirected to sign-in, and the unsent text is not sent without a valid session.
- A signed-in customer opens the chat in two browser tabs. Both tabs use the same session.
- A password is entered with leading or trailing spaces. The length rule applies to the value as typed.

## Requirements *(mandatory)*

### Functional Requirements

**Account creation and verification**

- **FR-001**: A visitor MUST be able to create an account with an email address and a password (AUTH-1).
- **FR-002**: The system MUST send a verification email to the address when an account is created (AUTH-2).
- **FR-003**: An account whose email is not verified MUST NOT be able to start or use a chat (AUTH-2).
- **FR-004**: A verification link MUST be single-use and MUST expire 24 hours after it is sent.
- **FR-005**: An unverified customer MUST be able to request a new verification email. Sending a new one MUST invalidate every earlier verification link for that account (AUTH-14).
- **FR-006**: Passwords MUST be at least 12 characters long and at most 72 bytes, and MUST be rejected if they appear on a list of commonly breached passwords (AUTH-8). The 72-byte limit comes from the password hashing algorithm (research R2).

**Sign-in and sessions**

- **FR-007**: A verified customer MUST be able to sign in with email and password and receive an authenticated session (AUTH-3).
- **FR-008**: A signed-in customer MUST be able to sign out, and signing out MUST end that session on the server so its credentials no longer grant access (AUTH-4).
- **FR-009**: An authenticated session MUST expire after about 15 minutes of inactivity. While the customer remains active, the session MUST renew without requiring their password again (AUTH-11). A customer may stay signed in for at most 30 days without re-entering their password, counted from their last password entry. After that, they must sign in again (decision: recommended default, confirmed by the user).
- **FR-010**: A customer MUST be able to be signed in on several devices at the same time. Signing out on one device MUST NOT end sessions on the others (AUTH-15).
- **FR-011**: Chat, feedback, and escalation requests without a valid session MUST be refused as unauthorized (AUTH-5).
- **FR-012**: A customer MUST only be able to read and write their own chat sessions. Requests for another customer's session MUST be refused without revealing that the session exists (AUTH-6).

**Password reset**

- **FR-013**: A customer MUST be able to request a password reset link by email (AUTH-7).
- **FR-014**: A reset link MUST be single-use and MUST expire 30 minutes after it is sent.
- **FR-015**: A customer MUST be able to set a new password through a valid reset link, and the new password MUST meet the rules in FR-006 (AUTH-7, AUTH-8).
- **FR-016**: A successful password reset MUST end every existing session for that account, on all devices (AUTH-16).

**Abuse protection**

- **FR-017**: Repeated failed sign-in attempts for one account, or from one source, MUST be rate limited and then temporarily locked (AUTH-9). The thresholds are in Assumption A2.
- **FR-018**: Sign-up, sign-in, and password-reset responses MUST NOT reveal whether an email address is registered (AUTH-13).

**Escalation**

- **FR-019**: Every escalation request MUST include the signed-in customer's account ID and email address so that an agent can follow up (AUTH-10).

**Customer interface**

- **FR-020**: The product MUST provide screens for sign-up, sign-in, email verification, requesting a new verification email, and forgotten or reset password. The chat MUST be reachable only through a route guard that sends signed-out visitors to sign-in (prd-auth-3.md section 7).
- **FR-021**: The interface MUST show the signed-in customer's email address and a sign-out action. When a session expires, the customer MUST be redirected to sign-in (AUTH-12).
- **FR-022**: All auth forms MUST be usable with a keyboard, have labeled fields, and show error text that says what to fix (prd-auth-3.md section 9).

**Data, privacy, and reliability**

- **FR-023**: The system MUST store only the customer's email address, a non-reversible form of the password, verification status, and timestamps for account data. Account data MUST be deleted once the account has had no successful sign-in for 30 days (for an account that has never signed in, 30 days after it was created). Chat transcripts MUST be deleted 30 days after their session ends. Transcripts are held in memory only and already expire after 30 minutes of idle time, which is sooner (decision: recommended default, confirmed by the user).
- **FR-024**: If the email service is unavailable, sign-up, resend, and password-reset requests MUST return a clear error that the customer can act on. They MUST NOT fail silently.
- **FR-025**: Passwords, verification and reset tokens, session credentials, and full email addresses MUST NOT appear in logs or other operational output (constitution Principle V).

### Key Entities

- **Customer Account**: A customer's identity for the support chat. Attributes: email address, verification status, password in non-reversible form, created and updated times. One email address maps to one account.
- **Verification Request**: A single-use, time-limited confirmation of an email address. Linked to one account. Expires 24 hours after it is sent. A newer request invalidates earlier ones.
- **Password Reset Request**: A single-use, time-limited request to set a new password. Linked to one account. Expires 30 minutes after it is sent.
- **Authenticated Session**: The signed-in state of one customer on one device. Has a short inactivity window. Can be ended by sign-out, by a password reset, or by expiry.
- **Chat Session**: An existing support conversation, now linked to the customer account that owns it.
- **Escalation Request**: A request for a human agent. Carries the customer's account ID and email address.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new customer can go from the landing page to sending a first chat message in under 3 minutes, including the time to confirm their email address. Proposed target, to be validated with users.
- **SC-002**: 100% of chat, feedback, and escalation requests without a valid signed-in session are refused in testing.
- **SC-003**: Customers who sign in with correct credentials reach the chat within 1 second at the median, not counting any deliberate delay caused by a block.
- **SC-004**: At least 80% of accounts created reach verified status within 24 hours of sign-up. Proposed target.
- **SC-005**: At least 80% of password reset links are used within their 30-minute lifetime. Proposed target.
- **SC-006**: Sign-in, sign-up, and reset messages are identical in content for registered and unregistered addresses in every test case.
- **SC-007**: Every escalation sent from a signed-in session includes the correct account ID and email address in testing.
- **SC-008**: In testing, a password reset signs out every device on the account in 100% of cases, and signing out on one device leaves the others signed in in 100% of cases.

## Assumptions

- **A1 (decided in prd-auth-3.md section 4)**: Customers have separate support-chat accounts. Reuse of Caramel AI platform accounts (SSO) is out of scope for the MVP.
- **A2 (proposed default, pending support-team confirmation)**: Sign-in is blocked for 15 minutes after 5 failed attempts within 15 minutes, counted per account and separately per source.
- **A3 (decided in prd-auth-3.md section 4)**: No multi-factor authentication at launch.
- **A4 (decided in prd-auth-3.md section 4)**: Anonymous chat is not offered. Customers must sign in before chatting.
- **A5 (planning decision deferred)**: The user store, email provider, sender domain, and breached-password list source are chosen in planning. This spec does not depend on them.
- **A6**: The chat session store stays in memory for the MVP, as stated in prd-auth-3.md section 7. Chat sessions are not persisted across restarts.
- **A7**: The chat and auth features are reached through the same site, so a signed-in session works across them.
- **A8**: Account deletion by the customer and profile editing are out of MVP scope. Inactivity for FR-023 is measured from the last successful sign-in, or from account creation if the customer has never signed in. The 30-day retention rule applies to any account data written by this feature.

## Dependencies

- A working email delivery service for verification, resend, and reset emails (FR-002, FR-005, FR-013, FR-024).
- The chat, feedback, and escalation features from [001 spec](../001-support-chat-assistant/spec.md) must require a session (FR-011, FR-012, FR-019).
- Functional requirements from [001-prd.md](../../001-prd.md) stay in force (FR-1 to FR-7).

## Out of Scope

- Social login and enterprise single sign-on.
- Multi-factor authentication.
- Roles beyond a single customer role.
- Linking to Caramel AI billing or API-key accounts.
- Account deletion and profile editing screens.
- Cross-session chat history.
- Account-specific actions such as plan changes or refunds.
