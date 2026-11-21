package com.caramelai.support.chat.escalation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.caramelai.support.chat.api.ServiceUnavailableException;
import com.caramelai.support.chat.assistant.SystemPrompt;
import com.caramelai.support.chat.escalation.EscalationRequest.Reason;
import com.caramelai.support.chat.escalation.EscalationRequest.TicketPayload;
import com.caramelai.support.chat.metrics.ChatMetrics;
import com.caramelai.support.chat.session.Message;
import com.caramelai.support.chat.session.Session;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EscalationServiceTest {

  private static final Clock CLOCK = Clock.systemUTC();

  private static Message customer(Session s, String text) {
    return Message.customer(s.id(), text, 4000, Instant.now());
  }

  private static Message assistant(Session s, String text, Message.Status status) {
    return Message.assistant(java.util.UUID.randomUUID(), s.id(), text, status, Instant.now());
  }

  @Test
  void ticketCarriesOnlySessionReasonAndLatestMessageNotTheTranscript() {
    List<TicketPayload> sent = new ArrayList<>();
    EscalationService service =
        new EscalationService(
            payload -> {
              sent.add(payload);
              return "T-1";
            },
            CLOCK,
            new ChatMetrics(new SimpleMeterRegistry()));
    Session session = new Session(Instant.now());
    session.append(customer(session, "FIRST-MESSAGE-SECRET"), Instant.now());
    session.append(assistant(session, "an answer", Message.Status.COMPLETE), Instant.now());
    session.append(customer(session, "I want a human"), Instant.now());

    EscalationRequest request = service.escalate(session, session.messages());

    assertThat(request.ticketRef()).isEqualTo("T-1");
    assertThat(sent).hasSize(1);
    assertThat(sent.get(0).latestCustomerMessage()).isEqualTo("I want a human");
    assertThat(sent.get(0).toString()).doesNotContain("FIRST-MESSAGE-SECRET");
    assertThat(session.status()).isEqualTo(Session.Status.ESCALATED);
  }

  @Test
  void aTicketingFailureBecomesACustomerSafeServiceUnavailableError() {
    EscalationService service =
        new EscalationService(
            payload -> {
              throw new TicketingClient.TicketingUnavailableException(new RuntimeException("boom"));
            },
            CLOCK,
            new ChatMetrics(new SimpleMeterRegistry()));
    Session session = new Session(Instant.now());

    assertThatThrownBy(() -> service.escalate(session, List.of()))
        .isInstanceOfSatisfying(
            ServiceUnavailableException.class,
            e -> {
              assertThat(e.code()).isEqualTo(EscalationService.TICKETING_UNAVAILABLE_CODE);
              assertThat(e.getMessage()).doesNotContain("boom");
            });
    // A failed attempt is recorded, and the session is not marked escalated.
    assertThat(session.escalations()).hasSize(1);
    assertThat(session.escalations().get(0).status()).isEqualTo(EscalationRequest.Status.FAILED);
    assertThat(session.status()).isEqualTo(Session.Status.ACTIVE);
  }

  @Test
  void reasonIsAssistantUnableWhenTheAssistantFailedOrOfferedAHuman() {
    Session s = new Session(Instant.now());
    List<Message> failed = List.of(customer(s, "q"), assistant(s, "", Message.Status.FAILED));
    List<Message> offered =
        List.of(
            customer(s, "q"),
            assistant(
                s, "I don't know. " + SystemPrompt.ESCALATION_OFFER, Message.Status.COMPLETE));

    assertThat(EscalationService.reasonFor(failed)).isEqualTo(Reason.ASSISTANT_UNABLE);
    assertThat(EscalationService.reasonFor(offered)).isEqualTo(Reason.ASSISTANT_UNABLE);
  }

  @Test
  void reasonIsCustomerRequestOtherwise() {
    Session s = new Session(Instant.now());
    List<Message> normal =
        List.of(customer(s, "q"), assistant(s, "a fine answer", Message.Status.COMPLETE));

    assertThat(EscalationService.reasonFor(List.of())).isEqualTo(Reason.CUSTOMER_REQUEST);
    assertThat(EscalationService.reasonFor(normal)).isEqualTo(Reason.CUSTOMER_REQUEST);
    assertThat(EscalationService.reasonFor(List.of(customer(s, "q"))))
        .isEqualTo(Reason.CUSTOMER_REQUEST);
  }
}
