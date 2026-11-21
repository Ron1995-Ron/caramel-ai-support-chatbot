package com.caramelai.support.chat.api;

import com.caramelai.support.chat.assistant.AssistantService;
import com.caramelai.support.chat.assistant.AssistantUnavailableException;
import com.caramelai.support.chat.config.ChatProperties;
import com.caramelai.support.chat.escalation.EscalationIntentDetector;
import com.caramelai.support.chat.escalation.EscalationRequest;
import com.caramelai.support.chat.escalation.EscalationService;
import com.caramelai.support.chat.metrics.ChatMetrics;
import com.caramelai.support.chat.session.ConversationWindow;
import com.caramelai.support.chat.session.Message;
import com.caramelai.support.chat.session.Session;
import com.caramelai.support.chat.session.SessionStore;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Sends a customer message and streams the assistant's reply as Server-Sent Events: zero or more
 * {@code token} events, then exactly one {@code done} or {@code error} event
 * (contracts/chat-api.yaml, FR-002, FR-008).
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/messages")
public class MessageController {

  private static final Logger log = LoggerFactory.getLogger(MessageController.class);

  private final SessionStore sessions;
  private final AssistantService assistant;
  private final ConversationWindow window;
  private final EscalationIntentDetector intentDetector;
  private final EscalationService escalations;
  private final ChatProperties properties;
  private final Clock clock;
  private final ChatMetrics metrics;

  public MessageController(
      SessionStore sessions,
      AssistantService assistant,
      ConversationWindow window,
      EscalationIntentDetector intentDetector,
      EscalationService escalations,
      ChatProperties properties,
      Clock clock,
      ChatMetrics metrics) {
    this.sessions = sessions;
    this.assistant = assistant;
    this.window = window;
    this.intentDetector = intentDetector;
    this.escalations = escalations;
    this.properties = properties;
    this.clock = clock;
    this.metrics = metrics;
  }

  public record CustomerMessageRequest(String content) {}

  /** One message as the UI sees it. {@code rating} is null until the customer rates an answer. */
  public record MessageView(
      UUID id, String role, String content, String status, Instant createdAt, String rating) {}

  /** The current session's messages, in order (FR-006). */
  @GetMapping
  List<MessageView> list(@PathVariable UUID sessionId) {
    Session session = sessions.get(sessionId);
    return session.messages().stream()
        .map(
            m ->
                new MessageView(
                    m.id(),
                    m.role().name().toLowerCase(Locale.ROOT),
                    m.content(),
                    m.status().name().toLowerCase(Locale.ROOT),
                    m.createdAt(),
                    session.feedbackFor(m.id()).map(f -> f.rating().wire()).orElse(null)))
        .toList();
  }

  @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  ResponseEntity<Flux<ServerSentEvent<Map<String, Object>>>> send(
      @PathVariable UUID sessionId, @RequestBody CustomerMessageRequest request) {
    Session session = sessions.get(sessionId);

    // Throws before the stream starts, so the client gets a normal 400 or 413 response.
    Message customerMessage =
        Message.customer(
            sessionId,
            request == null ? null : request.content(),
            properties.maxMessageLength(),
            clock.instant());

    List<Message> before = session.messages();
    session.append(customerMessage, clock.instant());

    Flux<ServerSentEvent<Map<String, Object>>> body =
        intentDetector.wantsHuman(customerMessage.content())
            ? streamEscalation(session, before)
            : streamAnswer(session, customerMessage, window.select(before));

    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("X-Accel-Buffering", "no")
        .body(body);
  }

  /**
   * The customer asked for a human in chat: create the ticket and confirm it, without the model.
   */
  private Flux<ServerSentEvent<Map<String, Object>>> streamEscalation(
      Session session, List<Message> before) {
    UUID assistantId = UUID.randomUUID();
    return Mono.fromCallable(() -> escalations.escalate(session, before))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(
            request -> {
              String text = confirmationText(request);
              appendAssistant(session, assistantId, text, Message.Status.COMPLETE);
              return Flux.just(
                  event("token", Map.of("delta", text)),
                  event("done", Map.of("messageId", assistantId.toString())));
            })
        .onErrorResume(
            error -> {
              appendAssistant(session, assistantId, "", Message.Status.FAILED);
              return Flux.just(errorEvent(error));
            });
  }

  private Flux<ServerSentEvent<Map<String, Object>>> streamAnswer(
      Session session, Message customerMessage, List<Message> history) {
    return Flux.defer(
        () -> {
          UUID assistantId = UUID.randomUUID();
          StringBuilder answer = new StringBuilder();
          long startNanos = System.nanoTime();
          AtomicLong firstTokenMs = new AtomicLong(-1);

          Flux<ServerSentEvent<Map<String, Object>>> tokens =
              assistant.stream(customerMessage.content(), history)
                  .map(
                      token -> {
                        if (firstTokenMs.compareAndSet(-1, elapsedMs(startNanos))) {
                          metrics.firstTokenAfter(firstTokenMs.get());
                        }
                        answer.append(token);
                        return event("token", Map.of("delta", token));
                      });

          Mono<ServerSentEvent<Map<String, Object>>> done =
              Mono.fromSupplier(
                  () -> {
                    appendAssistant(
                        session, assistantId, answer.toString(), Message.Status.COMPLETE);
                    log.info(
                        "Answer complete sessionId={} firstTokenMs={} totalMs={}",
                        session.id(),
                        firstTokenMs.get(),
                        elapsedMs(startNanos));
                    return event("done", Map.of("messageId", assistantId.toString()));
                  });

          return tokens
              .concatWith(done)
              .onErrorResume(
                  error -> {
                    // Keep any partial text, marked failed, so the UI never shows a blank gap.
                    appendAssistant(session, assistantId, answer.toString(), Message.Status.FAILED);
                    log.warn(
                        "Answer failed sessionId={} errorType={} totalMs={}",
                        session.id(),
                        error.getClass().getSimpleName(),
                        elapsedMs(startNanos));
                    return Flux.just(errorEvent(error));
                  });
        });
  }

  private void appendAssistant(Session session, UUID id, String text, Message.Status status) {
    session.append(
        Message.assistant(id, session.id(), text, status, clock.instant()), clock.instant());
  }

  static String confirmationText(EscalationRequest request) {
    return "I've created a support ticket for you (reference "
        + request.ticketRef()
        + "). A member of our support team will follow up. You can keep asking me questions in the"
        + " meantime.";
  }

  /** Only fixed, customer-safe messages are sent. Provider details never reach the client. */
  private static ServerSentEvent<Map<String, Object>> errorEvent(Throwable error) {
    if (error instanceof ServiceUnavailableException unavailable) {
      return event(
          "error", Map.of("code", unavailable.code(), "message", unavailable.getMessage()));
    }
    return event(
        "error",
        Map.of(
            "code", AssistantUnavailableException.CODE,
            "message", AssistantUnavailableException.CUSTOMER_MESSAGE));
  }

  private static ServerSentEvent<Map<String, Object>> event(String name, Map<String, Object> data) {
    return ServerSentEvent.<Map<String, Object>>builder().event(name).data(data).build();
  }

  private static long elapsedMs(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }
}
