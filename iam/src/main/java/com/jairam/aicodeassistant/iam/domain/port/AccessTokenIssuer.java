package com.jairam.aicodeassistant.iam.domain.port;

import com.jairam.aicodeassistant.iam.domain.model.User;
import java.time.Duration;

/**
 * Outbound port that mints signed, stateless access tokens (JWTs in the M1 adapter). Kept as a port
 * so the application layer stays independent of the signing technology (Nimbus/JOSE) and key
 * management.
 */
public interface AccessTokenIssuer {

  /**
   * Issues a signed access token for the given user.
   *
   * @param user the authenticated user
   * @return the encoded token plus its time-to-live (for the client/response)
   */
  IssuedAccessToken issue(User user);

  /** An encoded access token and its validity window. */
  record IssuedAccessToken(String token, Duration expiresIn) {}
}
