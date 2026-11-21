package com.caramelai.support.chat.escalation;

import com.caramelai.support.chat.escalation.EscalationRequest.TicketPayload;

/**
 * Creates a support ticket for a human agent to follow up on. The real ticketing system is not
 * chosen yet (PRD §9, §10; research.md R7), so this is the seam where it plugs in.
 */
public interface TicketingClient {

  /**
   * @return the ticket reference to show to the customer
   * @throws TicketingUnavailableException when the ticketing system cannot be reached
   */
  String createTicket(TicketPayload payload);

  /** The ticketing system is down or rejected the request. */
  class TicketingUnavailableException extends RuntimeException {
    public TicketingUnavailableException(Throwable cause) {
      super("The ticketing system is unavailable", cause, false, false);
    }
  }
}
