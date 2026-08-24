package com.jairam.aicodeassistant.iam.adapter.security;

import com.jairam.aicodeassistant.iam.config.IamAuthProperties;
import com.jairam.aicodeassistant.iam.domain.model.User;
import com.jairam.aicodeassistant.iam.domain.port.AccessTokenIssuer;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Issues signed JWT access tokens using an HMAC (HS256) key.
 *
 * <p>Claims: {@code sub} = user id, {@code email}, plus standard {@code iss} / {@code iat} / {@code
 * exp}. Tokens are stateless — verified by signature and expiry on each request (see {@link
 * IamSecurityConfig}); org-scoped RBAC is evaluated per request against membership, so it is
 * intentionally not baked into the token.
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private final JwtEncoder jwtEncoder;
  private final IamAuthProperties properties;
  private final Clock clock;

  JwtAccessTokenIssuer(JwtEncoder jwtEncoder, IamAuthProperties properties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public IssuedAccessToken issue(User user) {
    Instant now = clock.instant();
    Instant expiresAt = now.plus(properties.accessTokenTtl());

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.id().toString())
            .claim("email", user.email().value())
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(token, properties.accessTokenTtl());
  }
}
