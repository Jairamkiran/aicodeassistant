package com.jairam.aicodeassistant.integration.github;

import com.jairam.aicodeassistant.integration.github.internal.GitHubOAuthService;
import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub OAuth web flow endpoints.
 *
 * <ul>
 *   <li>{@code GET /api/v1/integrations/github/authorize} — returns the GitHub authorize URL for
 *       the client to redirect to; stashes an anti-CSRF {@code state} in the HTTP session.
 *   <li>{@code GET /api/v1/integrations/github/callback} — GitHub redirects here with {@code code}
 *       + {@code state}; we verify state and complete the link.
 * </ul>
 *
 * <p>Both require an authenticated user (the link is bound to their account). The {@code state}
 * parameter is validated against the session value to prevent CSRF on the callback.
 */
@RestController
@RequestMapping("/api/v1/integrations/github")
class GitHubOAuthController {

  private static final String STATE_SESSION_KEY = "github_oauth_state";

  private final GitHubOAuthService oauthService;

  GitHubOAuthController(GitHubOAuthService oauthService) {
    this.oauthService = oauthService;
  }

  @GetMapping("/authorize")
  Map<String, String> authorize(HttpServletRequest request) {
    currentUserId(); // ensure authenticated
    String state = UUID.randomUUID().toString();
    request.getSession(true).setAttribute(STATE_SESSION_KEY, state);
    return Map.of("authorizeUrl", oauthService.buildAuthorizeUrl(state));
  }

  @GetMapping("/callback")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void callback(
      @RequestParam("code") String code,
      @RequestParam("state") String state,
      HttpServletRequest request) {
    UUID userId = currentUserId();
    HttpSession session = request.getSession(false);
    Object expectedState = session == null ? null : session.getAttribute(STATE_SESSION_KEY);
    if (expectedState == null || !expectedState.equals(state)) {
      throw new OAuthStateMismatchException();
    }
    session.removeAttribute(STATE_SESSION_KEY);
    oauthService.completeLink(userId, code);
  }

  private static UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
      throw new NotAuthenticatedException();
    }
    try {
      return UUID.fromString(auth.getName());
    } catch (IllegalArgumentException e) {
      throw new NotAuthenticatedException();
    }
  }

  /** Raised when the OAuth callback state does not match (possible CSRF). HTTP 400. */
  static final class OAuthStateMismatchException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    OAuthStateMismatchException() {
      super(
          ErrorType.VALIDATION,
          HttpStatus.BAD_REQUEST,
          "OAuth state mismatch",
          Map.of("provider", "github"));
    }
  }

  /** Raised when no authenticated principal is present. HTTP 401. */
  static final class NotAuthenticatedException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    NotAuthenticatedException() {
      super(ErrorType.AUTHENTICATION, HttpStatus.UNAUTHORIZED, "Not authenticated", Map.of());
    }
  }
}
