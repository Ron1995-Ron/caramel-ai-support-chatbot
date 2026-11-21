package com.caramelai.support.chat.api;

/** A request that is well-formed but not allowed, such as rating a customer message (HTTP 422). */
public class InvalidRequestException extends RuntimeException {

  private final int status;
  private final String code;

  public InvalidRequestException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }
}
