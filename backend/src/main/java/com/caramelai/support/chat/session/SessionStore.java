package com.caramelai.support.chat.session;

import com.caramelai.support.chat.config.ChatProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * In-memory session store. A session expires after the idle timeout (30 minutes by default) and its
 * messages are removed with it. Nothing is persisted (Principle V, FR-011).
 */
@Component
public class SessionStore {

  private static final Logger log = LoggerFactory.getLogger(SessionStore.class);

  private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
  private final ChatProperties properties;
  private final Clock clock;

  public SessionStore(ChatProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public Session create() {
    Session session = new Session(clock.instant());
    sessions.put(session.id(), session);
    return session;
  }

  /** Returns the live session, or throws when it is unknown or past its idle timeout. */
  public Session get(UUID id) {
    Session session = sessions.get(id);
    if (session == null) {
      throw new SessionNotFoundException();
    }
    if (isExpired(session, clock.instant())) {
      remove(session);
      throw new SessionNotFoundException();
    }
    return session;
  }

  public int size() {
    return sessions.size();
  }

  /** Runs once a minute and drops sessions that have been idle for longer than the timeout. */
  @Scheduled(fixedDelayString = "PT1M")
  public void sweepExpired() {
    Instant now = clock.instant();
    sessions.values().stream()
        .filter(s -> isExpired(s, now))
        .forEach(
            s -> {
              remove(s);
              log.info("Session expired sessionId={}", s.id());
            });
  }

  private boolean isExpired(Session session, Instant now) {
    return session.lastActivityAt().plus(properties.sessionIdleTimeout()).isBefore(now);
  }

  private void remove(Session session) {
    session.markExpired();
    sessions.remove(session.id());
  }
}
