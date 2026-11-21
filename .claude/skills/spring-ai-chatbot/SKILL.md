---
name: spring-ai-chatbot
description: Build Java chatbots with Spring AI 2.0.2 and Spring Boot. Use when implementing AI chat endpoints, calling an LLM through ChatModel or StreamingChatModel, building multi-turn Prompts from SystemMessage/UserMessage/AssistantMessage, configuring or switching LLM providers (OpenAI, Anthropic, Ollama, Bedrock, Google AI Studio, OpenRouter, Mistral), setting default or per-request ChatOptions, handling API keys, or streaming responses (Flux, SSE) from a Spring Boot backend.
---

# Spring AI chatbot (v2.0.2)

Guidance based on the Spring AI Chat Model API reference:
https://docs.spring.io/spring-ai/reference/api/chatmodel.html

Items marked **(verify)** are not on that page. Check them against the provider's own page under `docs.spring.io/spring-ai/reference/api/chat/` before relying on them, in particular starter artifact names and property keys.

## Core types

```
ChatModel          extends Model<Prompt, ChatResponse>, StreamingChatModel
  String       call(String message)      // convenience, default method
  ChatResponse call(Prompt prompt)

StreamingChatModel extends StreamingModel<Prompt, ChatResponse>
  Flux<String>       stream(String message)   // default method
  Flux<ChatResponse> stream(Prompt prompt)
```

- `Prompt` wraps a `List<Message>` plus optional `ChatOptions`.
- `ChatResponse` holds `getResults()` (a `List<Generation>`) and `getMetadata()`.
- `Generation` holds `getOutput()` (an `AssistantMessage`) and `getMetadata()`.
- `Message` has `getText()`, `getMetadata()` and `getMessageType()`. The type is the conversational role.
- A model without distinct roles treats `UserMessage` as the standard category.

Prefer `call(Prompt)` / `stream(Prompt)` in real applications. `call(String)` is for quick calls.

## Synchronous completion

```java
@Service
class ChatService {

    private final ChatModel chatModel;

    ChatService(ChatModel chatModel) {          // constructor injection
        this.chatModel = chatModel;
    }

    String answer(String question) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage("You are the Caramel AI support assistant. If unsure, say so and offer escalation."),
                new UserMessage(question)));

        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();   // first Generation
    }
}
```

`getResult()` returns the first `Generation`. Use `getResults()` if the provider returns several. Read `response.getMetadata()` for usage and model details.

## Streaming

```java
@RestController
class ChatController {

    private final ChatModel chatModel;   // ChatModel is also a StreamingChatModel

    ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping(path = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> stream(@RequestBody ChatRequest request) {
        Prompt prompt = new Prompt(new UserMessage(request.message()));
        return chatModel.stream(prompt)
                .mapNotNull(r -> r.getResult() == null ? null : r.getResult().getOutput().getText())
                .onErrorResume(e -> Flux.just("[error] The assistant is temporarily unavailable."));
    }
}
```

- `Flux<ChatResponse>` gives metadata per chunk. `Flux<String>` (`stream(String)`) gives text only.
- Chunks can carry null text or a null result, so guard against nulls.
- Convert provider failures into a visible error for the client. Do not fail silently. Log the error type, never the prompt or the key.
- Use WebFlux, or Spring MVC with `Flux` return and SSE. Make sure the servlet container does not buffer the response.

## Multi-turn conversations

The model is stateless. Build the full message list each call and keep history in your own session store, keyed by session ID.

```java
List<Message> messages = new ArrayList<>();
messages.add(new SystemMessage(SYSTEM_PROMPT));
messages.addAll(history);                       // earlier UserMessage / AssistantMessage turns
messages.add(new UserMessage(userText));

ChatResponse response = chatModel.call(new Prompt(messages));
String reply = response.getResult().getOutput().getText();

history.add(new UserMessage(userText));
history.add(new AssistantMessage(reply));
```

Bound the history (turn count or token estimate) so long sessions do not exceed the context window. For a streamed reply, append the `AssistantMessage` after the stream completes, for example with `doOnComplete` over the collected text.

## Configuration and options

Two levels:

1. **Startup defaults** in `application.yml` / `application.properties`. They initialize the `ChatModel`.
2. **Per-request options** on the `Prompt`.

Per-request rules from the reference:

- With `ChatModel.call()` / `stream()`, the `ChatOptions` in a `Prompt` must be a complete set. They fully take precedence over the startup options. They are not merged.
- Pass `null` options in the `Prompt` to fall back to the startup defaults.
- `ChatClient` is incremental. You supply a delta customizer, and only the fields you set override the defaults.

Portable `ChatOptions`: `model`, `frequencyPenalty`, `maxTokens`, `presencePenalty`, `stopSequences`, `temperature`, `topK`, `topP`. `mutate()` returns a builder copy. Each provider also has its own options class with extras (for OpenAI: `logitBias`, `seed`, `user`).

```java
// Override per request. Build from the provider's options class and set every field you need.
Prompt prompt = new Prompt(messages, OpenAiChatOptions.builder()
        .model("gpt-4o-mini")
        .temperature(0.2)
        .maxTokens(512)
        .build());
```

```yaml
# Startup defaults. Property keys differ by provider (verify).
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.3
```

Use the portable `ChatOptions` type in business code where possible. Use the provider options class only where you need a provider-specific field.

## Switching providers

Business code depends on `ChatModel` / `StreamingChatModel`, never on a provider class. To switch provider:

1. Replace the starter dependency in the build file.
2. Replace the provider's `spring.ai.<provider>.*` properties.
3. Leave services and controllers unchanged.

Do not put more than one chat starter on the classpath without choosing explicitly. Several `ChatModel` beans make plain injection ambiguous. Use `@Qualifier` or the `spring.ai.model.chat` selector property (verify).

Providers documented on the Chat Model API page: OpenAI, Anthropic, Ollama, Amazon Bedrock, Mistral AI, and Qxotic jinfer (in-JVM). The navigation also lists Bedrock Converse, Azure OpenAI, DeepSeek, Docker Model Runner, Google GenAI, Groq, MiniMax, NVIDIA, Perplexity AI and OCI Generative AI. See `chat/comparison.html` for a feature comparison.

Starter names and notes (all **verify** against the provider page for 2.0.2):

| Provider | Starter artifact | Notes |
|---|---|---|
| OpenAI | `spring-ai-starter-model-openai` | `spring.ai.openai.api-key` |
| Anthropic | `spring-ai-starter-model-anthropic` | `spring.ai.anthropic.api-key`. See the Anthropic migration note in the upgrade docs. |
| Ollama | `spring-ai-starter-model-ollama` | `spring.ai.ollama.base-url`, no key, local |
| Amazon Bedrock | `spring-ai-starter-model-bedrock-converse` | AWS credentials, not an API key |
| Mistral AI | `spring-ai-starter-model-mistral-ai` | `spring.ai.mistralai.api-key` |
| Google AI Studio | `spring-ai-starter-model-google-genai` | API-key mode targets AI Studio. Vertex mode uses project and location. |
| OpenRouter | OpenAI starter | Point `spring.ai.openai.base-url` at OpenRouter and use its key and model IDs. |

Take the version from the Spring AI BOM (`spring-ai-bom` 2.0.2). Do not pin versions per starter.

## Guardrails

- **Inject, don't construct.** Take `ChatModel` (or `ChatClient.Builder`) by constructor injection. Do not build provider models by hand in business code, and do not use field injection.
- **Secrets.** Read keys from environment variables (`${GOOGLE_API_KEY}` style placeholders) or a git-ignored `.env`. Never hard-code a key, commit it, return it to a frontend, or log it. Keep `.env` in `.gitignore`. Disable request/response body logging for the HTTP client in production, since prompts may hold personal data.
- **Logs.** Log session IDs, model, latency and token counts. Do not log message text.
- **Errors.** Catch provider failures (timeouts, 401/429/5xx) at the controller boundary. Return a clear message and an escalation path. Set timeouts and retry limits.
- **No invented facts.** Put grounding rules in the `SystemMessage`: do not guess pricing, product behavior or API details, say when unsure, and offer a human handoff.
- **Test without a live model.** Mock `ChatModel` in unit tests. For streaming, return `Flux.just(...)` of hand-built `ChatResponse` objects and verify with `StepVerifier`. Keep any live-model test opt-in.
- **Avoid outdated patterns (verify against the 2.0.2 docs):**
  - Old starter names such as `spring-ai-openai-spring-boot-starter`. The `spring-ai-starter-model-*` names replaced them.
  - Pre-1.0 function-calling APIs (`.functions(...)`). Use the current tool-calling API.
  - Manually merging options on `ChatModel`. Use `ChatClient` for delta-style overrides.
  - Pinning a Spring AI version that differs from the BOM.
  - Copying examples from 1.x pages without checking the 2.0.2 version of the page.

## Workflow

1. Confirm the provider and model ID, and add the starter plus the BOM.
2. Set startup defaults and the key placeholder in `application.yml`.
3. Write the `ChatService` against `ChatModel` using a `Prompt` of typed messages.
4. Add the streaming endpoint with explicit error handling.
5. Add session history storage with a bounded window.
6. Test with a mocked `ChatModel`, then run one live call using the real key from the environment.
