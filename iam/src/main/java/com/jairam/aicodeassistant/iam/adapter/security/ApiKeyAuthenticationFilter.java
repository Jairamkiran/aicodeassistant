package com.jairam.aicodeassistant.iam.adapter.security;

import com.jairam.aicodeassistant.iam.application.ApiKeyAuthenticator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests presenting an {@code X-API-Key} header, as an alternative to the JWT
 * bearer path.
 *
 * <p>If the header is absent the filter does nothing and the chain proceeds (the request may still
 * authenticate via JWT, or be rejected as anonymous). If the header is present and valid, an {@link
 * ApiKeyPrincipal} is placed in the security context with authorities derived from the key's
 * scopes. An invalid key leaves the context empty, so the request is treated as unauthenticated and
 * the entry point returns 401.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  static final String HEADER = "X-API-Key";

  private final ApiKeyAuthenticator authenticator;

  public ApiKeyAuthenticationFilter(ApiKeyAuthenticator authenticator) {
    this.authenticator = authenticator;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HEADER);
    if (header != null
        && !header.isBlank()
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      authenticator
          .authenticate(header)
          .ifPresent(
              principal -> {
                var authorities =
                    principal.scopes().stream()
                        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope.name()))
                        .toList();
                SecurityContextHolder.getContext()
                    .setAuthentication(new ApiKeyAuthentication(principal, authorities));
              });
    }
    filterChain.doFilter(request, response);
  }

  /** Authentication holding the API-key principal. */
  static final class ApiKeyAuthentication extends AbstractAuthenticationToken {
    private static final long serialVersionUID = 1L;
    private final transient ApiKeyAuthenticator.AuthenticatedApiKey principal;

    ApiKeyAuthentication(
        ApiKeyAuthenticator.AuthenticatedApiKey principal,
        List<SimpleGrantedAuthority> authorities) {
      super(authorities);
      this.principal = principal;
      setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
      return null; // secret is never retained after authentication
    }

    @Override
    public Object getPrincipal() {
      return principal;
    }

    @Override
    public String getName() {
      return principal.userId().toString();
    }
  }
}
