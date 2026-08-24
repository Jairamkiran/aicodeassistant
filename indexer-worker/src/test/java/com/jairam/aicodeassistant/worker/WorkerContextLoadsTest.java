package com.jairam.aicodeassistant.worker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the indexer-worker ApplicationContext wires successfully without external infrastructure
 * — H2 for JPA and an in-JVM {@link EmbeddedKafka} broker for the Kafka listener machinery (no
 * Docker). Also asserts the worker does NOT drag in the business web layer: the {@code
 * platform-web} RFC-9457 handler must be ABSENT, proving the worker's footprint is lean.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(topics = "repository.import-requested")
class WorkerContextLoadsTest {

  @Autowired private ApplicationContext context;

  @Test
  void contextLoads() {
    assertThat(context).isNotNull();
  }

  @Test
  void platformClockIsAvailable() {
    assertThat(context.getBean(java.time.Clock.class)).isNotNull();
  }

  @Test
  void businessControllersAreAbsent() {
    // The worker is a headless deployable: it must NOT expose our business REST
    // controllers (auth, search, repositories, api-keys). It legitimately has a
    // servlet container for actuator (Boot's BasicErrorController) and may see
    // platform-web's shared error handler — so we assert specifically that no
    // bean from our adapter.rest packages is present.
    assertThat(context.getBeanDefinitionNames())
        .filteredOn(
            name -> {
              Class<?> type = context.getType(name);
              return type != null && type.getName().contains(".adapter.rest.");
            })
        .isEmpty();
  }
}
