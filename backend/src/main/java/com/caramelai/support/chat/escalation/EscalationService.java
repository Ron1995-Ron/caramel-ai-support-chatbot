package com.caramelai.support.chat.escalation;

import com.caramelai.support.chat.api.ServiceUnavailableException;
import com.caramelai.support.chat.assistant.SystemPrompt;
import com.caramelai.support.chat.escalation.EscalationRequest.Reason;
import com.caramelai.support.chat.escalation.EscalationRequest.Status;
import com.caramelai.support.chat.escalation.EscalationRequest.TicketPayload;
import com.caramelai.support.chat.metrics.ChatMetrics;
import com.caramelai.support.chat.session.Message;
import com.caramelai.support.chat.session.Session;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Creates a support ticket for a session and records the escalation (FR-005). */
@Service
public class EscalationService {

  public static final String TICKETING_UNAVAILABLE_CODE = "TICKETING_UNAVAILABLE";
  public static final String TICKETING_UNAVAILABLE_MESSAGE =
      "We couldn't reach the support team right now. Please try again in a moment.";

  private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

  private final TicketingClient ticketing;
  private final Clock clock;
  private final ChatMetrics metrics;

  public EscalationService(TicketingClient ticketing, Clock clock, ChatMetrics metrics) {
    this.ticketing = ticketing;
    this.clock = clock;
    this.metrics = metrics;
  }

  /**
   * Creates the ticket. The session stays usable afterwards so the customer can keep asking.
   *
   * @param messagesBeforeRequest the session's messages as they were before the customer asked for
   *     a human. Used to tell whether the assistant had just failed or admitted it could not help.
   * @throws ServiceUnavailableException when the ticketing system cannot be reached (FR-008)
   */
  public EscalationRequest escalate(Session session, List<Message> messagesBeforeRequest) {
    Reason reason = reasonFor(messagesBeforeRequest);
    TicketPayload payload = new TicketPayload(session.id(), reason, latestCustomerMessage(session));

    String ticketRef;
    try {
      ticketRef = ticketing.createTicket(payload);
    } catch (RuntimeException e) {
      session.addEscalation(
          new EscalationRequest(
              UUID.randomUUID(),
              session.id(),
              reason,
              null,
              payload,
              Status.FAILED,
              clock.instant()));
      log.warn(
          "Escalation failed sessionId={} errorType={}",
          session.id(),
          e.getClass().getSimpleName());
      throw new ServiceUnavailableException(
          TICKETING_UNAVAILABLE_CODE, TICKETING_UNAVAILABLE_MESSAGE);
    }

    EscalationRequest request =
        new EscalationRequest(
            UUID.randomUUID(),
            session.id(),
            reason,
            ticketRef,
            payload,
            Status.CREATED,
            clock.instant());
    boolean first = session.status() != Session.Status.ESCALATED;
    session.addEscalation(request);
    session.markEscalated();
    metrics.escalationCreated(reason, first);
    log.info(
        "Escalation created sessionId={} reason={} ticketRef={}", session.id(), reason, ticketRef);
    return request;
  }

  /**
   * ASSISTANT_UNABLE when the last thing in the session was an answer that failed or ended by
   * offering a human. Otherwise the customer asked on their own.
   */
  static Reason reasonFor(List<Message> before) {
    if (before.isEmpty()) {
      return Reason.CUSTOMER_REQUEST;
    }
    Message last = before.get(before.size() - 1);
    if (last.role() != Message.Role.ASSISTANT) {
      return Reason.CUSTOMER_REQUEST;
    }
    boolean failed = last.status() == Message.Status.FAILED;
    boolean offered = last.content().contains(SystemPrompt.ESCALATION_OFFER);
    return failed || offered ? Reason.ASSISTANT_UNABLE : Reason.CUSTOMER_REQUEST;
  }

  private static String latestCustomerMessage(Session session) {
    List<Message> messages = session.messages();
    for (int i = messages.size() - 1; i >= 0; i--) {
      if (messages.get(i).role() == Message.Role.CUSTOMER) {
        return messages.get(i).content();
      }
    }
    return "";
  }
}
