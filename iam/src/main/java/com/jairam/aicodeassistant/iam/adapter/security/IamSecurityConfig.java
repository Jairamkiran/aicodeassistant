package com.jairam.aicodeassistant.iam.adapter.security;

import com.jairam.aicodeassistant.iam.application.ApiKeyAuthenticator;
import com.jairam.aicodeassistant.iam.config.IamAuthProperties;
import com.jairam.aicodeassistant.platform.web.ratelimit.RateLimitFilter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Stateless security configuration for the API.
 *
 * <p>Sessions are disabled; authentication is a bearer JWT validated on each request (HMAC HS256,
 * key from {@link IamAuthProperties#jwtSecret()}). Public endpoints: the auth routes, actuator
 * health, and API docs. Everything else requires a valid token. Method-level security
 * ({@code @PreAuthorize}) is enabled for finer-grained rules.
 */
@Configuration
@EnableMethodSecurity
class IamSecurityConfig {

  private final IamAuthProperties properties;
  private final ApiKeyAuthenticator apiKeyAuthenticator;
  private final RateLimitFilter rateLimitFilter;

  IamSecurityConfig(
      IamAuthProperties properties,
      ApiKeyAuthenticator apiKeyAuthenticator,
      RateLimitFilter rateLimitFilter) {
    this.properties = properties;
    this.apiKeyAuthenticator = apiKeyAuthenticator;
    this.rateLimitFilter = rateLimitFilter;
  }

  @Bean
  SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout")
                    .permitAll()
                    .requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
        // API-key auth runs before JWT processing: an X-API-Key header, if valid,
        // authenticates the request; otherwise the JWT bearer path applies.
        .addFilterBefore(
            new ApiKeyAuthenticationFilter(apiKeyAuthenticator), BasicAuthenticationFilter.class)
        // Rate limiting runs AFTER authentication (so it can key by principal)
        // but before controllers, inside the chain where the SecurityContext is
        // still bound. BasicAuthenticationFilter is our anchor: API-key filter is
        // before it, rate limiting after it.
        .addFilterAfter(rateLimitFilter, ApiKeyAuthenticationFilter.class)
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    return http.build();
  }

  /** BCrypt-by-default delegating encoder (scheme-prefixed hashes, upgradable). */
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  private SecretKeySpec secretKey() {
    byte[] keyBytes = properties.jwtSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder() {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
  }

  @Bean
  JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
  }
}
