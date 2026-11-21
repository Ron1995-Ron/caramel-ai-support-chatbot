package com.caramelai.support.chat.assistant;

/**
 * The AI provider is unavailable, returned an error, or did not start answering in time. The
 * message is safe to show to the customer: it never carries provider details or the API key.
 */
public class AssistantUnavailableException extends RuntimeException {

  public static final String CODE = "PROVIDER_UNAVAILABLE";
  public static final String CUSTOMER_MESSAGE =
      "The assistant could not answer right now. Please try again, or ask for a human agent.";

  public AssistantUnavailableException(Throwable cause) {
    // The cause is kept for the error type only. It is never logged with its message.
    super(CUSTOMER_MESSAGE, cause, false, false);
  }
}
