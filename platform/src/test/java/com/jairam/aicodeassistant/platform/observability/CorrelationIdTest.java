package com.jairam.aicodeassistant.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CorrelationIdTest {

  @AfterEach
  void tearDown() {
    CorrelationId.clear();
  }

  @Test
  void currentReturnsSentinelWhenNothingBound() {
    CorrelationId.clear();
    assertThat(CorrelationId.current()).isEqualTo("no-correlation-id");
  }

  @Test
  void bindHonoursSuppliedValue() {
    String bound = CorrelationId.bind("client-supplied-id");

    assertThat(bound).isEqualTo("client-supplied-id");
    assertThat(CorrelationId.current()).isEqualTo("client-supplied-id");
  }

  @Test
  void bindGeneratesUuidWhenValueBlank() {
    String bound = CorrelationId.bind("   ");

    assertThat(bound).isNotBlank();
    assertThat(java.util.UUID.fromString(bound)).isNotNull();
  }

  @Test
  void bindTrimsWhitespace() {
    assertThat(CorrelationId.bind("  abc  ")).isEqualTo("abc");
  }

  @Test
  void clearRemovesBoundValue() {
    CorrelationId.bind("something");
    CorrelationId.clear();
    assertThat(CorrelationId.current()).isEqualTo("no-correlation-id");
  }
}
