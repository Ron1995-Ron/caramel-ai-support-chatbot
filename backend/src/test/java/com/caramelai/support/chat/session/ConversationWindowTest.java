package com.caramelai.support.chat.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.caramelai.support.chat.config.ChatProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationWindowTest {

  private static final UUID SESSION = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-11-14T00:00:00Z");

  private static ConversationWindow windowWithBudget(int tokens) {
    return new ConversationWindow(
        new ChatProperties(4000, Duration.ofMinutes(30), tokens, Duration.ofSeconds(30), "", ""));
  }

  private static Message customer(String text) {
    return new Message(
        UUID.randomUUID(), SESSION, Message.Role.CUSTOMER, text, Message.Status.COMPLETE, NOW);
  }

  private static Message assistant(String text, Message.Status status) {
    return new Message(UUID.randomUUID(), SESSION, Message.Role.ASSISTANT, text, status, NOW);
  }

  @Test
  void keepsAllExchangesWhenTheyFitTheBudget() {
    List<Message> history =
        List.of(
            customer("q1"),
            assistant("a1", Message.Status.COMPLETE),
            customer("q2"),
            assistant("a2", Message.Status.COMPLETE));

    assertThat(windowWithBudget(1000).select(history))
        .extracting(Message::content)
        .containsExactly("q1", "a1", "q2", "a2");
  }

  @Test
  void dropsOldestExchangesFirstAndKeepsTheNewest() {
    String filler = "x".repeat(400); // about 100 tokens each
    List<Message> history = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      history.add(customer("q" + i + filler));
      history.add(assistant("a" + i + filler, Message.Status.COMPLETE));
    }

    // Each exchange costs about 200 tokens, so a 450-token budget keeps the last two.
    List<Message> selected = windowWithBudget(450).select(history);

    assertThat(selected).hasSize(4);
    assertThat(selected.get(0).content()).startsWith("q4");
    assertThat(selected.get(3).content()).startsWith("a5");
    assertThat(selected.get(0).role()).isEqualTo(Message.Role.CUSTOMER);
  }

  @Test
  void dropsAFailedAnswerAndItsQuestionSoRolesStillAlternate() {
    List<Message> history =
        List.of(
            customer("q1"),
            assistant("a1", Message.Status.COMPLETE),
            customer("q2-failed"),
            assistant("partial", Message.Status.FAILED),
            customer("q3"),
            assistant("a3", Message.Status.COMPLETE));

    assertThat(windowWithBudget(1000).select(history))
        .extracting(Message::content)
        .containsExactly("q1", "a1", "q3", "a3");
  }

  @Test
  void returnsNothingForAnEmptyHistory() {
    assertThat(windowWithBudget(1000).select(List.of())).isEmpty();
  }
}
