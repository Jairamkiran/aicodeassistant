package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.InvalidCredentialsException;
import com.jairam.aicodeassistant.iam.domain.model.Email;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.port.PasswordHasher;
import com.jairam.aicodeassistant.iam.domain.port.UserRepository;
import com.jairam.aicodeassistant.platform.audit.AuditSignal;
import java.time.Clock;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: authenticate a user with email + password and issue a token pair.
 *
 * <p>Failures are deliberately indistinguishable (unknown email, wrong password, disabled account
 * all raise the same {@link InvalidCredentialsException}) to prevent user enumeration. A timing
 * side-channel remains for the unknown-email path (no hash comparison occurs); hardening it with a
 * dummy-hash compare is noted as a future improvement rather than implemented in M1.
 *
 * <p>Emits an {@link AuditSignal} for both successful and failed logins. Failed-login signals are
 * especially valuable (brute-force detection), so they are published even though the use case
 * throws — the audit write runs in its own transaction (see the audit module).
 */
@Service
public class AuthenticationService {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

  static final String ACTION_LOGIN = "USER_LOGIN";

  private final UserRepository users;
  private final PasswordHasher passwordHasher;
  private final TokenService tokenService;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public AuthenticationService(
      UserRepository users,
      PasswordHasher passwordHasher,
      TokenService tokenService,
      ApplicationEventPublisher events,
      Clock clock) {
    this.users = users;
    this.passwordHasher = passwordHasher;
    this.tokenService = tokenService;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Authenticates and issues tokens.
   *
   * @param command validated login request
   * @return access + refresh tokens on success
   * @throws InvalidCredentialsException on any authentication failure
   */
  @Transactional
  public AuthenticationResult login(LoginCommand command) {
    Email email;
    try {
      email = new Email(command.email());
    } catch (IllegalArgumentException malformed) {
      // A malformed email can't match any account; treat as invalid credentials.
      auditFailure(command.email());
      throw new InvalidCredentialsException();
    }

    User user = users.findByEmail(email).orElse(null);
    if (user == null
        || !passwordHasher.matches(command.rawPassword(), user.passwordHash())
        || !user.canAuthenticate()) {
      auditFailure(email.value());
      throw new InvalidCredentialsException();
    }

    log.info("User {} authenticated", user.id());
    events.publishEvent(
        AuditSignal.builder(ACTION_LOGIN)
            .success(true)
            .actor("USER", user.id().toString())
            .occurredAt(clock.instant())
            .build());
    return tokenService.issueForNewSession(user);
  }

  private void auditFailure(String attemptedEmail) {
    events.publishEvent(
        AuditSignal.builder(ACTION_LOGIN)
            .success(false)
            .actor("ANONYMOUS", null)
            .occurredAt(clock.instant())
            .attributes(Map.of("attemptedEmail", attemptedEmail))
            .build());
  }

  /** Validated login request. */
  public record LoginCommand(String email, String rawPassword) {}
}
