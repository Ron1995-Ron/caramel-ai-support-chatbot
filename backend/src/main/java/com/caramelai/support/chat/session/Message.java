package com.caramelai.support.chat.session;

import java.time.Instant;
import java.util.UUID;

/**
 * One customer message or one assistant response (data-model.md). The role is always set by the
 * server, never taken from the client.
 */
public record Message(
    UUID id, UUID sessionId, Role role, String content, Status status, Instant createdAt) {

  public enum Role {
    CUSTOMER,
    ASSISTANT
  }

  public enum Status {
    COMPLETE,
    FAILED
  }

  /**
   * Customer content must be 1 to 4,000 characters and not blank. The API layer checks this first
   * to return the right HTTP status. This factory is the last line of defence.
   */
  public static Message customer(UUID sessionId, String content, int maxLength, Instant now) {
    if (content == null || content.isBlank()) {
      throw new MessageBlankException();
    }
    if (content.length() > maxLength) {
      throw new MessageTooLongException(maxLength);
    }
    return new Message(UUID.randomUUID(), sessionId, Role.CUSTOMER, content, Status.COMPLETE, now);
  }

  public static Message assistant(
      UUID id, UUID sessionId, String content, Status status, Instant now) {
    return new Message(id, sessionId, Role.ASSISTANT, content, status, now);
  }
}
