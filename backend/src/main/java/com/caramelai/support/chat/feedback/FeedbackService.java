package com.caramelai.support.chat.feedback;

import com.caramelai.support.chat.api.InvalidRequestException;
import com.caramelai.support.chat.feedback.Feedback.Rating;
import com.caramelai.support.chat.metrics.ChatMetrics;
import com.caramelai.support.chat.session.Message;
import com.caramelai.support.chat.session.Session;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Records a thumbs up or down on an assistant message (FR-007). */
@Service
public class FeedbackService {

  private final Clock clock;
  private final ChatMetrics metrics;

  public FeedbackService(Clock clock, ChatMetrics metrics) {
    this.clock = clock;
    this.metrics = metrics;
  }

  /**
   * Records the rating, or changes it if the message was already rated. The message must be an
   * assistant message in this session.
   *
   * @throws InvalidRequestException (422) when it is not
   */
  public Feedback rate(Session session, UUID messageId, Rating rating) {
    boolean isAssistantMessageInSession =
        session.messages().stream()
            .anyMatch(m -> m.id().equals(messageId) && m.role() == Message.Role.ASSISTANT);
    if (!isAssistantMessageInSession) {
      throw new InvalidRequestException(
          422, "NOT_AN_ASSISTANT_MESSAGE", "Only answers from the assistant can be rated.");
    }
    Optional<Rating> previous = session.feedbackFor(messageId).map(Feedback::rating);
    Feedback feedback = new Feedback(messageId, rating, clock.instant());
    session.putFeedback(feedback);
    metrics.feedbackRecorded(previous, rating);
    return feedback;
  }
}
