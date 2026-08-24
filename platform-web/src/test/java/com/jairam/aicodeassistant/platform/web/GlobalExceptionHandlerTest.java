package com.jairam.aicodeassistant.platform.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jairam.aicodeassistant.platform.error.ResourceNotFoundException;
import com.jairam.aicodeassistant.platform.error.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies the RFC-9457 problem-detail contract produced by the central exception handler: stable
 * {@code type} URN, {@code code}, {@code correlationId}, {@code timestamp}, and correct HTTP status
 * per exception category.
 *
 * <p>Uses standalone MockMvc so it exercises the real advice without booting a full Spring context
 * (fast, no infra).
 */
class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @RestController
  static class ProbeController {
    @GetMapping("/probe/not-found")
    String notFound() {
      throw new ResourceNotFoundException("User", "u-1");
    }

    @GetMapping("/probe/validation")
    String validation() {
      throw new ValidationException("bad input");
    }

    static final String SECRET_LEAK_MARKER = "SENSITIVE-DB-PASSWORD-abc123";

    @GetMapping("/probe/boom")
    String boom() {
      throw new IllegalStateException(SECRET_LEAK_MARKER);
    }
  }

  @BeforeEach
  void setUp() {
    Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler(fixed))
            .build();
  }

  @Test
  void notFoundRendersRfc9457Body() throws Exception {
    mockMvc
        .perform(get("/probe/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.type").value("https://aicodeassistant.dev/problems/resource-not-found"))
        .andExpect(jsonPath("$.code").value("resource-not-found"))
        .andExpect(jsonPath("$.correlationId").exists())
        .andExpect(jsonPath("$.timestamp").value("2026-01-01T00:00Z"))
        .andExpect(jsonPath("$.resource").value("User"))
        .andExpect(jsonPath("$.identifier").value("u-1"));
  }

  @Test
  void validationRendersBadRequest() throws Exception {
    mockMvc
        .perform(get("/probe/validation"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation-error"));
  }

  @Test
  void unexpectedExceptionRendersSanitisedInternalError() throws Exception {
    mockMvc
        .perform(get("/probe/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("internal-error"))
        // The raw exception message must NOT leak to the client.
        .andExpect(
            jsonPath("$.detail")
                .value(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(ProbeController.SECRET_LEAK_MARKER))));
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder get(
      String path) {
    return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path);
  }
}
