package com.jairam.aicodeassistant.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end authentication flow over the real HTTP stack (MockMvc through the Spring Security
 * filter chain), backed by H2 — runs WITHOUT Docker.
 *
 * <p>Covers the M1 acceptance flow: register → login → access a protected endpoint → refresh
 * (rotation) → logout, plus negative paths: unauthenticated access is 401, duplicate email is 409,
 * bad credentials are 401, and refresh reuse is detected and the family revoked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("iamtest")
class AuthFlowE2eTest {

  private static final String REFRESH_COOKIE = "aicodeassistant_refresh";

  @Autowired private MockMvc mockMvc;

  private static String registerBody(String email) {
    return """
        {"email":"%s","password":"Sup3rSecret!","displayName":"Test User"}
        """
        .formatted(email);
  }

  private static String loginBody(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }

  @Test
  void fullHappyPath() throws Exception {
    String email = "happy@example.com";

    // Register -> 201 with a user id.
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").exists());

    // Login -> 200, access token in body, refresh token in HttpOnly cookie.
    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(email, "Sup3rSecret!")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(cookie().exists(REFRESH_COOKIE))
            .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
            .andReturn();

    String accessToken = readJson(login, "accessToken");
    Cookie refreshCookie = login.getResponse().getCookie(REFRESH_COOKIE);
    assertThat(refreshCookie).isNotNull();

    // Access protected /me with the bearer token -> 200 and correct identity.
    mockMvc
        .perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.memberships").isArray());

    // Refresh (rotation) -> 200, new access token, new refresh cookie.
    MvcResult refreshed =
        mockMvc
            .perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(cookie().exists(REFRESH_COOKIE))
            .andReturn();

    Cookie rotatedCookie = refreshed.getResponse().getCookie(REFRESH_COOKIE);
    assertThat(rotatedCookie).isNotNull();
    assertThat(rotatedCookie.getValue()).isNotEqualTo(refreshCookie.getValue());

    // Logout with the rotated cookie -> 204 and cookie cleared.
    mockMvc
        .perform(post("/api/v1/auth/logout").cookie(rotatedCookie))
        .andExpect(status().isNoContent());
  }

  @Test
  void protectedEndpointRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void duplicateEmailIsConflict() throws Exception {
    String email = "dup@example.com";
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("resource-conflict"));
  }

  @Test
  void wrongPasswordIsUnauthorized() throws Exception {
    String email = "wrongpw@example.com";
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody(email)));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, "WrongPassword!")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("authentication-error"));
  }

  @Test
  void refreshTokenReuseIsDetectedAndFamilyRevoked() throws Exception {
    String email = "reuse@example.com";
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody(email)));

    Cookie original =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(email, "Sup3rSecret!")))
            .andReturn()
            .getResponse()
            .getCookie(REFRESH_COOKIE);
    assertThat(original).isNotNull();

    // First rotation consumes the original token and yields a new one.
    Cookie rotated =
        mockMvc
            .perform(post("/api/v1/auth/refresh").cookie(original))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie(REFRESH_COOKIE);
    assertThat(rotated).isNotNull();

    // Replaying the ORIGINAL (now used) token is reuse -> 401, and it revokes the
    // whole family, so the previously-valid rotated token is now rejected too.
    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(original))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(post("/api/v1/auth/refresh").cookie(rotated))
        .andExpect(status().isUnauthorized());
  }

  private static String readJson(MvcResult result, String field) throws Exception {
    String body = result.getResponse().getContentAsString();
    var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
    return node.get(field).asText();
  }
}
