package com.caramelai.support.chat.session;

import com.caramelai.support.chat.config.ChatProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Chooses which earlier messages are sent to the model (FR-003). The model is stateless, so each
 * request carries the history. Long sessions are trimmed oldest-first to fit a token budget, so the
 * assistant keeps answering and never loses the most recent exchanges (spec User Story 2).
 *
 * <p>The token count is an estimate (about 4 characters per token). It only needs to be close
 * enough to stay under the model's context limit.
 */
@Component
public class ConversationWindow {

  private static final int CHARS_PER_TOKEN = 4;

  private final int budgetTokens;

  public ConversationWindow(ChatProperties properties) {
    this.budgetTokens = properties.historyTokenBudget();
  }

  /**
   * @param earlier all messages in the session before the new customer message, in order
   * @return the most recent complete exchanges that fit the budget, in order. The list is empty or
   *     starts with a customer message and alternates customer / assistant.
   */
  public List<Message> select(List<Message> earlier) {
    List<Message> usable = completeExchanges(earlier);

    // Walk from the newest exchange backwards, keeping whole exchanges until the budget is used.
    List<Message> kept = new ArrayList<>();
    int used = 0;
    for (int i = usable.size() - 2; i >= 0; i -= 2) {
      Message customer = usable.get(i);
      Message assistant = usable.get(i + 1);
      int cost = estimateTokens(customer) + estimateTokens(assistant);
      if (used + cost > budgetTokens) {
        break;
      }
      used += cost;
      kept.add(0, assistant);
      kept.add(0, customer);
    }
    return kept;
  }

  /**
   * Keeps only customer + assistant pairs where the answer completed. A failed answer, and the
   * customer question it was for, are dropped so the roles still alternate.
   */
  private static List<Message> completeExchanges(List<Message> messages) {
    List<Message> pairs = new ArrayList<>();
    for (int i = 0; i + 1 < messages.size(); i++) {
      Message first = messages.get(i);
      Message second = messages.get(i + 1);
      if (first.role() == Message.Role.CUSTOMER
          && second.role() == Message.Role.ASSISTANT
          && second.status() == Message.Status.COMPLETE) {
        pairs.add(first);
        pairs.add(second);
        i++;
      }
    }
    return pairs;
  }

  private static int estimateTokens(Message message) {
    return Math.max(1, message.content().length() / CHARS_PER_TOKEN);
  }
}
