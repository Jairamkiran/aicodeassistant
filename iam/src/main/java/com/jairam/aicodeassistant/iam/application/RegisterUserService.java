package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.EmailAlreadyRegisteredException;
import com.jairam.aicodeassistant.iam.domain.event.UserRegistered;
import com.jairam.aicodeassistant.iam.domain.model.Email;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.port.PasswordHasher;
import com.jairam.aicodeassistant.iam.domain.port.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: register a new user.
 *
 * <p>Enforces email uniqueness, hashes the password via the {@link PasswordHasher} port, persists
 * the {@link User} aggregate, and publishes {@link UserRegistered} (relayed to Kafka via the
 * transactional outbox). All within one transaction so the state change and the event commit
 * atomically.
 */
@Service
public class RegisterUserService {

  private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

  private final UserRepository users;
  private final PasswordHasher passwordHasher;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public RegisterUserService(
      UserRepository users,
      PasswordHasher passwordHasher,
      ApplicationEventPublisher events,
      Clock clock) {
    this.users = users;
    this.passwordHasher = passwordHasher;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Registers a user.
   *
   * @param command the validated registration request
   * @return the new user's id
   * @throws EmailAlreadyRegisteredException if the email is already taken
   */
  @Transactional
  public RegisterUserResult register(RegisterUserCommand command) {
    Email email = new Email(command.email());
    if (users.existsByEmail(email)) {
      throw new EmailAlreadyRegisteredException(email.value());
    }

    Instant now = clock.instant();
    String hash = passwordHasher.hash(command.rawPassword());
    User user = User.register(email, hash, command.displayName().trim(), now);
    User saved = users.save(user);

    // Domain event (consumed by other contexts, e.g. notifications later) ...
    events.publishEvent(UserRegistered.of(saved.id().value(), email.value(), now));
    // ... plus a security audit signal for the audit log.
    events.publishEvent(
        com.jairam.aicodeassistant.platform.audit.AuditSignal.builder("USER_REGISTERED")
            .success(true)
            .actor("USER", saved.id().toString())
            .occurredAt(now)
            .build());
    log.info("Registered new user {}", saved.id());

    return new RegisterUserResult(saved.id().value());
  }

  /** Input command for registration (already bean-validated at the REST edge). */
  public record RegisterUserCommand(String email, String rawPassword, String displayName) {}

  /** Result carrying the created user id. */
  public record RegisterUserResult(java.util.UUID userId) {}
}
