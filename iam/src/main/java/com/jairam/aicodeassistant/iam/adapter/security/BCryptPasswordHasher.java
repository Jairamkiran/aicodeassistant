package com.jairam.aicodeassistant.iam.adapter.security;

import com.jairam.aicodeassistant.iam.domain.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link PasswordHasher} backed by Spring Security's {@link PasswordEncoder}.
 *
 * <p>The encoder is a {@code DelegatingPasswordEncoder} (see {@link IamSecurityConfig}) whose
 * default is BCrypt strength-12. Because stored hashes are prefixed with their scheme ({@code
 * {bcrypt}...}), the algorithm can be upgraded later without invalidating existing credentials.
 */
@Component
class BCryptPasswordHasher implements PasswordHasher {

  private final PasswordEncoder encoder;

  BCryptPasswordHasher(PasswordEncoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public String hash(String rawPassword) {
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(String rawPassword, String storedHash) {
    return encoder.matches(rawPassword, storedHash);
  }
}
