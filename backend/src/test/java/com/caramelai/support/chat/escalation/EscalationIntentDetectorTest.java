package com.caramelai.support.chat.escalation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EscalationIntentDetectorTest {

  private final EscalationIntentDetector detector = new EscalationIntentDetector();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "I want to talk to a person",
        "Can I speak with someone?",
        "please connect me to a human agent",
        "I need a real person",
        "get me a live agent",
        "Transfer me to your support team",
        "I'd like human support",
        "escalate this please",
        "open a support ticket",
        "Representative please",
        "TALK TO A HUMAN"
      })
  void detectsRequestsForAHuman(String message) {
    assertThat(detector.wantsHuman(message)).as(message).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "How do I get human-readable output from the API?",
        "What does the 429 error mean?",
        "How do I create an API key?",
        "Can the model talk about humans in its answers?",
        "Which agent frameworks work with your API?",
        "Does streaming work with the support SDK?"
      })
  void ignoresOrdinaryQuestions(String message) {
    assertThat(detector.wantsHuman(message)).as(message).isFalse();
  }

  @Test
  void handlesNull() {
    assertThat(detector.wantsHuman(null)).isFalse();
  }
}
