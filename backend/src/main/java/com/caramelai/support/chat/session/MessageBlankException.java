package com.caramelai.support.chat.session;

/** Customer message is empty or whitespace-only. Maps to HTTP 400. */
public class MessageBlankException extends RuntimeException {

  public MessageBlankException() {
    super("The message is empty. Type a question and try again.");
  }
}
