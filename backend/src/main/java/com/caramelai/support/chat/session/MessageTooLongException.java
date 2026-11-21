package com.caramelai.support.chat.session;

/** Customer message is over the length limit. Maps to HTTP 413. */
public class MessageTooLongException extends RuntimeException {

  public MessageTooLongException(int maxLength) {
    super("The message is too long. Please keep it under " + maxLength + " characters.");
  }
}
