package com.caramelai.support.chat.feedback;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * A thumbs up or down on one assistant message. One record per message: changing the rating
 * replaces it. No free text is stored (research.md R8, Principle V).
 */
public record Feedback(UUID messageId, Rating rating, Instant recordedAt) {

  public enum Rating {
    UP,
    DOWN;

    /** Wire form is lower case ("up" / "down"), as in contracts/chat-api.yaml. */
    public String wire() {
      return name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the rating, or null when the text is not "up" or "down"
     */
    public static Rating fromWire(String text) {
      if (text == null) {
        return null;
      }
      return switch (text.trim().toLowerCase(Locale.ROOT)) {
        case "up" -> UP;
        case "down" -> DOWN;
        default -> null;
      };
    }
  }
}
