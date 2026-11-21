package com.caramelai.support.chat.session;

import com.caramelai.support.chat.escalation.EscalationRequest;
import com.caramelai.support.chat.feedback.Feedback;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * One anonymous chat conversation, held in memory only (research.md R3). Access to the mutable
 * state is synchronized because a session can be read by the sweep thread while a request streams.
 */
public class Session {

  public enum Status {
    ACTIVE,
    ESCALATED,
    EXPIRED
  }

  private final UUID id = UUID.randomUUID();
  private final Instant createdAt;
  private final List<Message> messages = new ArrayList<>();
  private final List<EscalationRequest> escalations = new ArrayList<>();
  private final Map<UUID, Feedback> feedbackByMessage = new HashMap<>();
  private Instant lastActivityAt;
  private Status status = Status.ACTIVE;

  public Session(Instant now) {
    this.createdAt = now;
    this.lastActivityAt = now;
  }

  public UUID id() {
    return id;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public synchronized Instant lastActivityAt() {
    return lastActivityAt;
  }

  public synchronized Status status() {
    return status;
  }

  /** Sessions stay usable after escalation so the customer can keep asking. */
  public synchronized void markEscalated() {
    if (status == Status.ACTIVE) {
      status = Status.ESCALATED;
    }
  }

  public synchronized void markExpired() {
    status = Status.EXPIRED;
  }

  public synchronized void touch(Instant now) {
    lastActivityAt = now;
  }

  public synchronized void append(Message message, Instant now) {
    messages.add(message);
    lastActivityAt = now;
  }

  /** A copy, in order, so callers cannot modify the session's list. */
  public synchronized List<Message> messages() {
    return List.copyOf(messages);
  }

  /** Replaces any earlier rating for the same message, so there is never more than one. */
  public synchronized void putFeedback(Feedback feedback) {
    feedbackByMessage.put(feedback.messageId(), feedback);
  }

  public synchronized Optional<Feedback> feedbackFor(UUID messageId) {
    return Optional.ofNullable(feedbackByMessage.get(messageId));
  }

  public synchronized void addEscalation(EscalationRequest request) {
    escalations.add(request);
  }

  public synchronized List<EscalationRequest> escalations() {
    return List.copyOf(escalations);
  }
}
