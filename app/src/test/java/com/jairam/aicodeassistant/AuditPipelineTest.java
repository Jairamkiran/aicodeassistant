package com.jairam.aicodeassistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jairam.aicodeassistant.audit.AuditQuery;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the end-to-end audit pipeline in the full app context (H2, no Docker): a registration
 * and a failed login both produce audit rows via the neutral {@code AuditSignal} → {@code
 * AuditSignalListener} → {@code audit_events} path. This is the cross-module wiring proof (iam
 * publishes, audit records) that the per-module tests cannot cover in isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditPipelineTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditQuery auditQuery;

  @Test
  void registrationAndFailedLoginAreAudited() throws Exception {
    long before = auditQuery.count();

    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"email\":\"audit@example.com\",\"password\":\"Sup3rSecret!\","
                    + "\"displayName\":\"Audit\"}"));

    mockMvc.perform(
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"audit@example.com\",\"password\":\"WRONG\"}"));

    // Audit writes commit in their own (REQUIRES_NEW) transactions; give them a
    // moment in case of async ordering, then assert both event types landed.
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              assertThat(auditQuery.count()).isGreaterThan(before);
              var types = auditQuery.recent(20).stream().map(AuditQuery.Entry::eventType).toList();
              assertThat(types).contains("USER_REGISTERED", "USER_LOGIN");
            });
  }
}
