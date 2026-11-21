package com.caramelai.support.chat.metrics;

import com.caramelai.support.chat.escalation.EscalationRequest.Reason;
import com.caramelai.support.chat.feedback.Feedback.Rating;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The PRD section 8 success metrics, kept as counts and timings only. Nothing here receives or
 * stores message text (Principle V, SC-006).
 *
 * <ul>
 *   <li>Share of sessions resolved without escalation: {@link #resolvedWithoutEscalationShare()}
 *   <li>Thumbs-up ratio: {@link #thumbsUpRatio()}
 *   <li>Escalation rate by reason: counter {@code chat.escalations}, tagged {@code reason}
 *   <li>Median time from customer message to first token: timer {@code chat.first_token}
 * </ul>
 */
@Component
public class ChatMetrics {

  private static final Logger log = LoggerFactory.getLogger(ChatMetrics.class);

  private final MeterRegistry registry;
  private final Counter sessionsStarted;
  private final Counter sessionsEscalated;
  private final Timer firstToken;
  // Current ratings (a changed rating moves from one count to the other), so the ratio is not
  // skewed by customers who change their mind.
  private final AtomicInteger thumbsUp = new AtomicInteger();
  private final AtomicInteger thumbsDown = new AtomicInteger();

  public ChatMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.sessionsStarted = Counter.builder("chat.sessions.started").register(registry);
    this.sessionsEscalated = Counter.builder("chat.sessions.escalated").register(registry);
    this.firstToken =
        Timer.builder("chat.first_token")
            .description("Customer message to first answer token")
            .publishPercentiles(0.5, 0.95)
            .register(registry);
    registry.gauge("chat.feedback.up", thumbsUp);
    registry.gauge("chat.feedback.down", thumbsDown);
  }

  public void sessionStarted() {
    sessionsStarted.increment();
  }

  /**
   * @param firstEscalationInSession true only the first time this session escalates, so one session
   *     is counted once however many tickets it creates
   */
  public void escalationCreated(Reason reason, boolean firstEscalationInSession) {
    Counter.builder("chat.escalations")
        .tag("reason", reason.name().toLowerCase(Locale.ROOT))
        .register(registry)
        .increment();
    if (firstEscalationInSession) {
      sessionsEscalated.increment();
    }
  }

  public void feedbackRecorded(Optional<Rating> previous, Rating current) {
    previous.ifPresent(p -> (p == Rating.UP ? thumbsUp : thumbsDown).decrementAndGet());
    (current == Rating.UP ? thumbsUp : thumbsDown).incrementAndGet();
  }

  public void firstTokenAfter(long millis) {
    firstToken.record(Duration.ofMillis(millis));
  }

  /** 1 minus escalated sessions over started sessions. NaN until a session has started. */
  public double resolvedWithoutEscalationShare() {
    double started = sessionsStarted.count();
    return started == 0 ? Double.NaN : 1.0 - sessionsEscalated.count() / started;
  }

  /** Up ratings over all ratings. NaN until an answer has been rated. */
  public double thumbsUpRatio() {
    double total = thumbsUp.get() + thumbsDown.get();
    return total == 0 ? Double.NaN : thumbsUp.get() / total;
  }

  public double medianFirstTokenMillis() {
    if (firstToken.count() == 0) {
      return Double.NaN;
    }
    for (var p : firstToken.takeSnapshot().percentileValues()) {
      if (p.percentile() == 0.5) {
        return p.value(java.util.concurrent.TimeUnit.MILLISECONDS);
      }
    }
    return Double.NaN;
  }

  /** Counts and timings only, so this is safe to log. */
  @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT15M")
  void logSummary() {
    log.info(
        "metrics sessionsStarted={} sessionsEscalated={} resolvedWithoutEscalation={}"
            + " thumbsUp={} thumbsDown={} thumbsUpRatio={} medianFirstTokenMs={}",
        (long) sessionsStarted.count(),
        (long) sessionsEscalated.count(),
        resolvedWithoutEscalationShare(),
        thumbsUp.get(),
        thumbsDown.get(),
        thumbsUpRatio(),
        medianFirstTokenMillis());
  }
}
