package com.caramelai.support.chat.api;

import com.caramelai.support.chat.escalation.EscalationRequest;
import com.caramelai.support.chat.escalation.EscalationService;
import com.caramelai.support.chat.session.Session;
import com.caramelai.support.chat.session.SessionStore;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/escalations")
public class EscalationController {

  private final SessionStore sessions;
  private final EscalationService escalations;

  public EscalationController(SessionStore sessions, EscalationService escalations) {
    this.sessions = sessions;
    this.escalations = escalations;
  }

  public record EscalationCreated(String ticketRef, String status) {}

  /** The always-visible "talk to a person" button (FR-005, SC-005): one request, one ticket. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  EscalationCreated create(@PathVariable UUID sessionId) {
    Session session = sessions.get(sessionId);
    EscalationRequest request = escalations.escalate(session, session.messages());
    return new EscalationCreated(request.ticketRef(), "created");
  }
}
