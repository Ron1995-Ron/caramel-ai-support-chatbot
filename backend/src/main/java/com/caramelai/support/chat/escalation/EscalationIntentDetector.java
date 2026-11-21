package com.caramelai.support.chat.escalation;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Spots a customer asking for a human. A simple phrase check, chosen over a model call because it
 * is instant, free, and works when the AI provider is down (when escalation matters most). It is
 * deliberately narrow: "human-readable output" must not trigger a ticket. The always-visible
 * escalation button covers anything this misses.
 */
@Component
public class EscalationIntentDetector {

  private static final List<Pattern> PATTERNS =
      List.of(
          // "talk to a person", "speak with someone", "connect me to an agent", "transfer me to a
          // human"
          Pattern.compile(
              "\\b(talk|speak|chat|connect|transfer|escalate)\\b.{0,20}\\b(to|with)\\b"
                  + "\\s+(a|an|the|your)?\\s*(real |live |human |actual )?"
                  + "(person|human|agent|someone|somebody|representative|rep|human being|support (team|staff|agent))\\b"),
          // "I want a human agent", "need a real person", "get me a live agent"
          Pattern.compile("\\b(real|live|actual) (person|human|agent|support)\\b"),
          Pattern.compile("\\bhuman (agent|support|being|representative|help)\\b"),
          Pattern.compile(
              "\\b(support|customer service|customer support) (agent|rep|representative|team|staff)\\b"),
          Pattern.compile("\\b(representative|human)\\s*(please|now)\\b"),
          Pattern.compile("\\bescalate (this|my|the)\\b"),
          Pattern.compile("\\bopen (a |another )?(support )?ticket\\b"));

  public boolean wantsHuman(String message) {
    if (message == null) {
      return false;
    }
    String text = message.toLowerCase();
    return PATTERNS.stream().anyMatch(p -> p.matcher(text).find());
  }
}
