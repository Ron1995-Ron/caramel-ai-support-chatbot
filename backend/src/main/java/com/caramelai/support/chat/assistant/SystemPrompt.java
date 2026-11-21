package com.caramelai.support.chat.assistant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The Caramel AI support persona and its rules (Principle III). The text lives in
 * prompts/support-system-prompt.txt so it can be reviewed without reading code.
 */
@Component
public class SystemPrompt {

  /**
   * The exact sentence the assistant ends with when it cannot help. The escalation service looks
   * for it to tell "assistant could not resolve" apart from "customer asked for a human".
   */
  public static final String ESCALATION_OFFER =
      "Would you like me to connect you with a human support agent?";

  private static final String CONTENT_PLACEHOLDER = "{{SUPPORT_CONTENT}}";
  private static final String OFFER_PLACEHOLDER = "{{ESCALATION_OFFER}}";

  private final String template;

  public SystemPrompt() {
    try {
      this.template =
          new ClassPathResource("prompts/support-system-prompt.txt")
              .getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the system prompt file", e);
    }
    for (String placeholder : new String[] {CONTENT_PLACEHOLDER, OFFER_PLACEHOLDER}) {
      if (!template.contains(placeholder)) {
        throw new IllegalStateException("System prompt is missing the " + placeholder + " marker");
      }
    }
  }

  public String render(String supportContent) {
    return template
        .replace(OFFER_PLACEHOLDER, ESCALATION_OFFER)
        .replace(CONTENT_PLACEHOLDER, supportContent);
  }
}
