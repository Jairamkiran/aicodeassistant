package com.jairam.aicodeassistant.iam.domain.port;

import com.jairam.aicodeassistant.iam.domain.model.Email;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.model.UserId;
import java.util.Optional;

/**
 * Outbound port for persisting and loading {@link User} aggregates.
 *
 * <p>Defined in the domain, implemented by a persistence adapter. The domain and application layers
 * depend only on this interface — the Dependency Inversion Principle in practice.
 */
public interface UserRepository {

  /** Persists a new or updated user, returning the stored state. */
  User save(User user);

  Optional<User> findById(UserId id);

  Optional<User> findByEmail(Email email);

  boolean existsByEmail(Email email);
}
