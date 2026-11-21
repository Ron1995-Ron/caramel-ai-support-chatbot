package com.caramelai.support.chat.escalation;

import java.time.Instant;
import java.util.UUID;

/** A request for a human agent, raised within a session (data-model.md). */
public record EscalationRequest(
    UUID id,
    UUID sessionId,
    Reason reason,
    String ticketRef,
    TicketPayload ticketPayload,
    Status status,
    Instant createdAt) {

  public enum Reason {
    CUSTOMER_REQUEST,
    ASSISTANT_UNABLE
  }

  public enum Status {
    CREATED,
    FAILED
  }

  /**
   * What is sent to the ticketing system. Only the session reference, the reason, and the
   * customer's latest message. NOT the full transcript, until a retention policy is recorded
   * (plan.md Constitution Check, Principle V).
   */
  public record TicketPayload(UUID sessionId, Reason reason, String latestCustomerMessage) {}
}
