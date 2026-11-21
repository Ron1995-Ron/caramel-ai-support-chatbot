package com.caramelai.support.chat.api;

import com.caramelai.support.chat.session.MessageBlankException;
import com.caramelai.support.chat.session.MessageTooLongException;
import com.caramelai.support.chat.session.SessionNotFoundException;
import jakarta.servlet.ServletException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps errors to RFC 9457 problem responses with a stable {@code code} the UI can use
 * (contracts/chat-api.yaml). Messages are written for the customer and never contain exception
 * internals, message text, or the API key.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(SessionNotFoundException.class)
  ResponseEntity<ProblemDetail> sessionNotFound(SessionNotFoundException e) {
    return problem(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found", e.getMessage());
  }

  @ExceptionHandler(MessageBlankException.class)
  ResponseEntity<ProblemDetail> blank(MessageBlankException e) {
    return problem(HttpStatus.BAD_REQUEST, "MESSAGE_BLANK", "Message is empty", e.getMessage());
  }

  @ExceptionHandler(MessageTooLongException.class)
  ResponseEntity<ProblemDetail> tooLong(MessageTooLongException e) {
    return problem(
        HttpStatus.CONTENT_TOO_LARGE, "MESSAGE_TOO_LONG", "Message is too long", e.getMessage());
  }

  @ExceptionHandler(InvalidRequestException.class)
  ResponseEntity<ProblemDetail> invalid(InvalidRequestException e) {
    return problem(
        HttpStatus.valueOf(e.status()), e.code(), "Request cannot be processed", e.getMessage());
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  ResponseEntity<ProblemDetail> unavailable(ServiceUnavailableException e) {
    return problem(HttpStatus.SERVICE_UNAVAILABLE, e.code(), "Service unavailable", e.getMessage());
  }

  /** Malformed or unreadable JSON body: the caller's mistake, not a server error. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException e) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST",
        "Request cannot be read",
        "The request body could not be read.");
  }

  /**
   * Errors Spring MVC raises itself, such as a wrong HTTP method (405), media type (415), or an
   * unknown path (404). They keep their own status instead of becoming a 500. Only fixed text is
   * returned, never exception text.
   */
  @ExceptionHandler(ServletException.class)
  ResponseEntity<ProblemDetail> framework(ServletException e) {
    if (!(e instanceof ErrorResponse errorResponse)) {
      return unexpected(e);
    }
    HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.BAD_REQUEST;
    }
    return problem(
        status, "REQUEST_REJECTED", "Request rejected", "This request is not supported.");
  }

  /** Last resort: log the error type only (never the message) and return a generic problem. */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> unexpected(Exception e) {
    log.error("Unexpected error errorType={}", e.getClass().getSimpleName());
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Something went wrong",
        "Something went wrong on our side. Please try again.");
  }

  private static ResponseEntity<ProblemDetail> problem(
      HttpStatus status, String code, String title, String detail) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
    body.setType(URI.create("urn:caramel:problem:" + code.toLowerCase().replace('_', '-')));
    body.setTitle(title);
    body.setProperty("code", code);
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
  }
}
