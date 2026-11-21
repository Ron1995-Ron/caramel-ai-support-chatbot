package com.caramelai.support.chat.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.caramelai.support.chat.escalation.EscalationRequest.Reason;
import com.caramelai.support.chat.feedback.Feedback.Rating;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final ChatMetrics metrics = new ChatMetrics(registry);

  @Test
  void ratiosAreNaNBeforeAnyData() {
    assertThat(metrics.resolvedWithoutEscalationShare()).isNaN();
    assertThat(metrics.thumbsUpRatio()).isNaN();
    assertThat(metrics.medianFirstTokenMillis()).isNaN();
  }

  @Test
  void aSessionIsCountedAsEscalatedOnlyOnce() {
    for (int i = 0; i < 4; i++) {
      metrics.sessionStarted();
    }
    metrics.escalationCreated(Reason.CUSTOMER_REQUEST, true);
    metrics.escalationCreated(Reason.ASSISTANT_UNABLE, false);

    assertThat(metrics.resolvedWithoutEscalationShare()).isEqualTo(0.75);
    assertThat(registry.get("chat.escalations").tag("reason", "customer_request").counter().count())
        .isEqualTo(1);
    assertThat(registry.get("chat.escalations").tag("reason", "assistant_unable").counter().count())
        .isEqualTo(1);
  }

  @Test
  void changingARatingMovesItInsteadOfCountingTwice() {
    metrics.feedbackRecorded(Optional.empty(), Rating.UP);
    metrics.feedbackRecorded(Optional.empty(), Rating.UP);
    metrics.feedbackRecorded(Optional.of(Rating.UP), Rating.DOWN);

    assertThat(metrics.thumbsUpRatio()).isEqualTo(0.5);
  }

  @Test
  void recordsMedianFirstTokenTime() {
    metrics.firstTokenAfter(100);
    metrics.firstTokenAfter(200);
    metrics.firstTokenAfter(900);

    assertThat(metrics.medianFirstTokenMillis()).isBetween(150.0, 250.0);
  }
}
