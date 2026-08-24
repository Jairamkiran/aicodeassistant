package com.jairam.aicodeassistant.iam.application;

import com.jairam.aicodeassistant.iam.config.IamAuthProperties;
import com.jairam.aicodeassistant.iam.domain.InvalidRefreshTokenException;
import com.jairam.aicodeassistant.iam.domain.model.RefreshToken;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.port.AccessTokenIssuer;
import com.jairam.aicodeassistant.iam.domain.port.RefreshTokenGenerator;
import com.jairam.aicodeassistant.iam.domain.port.RefreshTokenStore;
import com.jairam.aicodeassistant.iam.domain.port.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and rotates the access/refresh token pair, and implements refresh-token <em>reuse
 * detection</em>.
 *
 * <p>Rotation contract: presenting an active refresh token consumes it ({@code markUsed}) and
 * issues a new token in the same family plus a fresh access token. Presenting a token that is
 * already used or revoked is treated as a replay of a stolen token — the entire family is revoked
 * and the request is rejected, forcing re-authentication.
 */
@Service
public class TokenService {

  private static final Logger log = LoggerFactory.getLogger(TokenService.class);

  private final RefreshTokenStore refreshTokens;
  private final RefreshTokenGenerator refreshTokenGenerator;
  private final AccessTokenIssuer accessTokenIssuer;
  private final UserRepository users;
  private final TokenFamilyRevoker familyRevoker;
  private final IamAuthProperties properties;
  private final Clock clock;

  public TokenService(
      RefreshTokenStore refreshTokens,
      RefreshTokenGenerator refreshTokenGenerator,
      AccessTokenIssuer accessTokenIssuer,
      UserRepository users,
      TokenFamilyRevoker familyRevoker,
      IamAuthProperties properties,
      Clock clock) {
    this.refreshTokens = refreshTokens;
    this.refreshTokenGenerator = refreshTokenGenerator;
    this.accessTokenIssuer = accessTokenIssuer;
    this.users = users;
    this.familyRevoker = familyRevoker;
    this.properties = properties;
    this.clock = clock;
  }

  /** Issues a brand-new token pair for a freshly authenticated user (login). */
  @Transactional
  public AuthenticationResult issueForNewSession(User user) {
    Instant now = clock.instant();
    var generated = refreshTokenGenerator.generate();
    RefreshToken token =
        RefreshToken.issueNewFamily(
            user.id(), generated.tokenHash(), now, now.plus(properties.refreshTokenTtl()));
    refreshTokens.save(token);
    return assemble(user, generated.rawValue());
  }

  /**
   * Rotates a presented refresh token into a new pair.
   *
   * @param rawRefreshToken the client's raw refresh secret (from the cookie)
   * @throws InvalidRefreshTokenException if unknown/expired, or (reuse detected) already
   *     used/revoked — in which case the family is revoked first
   */
  @Transactional
  public AuthenticationResult rotate(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw new InvalidRefreshTokenException();
    }
    Instant now = clock.instant();
    String presentedHash = refreshTokenGenerator.hash(rawRefreshToken);

    RefreshToken existing =
        refreshTokens.findByTokenHash(presentedHash).orElseThrow(InvalidRefreshTokenException::new);

    // Reuse detection: an already-used or revoked token presented again means the
    // secret leaked and is being replayed. Burn the whole family — in a NEW
    // transaction so the revocation commits even though we then throw to reject
    // the request (a single enclosing transaction would roll the revocation back).
    if (existing.isUsed() || existing.isRevoked()) {
      int revoked = familyRevoker.revokeFamilyNow(existing.familyId(), now);
      log.warn(
          "Refresh token reuse detected for user {} — revoked {} token(s) in family {}",
          existing.userId(),
          revoked,
          existing.familyId());
      throw new InvalidRefreshTokenException();
    }

    if (!existing.isActive(now)) {
      throw new InvalidRefreshTokenException();
    }

    User user = users.findById(existing.userId()).orElseThrow(InvalidRefreshTokenException::new);
    if (!user.canAuthenticate()) {
      // Account disabled since login — revoke the family (new tx) and refuse.
      familyRevoker.revokeFamilyNow(existing.familyId(), now);
      throw new InvalidRefreshTokenException();
    }

    // Consume the presented token and issue its successor in the same family.
    existing.markUsed(now);
    refreshTokens.save(existing);

    var generated = refreshTokenGenerator.generate();
    RefreshToken next =
        RefreshToken.issueInFamily(
            user.id(),
            existing.familyId(),
            generated.tokenHash(),
            now,
            now.plus(properties.refreshTokenTtl()));
    refreshTokens.save(next);

    return assemble(user, generated.rawValue());
  }

  /** Revokes the family of the presented token (logout). Silent if unknown. */
  @Transactional
  public void revoke(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      return;
    }
    Instant now = clock.instant();
    String presentedHash = refreshTokenGenerator.hash(rawRefreshToken);
    refreshTokens
        .findByTokenHash(presentedHash)
        .ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), now));
  }

  private AuthenticationResult assemble(User user, String rawRefreshValue) {
    AccessTokenIssuer.IssuedAccessToken access = accessTokenIssuer.issue(user);
    return new AuthenticationResult(
        access.token(), access.expiresIn(), rawRefreshValue, properties.refreshTokenTtl());
  }
}
