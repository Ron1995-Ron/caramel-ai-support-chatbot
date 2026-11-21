package com.caramelai.support.chat.api;

import com.caramelai.support.chat.feedback.Feedback.Rating;
import com.caramelai.support.chat.feedback.FeedbackService;
import com.caramelai.support.chat.session.Session;
import com.caramelai.support.chat.session.SessionStore;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/messages/{messageId}/feedback")
public class FeedbackController {

  private final SessionStore sessions;
  private final FeedbackService feedback;

  public FeedbackController(SessionStore sessions, FeedbackService feedback) {
    this.sessions = sessions;
    this.feedback = feedback;
  }

  public record FeedbackRequest(String rating) {}

  /** Records or changes the rating for one assistant message. Safe to call again (idempotent). */
  @PutMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void rate(
      @PathVariable UUID sessionId,
      @PathVariable UUID messageId,
      @RequestBody FeedbackRequest request) {
    Session session = sessions.get(sessionId);
    Rating rating = Rating.fromWire(request == null ? null : request.rating());
    if (rating == null) {
      throw new InvalidRequestException(
          422, "INVALID_RATING", "The rating must be \"up\" or \"down\".");
    }
    feedback.rate(session, messageId, rating);
  }
}
