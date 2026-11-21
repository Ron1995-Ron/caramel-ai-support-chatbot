package com.caramelai.support.chat.api;

/** A dependency such as the ticketing system is down (HTTP 503). Message is customer-facing. */
public class ServiceUnavailableException extends RuntimeException {

  private final String code;

  public ServiceUnavailableException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
