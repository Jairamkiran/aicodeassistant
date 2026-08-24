package com.jairam.aicodeassistant.platform.web;

import com.jairam.aicodeassistant.platform.observability.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ensures every request carries a correlation id for the duration of its processing. If the client
 * supplied {@link CorrelationId#HEADER}, that value is honoured (enabling end-to-end tracing across
 * services); otherwise a fresh UUID is generated. The id is bound to the MDC (for logs), echoed
 * back on the response header (for clients), and always cleared in a finally block so it cannot
 * leak across pooled threads.
 *
 * <p>Ordered highest precedence so the id is present for all downstream filters (including
 * security) and their logs.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = CorrelationId.bind(request.getHeader(CorrelationId.HEADER));
    try {
      response.setHeader(CorrelationId.HEADER, correlationId);
      filterChain.doFilter(request, response);
    } finally {
      CorrelationId.clear();
    }
  }
}
