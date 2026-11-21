package com.caramelai.support.chat.knowledge;

import com.caramelai.support.chat.config.ChatProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/** Loads one support-content file at startup and returns it for every question. */
@Component
public class StaticPromptKnowledgeSource implements KnowledgeSource {

  private final String content;

  public StaticPromptKnowledgeSource(ResourceLoader loader, ChatProperties properties) {
    Resource resource = loader.getResource(properties.supportContentLocation());
    try {
      this.content = resource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Cannot read the support content file at " + properties.supportContentLocation(), e);
    }
  }

  @Override
  public String contextFor(String question) {
    return content;
  }
}
