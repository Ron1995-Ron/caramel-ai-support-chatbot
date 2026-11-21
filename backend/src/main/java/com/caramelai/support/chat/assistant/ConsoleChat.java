package com.caramelai.support.chat.assistant;

import com.caramelai.support.chat.session.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * A terminal chat with the assistant: type a question, read the streamed answer. Runs only when the
 * app is started with the {@code --chat} argument, so the web server is unaffected otherwise. The
 * conversation is kept in memory for this run only and is not logged.
 */
@Component
public class ConsoleChat implements CommandLineRunner {

  static final String CHAT_ARGUMENT = "--chat";

  private final AssistantService assistant;

  public ConsoleChat(AssistantService assistant) {
    this.assistant = assistant;
  }

  @Override
  public void run(String... args) throws IOException {
    for (String arg : args) {
      if (CHAT_ARGUMENT.equals(arg)) {
        systemPrompt(
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
            System.out);
        return;
      }
    }
  }

  /**
   * Talks with the assistant using the terminal: reads each line from standard input, prints the
   * streamed reply to standard output, and keeps earlier exchanges as session memory. Type exit or
   * quit, or close the input, to stop.
   */
  public void systemPrompt() throws IOException {
    systemPrompt(
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), System.out);
  }

  void systemPrompt(BufferedReader input, PrintStream output) throws IOException {
    UUID sessionId = UUID.randomUUID();
    List<Message> history = new ArrayList<>();
    output.println("Caramel AI support chat. Type 'exit' to quit.");

    while (true) {
      output.print("\nYou: ");
      output.flush();
      String line = input.readLine();
      if (line == null) {
        break;
      }
      String question = line.strip();
      if (question.isEmpty()) {
        continue;
      }
      if (question.equalsIgnoreCase("exit") || question.equalsIgnoreCase("quit")) {
        break;
      }

      Message customer = Message.customer(sessionId, question, 4000, Instant.now());
      StringBuilder answer = new StringBuilder();
      output.print("Assistant: ");
      output.flush();
      try {
        assistant.stream(question, history)
            .doOnNext(
                token -> {
                  answer.append(token);
                  output.print(token);
                  output.flush();
                })
            .blockLast();
        output.println();
        history.add(customer);
        history.add(
            Message.assistant(
                UUID.randomUUID(),
                sessionId,
                answer.toString(),
                Message.Status.COMPLETE,
                Instant.now()));
      } catch (AssistantUnavailableException e) {
        output.println();
        output.println("[The assistant is unavailable right now. Please try again.]");
      }
    }
    output.println("Goodbye.");
  }
}
