package com.caramelai.support.chat.session;

/** The session does not exist or has expired. Maps to HTTP 404. */
public class SessionNotFoundException extends RuntimeException {

  public SessionNotFoundException() {
    super("This chat session does not exist or has expired. Start a new chat.");
  }
}
