package com.caramelai.support.chat.api;

import com.caramelai.support.chat.metrics.ChatMetrics;
import com.caramelai.support.chat.session.Session;
import com.caramelai.support.chat.session.SessionStore;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

  private static final Logger log = LoggerFactory.getLogger(SessionController.class);

  private final SessionStore sessions;
  private final ChatMetrics metrics;

  public SessionController(SessionStore sessions, ChatMetrics metrics) {
    this.sessions = sessions;
    this.metrics = metrics;
  }

  public record SessionCreated(UUID sessionId) {}

  /** Starts a new anonymous chat session (FR-001). */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  SessionCreated create() {
    Session session = sessions.create();
    metrics.sessionStarted();
    log.info("Session created sessionId={}", session.id());
    return new SessionCreated(session.id());
  }
}
