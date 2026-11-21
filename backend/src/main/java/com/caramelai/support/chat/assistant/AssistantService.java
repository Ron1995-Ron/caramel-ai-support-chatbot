package com.caramelai.support.chat.assistant;

import com.caramelai.support.chat.config.ChatProperties;
import com.caramelai.support.chat.knowledge.KnowledgeSource;
import com.caramelai.support.chat.session.Message;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Streams the assistant's answer from the model. Business code depends on {@link ChatModel}, not on
 * the Google provider class, so the provider can change without touching this class.
 */
@Service
public class AssistantService {

  private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

  private final ChatModel chatModel;
  private final KnowledgeSource knowledge;
  private final SystemPrompt systemPrompt;
  private final Duration providerTimeout;

  public AssistantService(
      ChatModel chatModel,
      KnowledgeSource knowledge,
      SystemPrompt systemPrompt,
      ChatProperties properties) {
    this.chatModel = chatModel;
    this.knowledge = knowledge;
    this.systemPrompt = systemPrompt;
    this.providerTimeout = properties.providerTimeout();
  }

  /**
   * Streams the answer text. {@code history} holds the earlier completed exchanges to include, in
   * order. The new question is passed separately.
   *
   * <p>Any provider failure, or no output within the provider timeout, ends the stream with an
   * {@link AssistantUnavailableException} (Principle IV, FR-008).
   */
  public Flux<String> stream(String question, List<Message> history) {
    Prompt prompt = buildPrompt(question, history);
    return Flux.defer(() -> chatModel.stream(prompt))
        .mapNotNull(
            response ->
                response.getResult() == null || response.getResult().getOutput() == null
                    ? null
                    : response.getResult().getOutput().getText())
        .filter(text -> !text.isEmpty())
        // Time allowed before the first token only. Later tokens are not individually timed.
        .timeout(Mono.delay(providerTimeout), token -> Mono.never())
        .onErrorMap(
            e -> !(e instanceof AssistantUnavailableException),
            e -> {
              // Log the error type only. Provider messages can echo the prompt or the key.
              log.warn("Provider call failed errorType={}", e.getClass().getSimpleName());
              return new AssistantUnavailableException(e);
            });
  }

  Prompt buildPrompt(String question, List<Message> history) {
    List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
    messages.add(new SystemMessage(systemPrompt.render(knowledge.contextFor(question))));
    for (Message m : history) {
      messages.add(
          m.role() == Message.Role.CUSTOMER
              ? new UserMessage(m.content())
              : new AssistantMessage(m.content()));
    }
    messages.add(new UserMessage(question));
    return new Prompt(messages);
  }
}
