package com.caramelai.support.chat.escalation;

import com.caramelai.support.chat.escalation.EscalationRequest.TicketPayload;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * STAND-IN ticketing client. It creates no real ticket: it makes up a reference and logs it.
 * Replace it with a client for the real ticketing system once one is chosen (research.md R7). Until
 * then, no human actually receives these escalations.
 *
 * <p>It logs the reference and reason only. The customer's message is never logged (Principle V).
 */
@Component
public class LoggingTicketingClient implements TicketingClient {

  private static final Logger log = LoggerFactory.getLogger(LoggingTicketingClient.class);

  public LoggingTicketingClient() {
    log.warn(
        "No real ticketing system is configured. Escalations create STAND-IN tickets that no"
            + " human will receive. Provide a TicketingClient for the real system before launch.");
  }

  @Override
  public String createTicket(TicketPayload payload) {
    String ref = "CARAMEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    log.warn(
        "STAND-IN ticket created (no real ticketing system configured) ticketRef={} reason={}",
        ref,
        payload.reason());
    return ref;
  }
}
