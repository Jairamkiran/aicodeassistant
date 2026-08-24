package com.jairam.aicodeassistant.integration.github;

import com.jairam.aicodeassistant.platform.error.ApplicationException;
import com.jairam.aicodeassistant.platform.error.ErrorType;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Base type for GitHub integration failures, surfaced as domain errors so callers never see
 * provider HTTP/JSON specifics.
 *
 * <p>Subtypes distinguish the failure modes callers may reasonably act on: {@link NotLinked} (user
 * must connect GitHub), {@link Unavailable} (transient — retry later), and the generic case. All
 * render through the platform RFC-9457 handler.
 */
public class GitHubException extends ApplicationException {

  private static final long serialVersionUID = 1L;

  protected GitHubException(
      ErrorType type, HttpStatus status, String message, Map<String, Object> properties) {
    super(type, status, message, properties);
  }

  /** The user has not linked a GitHub account (or the link was revoked). HTTP 409. */
  public static final class NotLinked extends GitHubException {
    private static final long serialVersionUID = 1L;

    public NotLinked() {
      super(
          ErrorType.CONFLICT,
          HttpStatus.CONFLICT,
          "GitHub account is not linked",
          Map.of("provider", "github"));
    }
  }

  /** GitHub is unreachable, timed out, rate-limited, or returned 5xx. HTTP 503. */
  public static final class Unavailable extends GitHubException {
    private static final long serialVersionUID = 1L;

    public Unavailable(String detail) {
      super(
          ErrorType.DEPENDENCY_UNAVAILABLE,
          HttpStatus.SERVICE_UNAVAILABLE,
          "GitHub is temporarily unavailable: " + detail,
          Map.of("provider", "github"));
    }
  }

  /** The stored GitHub credential was rejected (401/403) — re-link required. HTTP 409. */
  public static final class CredentialRejected extends GitHubException {
    private static final long serialVersionUID = 1L;

    public CredentialRejected() {
      super(
          ErrorType.CONFLICT,
          HttpStatus.CONFLICT,
          "GitHub rejected the stored credential; please re-link your account",
          Map.of("provider", "github"));
    }
  }
}
