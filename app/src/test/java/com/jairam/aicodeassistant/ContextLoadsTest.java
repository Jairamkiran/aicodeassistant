package com.jairam.aicodeassistant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the full {@code app} ApplicationContext wires successfully with all bounded
 * contexts on the classpath — WITHOUT external infrastructure.
 *
 * <p>The {@code test} profile excludes the DataSource/JPA/Kafka/Redis auto-configurations (which
 * require live infra); those are exercised by the Testcontainers integration test. This test guards
 * the bean graph, component scan, and auto-configuration wiring so a broken context is caught in
 * fast unit scope on every build.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContextLoadsTest {

  @Autowired private ApplicationContext context;

  @Test
  void contextLoads() {
    assertThat(context).isNotNull();
  }

  @Test
  void platformClockBeanIsPresent() {
    assertThat(context.getBean(java.time.Clock.class)).isNotNull();
  }

  @Test
  void globalExceptionHandlerFromPlatformWebIsWired() {
    assertThat(
            context.getBeansOfType(
                com.jairam.aicodeassistant.platform.web.GlobalExceptionHandler.class))
        .isNotEmpty();
  }
}
