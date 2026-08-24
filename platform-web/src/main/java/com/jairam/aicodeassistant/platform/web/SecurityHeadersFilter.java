package com.jairam.aicodeassistant.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds baseline security response headers to every API response. These are cheap, broadly-safe
 * defenses for a JSON API served behind the SPA:
 *
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} — no MIME sniffing;
 *   <li>{@code X-Frame-Options: DENY} — the API is never framed;
 *   <li>{@code Referrer-Policy} — do not leak full URLs cross-origin;
 *   <li>{@code Cache-Control: no-store} — API responses may carry per-user data and must not be
 *       cached by shared caches.
 * </ul>
 *
 * <p>Content-Security-Policy is intentionally set on the SPA (nginx) rather than the API, since CSP
 * governs document/script loading, not JSON responses.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    if (response.getHeader("Cache-Control") == null) {
      response.setHeader("Cache-Control", "no-store");
    }
    filterChain.doFilter(request, response);
  }
}
