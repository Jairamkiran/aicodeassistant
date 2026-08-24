package com.jairam.aicodeassistant.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end API-key lifecycle over the real HTTP + security stack on H2 (no Docker): a user logs
 * in, mints an API key, authenticates a protected call with {@code X-API-Key}, then revokes it and
 * confirms the key no longer works.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("iamtest")
class ApiKeyE2eTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper json;

  private String bearerAfterRegisterAndLogin(String email) throws Exception {
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"email\":\"%s\",\"password\":\"Sup3rSecret!\",\"displayName\":\"K\"}"
                    .formatted(email)));
    String body =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"password\":\"Sup3rSecret!\"}".formatted(email)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("accessToken").asText();
  }

  @Test
  void apiKeyCanAuthenticateThenIsRejectedAfterRevoke() throws Exception {
    String bearer = bearerAfterRegisterAndLogin("apikey@example.com");

    // Mint an API key (READ scope).
    String createBody =
        mockMvc
            .perform(
                post("/api/v1/api-keys")
                    .header("Authorization", "Bearer " + bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"ci\",\"scopes\":[\"READ\"]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.apiKey").exists())
            .andExpect(jsonPath("$.keyPrefix").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String rawKey = json.readTree(createBody).get("apiKey").asText();
    String keyId = json.readTree(createBody).get("id").asText();
    assertThat(rawKey).startsWith("aca_");

    // Authenticate a protected endpoint using ONLY the API key.
    mockMvc
        .perform(get("/api/v1/users/me").header("X-API-Key", rawKey))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("apikey@example.com"));

    // It should appear in the listing (metadata only, no secret).
    mockMvc
        .perform(get("/api/v1/api-keys").header("Authorization", "Bearer " + bearer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].keyPrefix").exists())
        .andExpect(jsonPath("$[0].secretHash").doesNotExist());

    // Revoke it.
    mockMvc
        .perform(delete("/api/v1/api-keys/" + keyId).header("Authorization", "Bearer " + bearer))
        .andExpect(status().isNoContent());

    // The revoked key can no longer authenticate → 401.
    mockMvc
        .perform(get("/api/v1/users/me").header("X-API-Key", rawKey))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void malformedApiKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/me").header("X-API-Key", "not-a-valid-key"))
        .andExpect(status().isUnauthorized());
  }
}
