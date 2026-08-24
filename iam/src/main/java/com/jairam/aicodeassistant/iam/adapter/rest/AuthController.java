package com.jairam.aicodeassistant.iam.adapter.rest;

import com.jairam.aicodeassistant.iam.adapter.rest.dto.LoginRequest;
import com.jairam.aicodeassistant.iam.adapter.rest.dto.RegisterRequest;
import com.jairam.aicodeassistant.iam.adapter.rest.dto.RegisterResponse;
import com.jairam.aicodeassistant.iam.adapter.rest.dto.TokenResponse;
import com.jairam.aicodeassistant.iam.application.AuthenticationResult;
import com.jairam.aicodeassistant.iam.application.AuthenticationService;
import com.jairam.aicodeassistant.iam.application.RegisterUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints: register, login, refresh, logout.
 *
 * <p>Access tokens are returned in the JSON body; refresh tokens are delivered and read via an
 * HttpOnly cookie (see {@link RefreshCookieFactory}). These routes are public in {@code
 * IamSecurityConfig}; everything else requires a bearer token.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

  private final RegisterUserService registerUserService;
  private final AuthenticationService authenticationService;
  private final com.jairam.aicodeassistant.iam.application.TokenService tokenService;
  private final RefreshCookieFactory cookies;

  AuthController(
      RegisterUserService registerUserService,
      AuthenticationService authenticationService,
      com.jairam.aicodeassistant.iam.application.TokenService tokenService,
      RefreshCookieFactory cookies) {
    this.registerUserService = registerUserService;
    this.authenticationService = authenticationService;
    this.tokenService = tokenService;
    this.cookies = cookies;
  }

  @PostMapping("/register")
  ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    var result =
        registerUserService.register(
            new RegisterUserService.RegisterUserCommand(
                request.email(), request.password(), request.displayName()));
    return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(result.userId()));
  }

  @PostMapping("/login")
  ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthenticationResult result =
        authenticationService.login(
            new AuthenticationService.LoginCommand(request.email(), request.password()));
    return respondWithTokens(result);
  }

  @PostMapping("/refresh")
  ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
    String raw = readRefreshCookie(request);
    AuthenticationResult result = tokenService.rotate(raw);
    return respondWithTokens(result);
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout(HttpServletRequest request) {
    String raw = readRefreshCookie(request);
    tokenService.revoke(raw);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
        .build();
  }

  // --- helpers -----------------------------------------------------------------

  private ResponseEntity<TokenResponse> respondWithTokens(AuthenticationResult result) {
    ResponseCookie refreshCookie =
        cookies.issue(result.rawRefreshToken(), result.refreshTokenTtl());
    TokenResponse body =
        TokenResponse.bearer(result.accessToken(), result.accessTokenTtl().toSeconds());
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(body);
  }

  private String readRefreshCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    for (var cookie : request.getCookies()) {
      if (cookies.cookieName().equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
