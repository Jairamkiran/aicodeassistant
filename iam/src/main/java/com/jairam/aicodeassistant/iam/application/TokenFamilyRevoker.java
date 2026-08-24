package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.domain.port.RefreshTokenStore;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes a refresh-token family in its OWN transaction.
 *
 * <p>This exists as a separate bean on purpose. When reuse is detected, the caller revokes the
 * family and then throws to reject the request — but that exception would roll back a single
 * enclosing transaction, undoing the revocation. Running the revocation with {@link
 * Propagation#REQUIRES_NEW} commits it independently, so a detected-stolen family is genuinely
 * killed even though the request is refused. (Self-invocation cannot change propagation, hence the
 * dedicated bean.)
 */
@Component
public class TokenFamilyRevoker {

  private final RefreshTokenStore refreshTokens;

  public TokenFamilyRevoker(RefreshTokenStore refreshTokens) {
    this.refreshTokens = refreshTokens;
  }

  /** Revokes every active token in the family in a new, independently-committed transaction. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int revokeFamilyNow(UUID familyId, Instant now) {
    return refreshTokens.revokeFamily(familyId, now);
  }
}
