package com.caramelai.support.chat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a request ID and logs one line per request: method, path, status, latency. Paths carry
 * session IDs only. It never logs request or response bodies, query strings, or headers, so
 * customer text and the API key cannot reach the logs (Principles II and V, FR-012).
 *
 * <p>Streamed answers finish on an async dispatch. The line is logged once, when the request really
 * ends, and reuses the request ID from the first dispatch.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID = "requestId";
  private static final String START_ATTR = RequestLoggingFilter.class.getName() + ".start";
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String requestId = (String) request.getAttribute(REQUEST_ID);
    if (requestId == null) {
      requestId = UUID.randomUUID().toString().substring(0, 8);
      request.setAttribute(REQUEST_ID, requestId);
      request.setAttribute(START_ATTR, System.nanoTime());
      response.setHeader("X-Request-Id", requestId);
    }
    MDC.put(REQUEST_ID, requestId);
    try {
      chain.doFilter(request, response);
    } finally {
      if (!request.isAsyncStarted()) {
        long start = (Long) request.getAttribute(START_ATTR);
        log.info(
            "request method={} path={} status={} latencyMs={}",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            (System.nanoTime() - start) / 1_000_000);
      }
      MDC.remove(REQUEST_ID);
    }
  }
}
