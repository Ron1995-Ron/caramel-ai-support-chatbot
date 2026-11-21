package com.caramelai.support.chat.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.caramelai.support.chat.config.ChatProperties;
import com.caramelai.support.chat.knowledge.KnowledgeSource;
import com.caramelai.support.chat.session.Message;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class AssistantServiceTest {

  private static final UUID SESSION = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-11-14T00:00:00Z");

  private final ChatModel chatModel = mock(ChatModel.class);
  private final KnowledgeSource knowledge = question -> "SUPPORT-CONTENT-MARKER";
  private final AssistantService service =
      new AssistantService(
          chatModel,
          knowledge,
          new SystemPrompt(),
          new ChatProperties(4000, Duration.ofMinutes(30), 6000, Duration.ofMillis(300), "", ""));

  private static ChatResponse chunk(String text) {
    return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
  }

  private static Message earlier(Message.Role role, String text) {
    return new Message(UUID.randomUUID(), SESSION, role, text, Message.Status.COMPLETE, NOW);
  }

  @Test
  void sendsSupportContentEarlierExchangesAndTheNewQuestionToTheModel() {
    when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.just(chunk("ok")));
    List<Message> history =
        List.of(
            earlier(Message.Role.CUSTOMER, "What are the rate limits?"),
            earlier(Message.Role.ASSISTANT, "I do not have that information."));

    StepVerifier.create(service.stream("And for the second tier?", history))
        .expectNext("ok")
        .verifyComplete();

    ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
    org.mockito.Mockito.verify(chatModel).stream(captor.capture());
    var messages = captor.getValue().getInstructions();

    assertThat(messages).hasSize(4);
    assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
    assertThat(messages.get(0).getText()).contains("SUPPORT-CONTENT-MARKER");
    assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
    assertThat(messages.get(1).getText()).isEqualTo("What are the rate limits?");
    assertThat(messages.get(2)).isInstanceOf(AssistantMessage.class);
    assertThat(messages.get(3)).isInstanceOf(UserMessage.class);
    assertThat(messages.get(3).getText()).isEqualTo("And for the second tier?");
  }

  @Test
  void streamsEachChunkAsItArrivesAndSkipsEmptyOnes() {
    when(chatModel.stream(any(Prompt.class)))
        .thenReturn(Flux.just(chunk("To set up "), chunk(""), chunk("an API key...")));

    StepVerifier.create(service.stream("How do I start?", List.of()))
        .expectNext("To set up ", "an API key...")
        .verifyComplete();
  }

  @Test
  void turnsAProviderFailureIntoACustomerSafeError() {
    when(chatModel.stream(any(Prompt.class)))
        .thenReturn(Flux.error(new RuntimeException("401 key=SECRET-VALUE")));

    StepVerifier.create(service.stream("Hi", List.of()))
        .expectErrorSatisfies(
            e -> {
              assertThat(e).isInstanceOf(AssistantUnavailableException.class);
              assertThat(e.getMessage()).isEqualTo(AssistantUnavailableException.CUSTOMER_MESSAGE);
              assertThat(e.getMessage()).doesNotContain("SECRET-VALUE");
            })
        .verify();
  }

  @Test
  void failsWithACustomerSafeErrorWhenNothingArrivesInTime() {
    when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.never());

    assertThatThrownBy(() -> service.stream("Hi", List.of()).blockLast(Duration.ofSeconds(5)))
        .isInstanceOf(AssistantUnavailableException.class);
  }
}
