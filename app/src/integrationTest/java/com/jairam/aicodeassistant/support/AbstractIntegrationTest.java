package com.jairam.aicodeassistant.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that need the real infrastructure (Postgres with pgvector +
 * Kafka), provided by {@link IntegrationTestContainers}.
 *
 * <p>Boots the FULL application context (unlike the unit-scope {@code ContextLoadsTest}, which
 * excludes infra auto-config): Flyway runs the real migrations against the container, JPA validates
 * against the migrated schema, and Kafka is a live broker. This is where the M0 DoD item
 * "Testcontainers integration test that hits Postgres+Kafka" is satisfied.
 *
 * <p>Uses the {@code integration} profile so it does NOT inherit the infra exclusions of the {@code
 * test} profile.
 */
@SpringBootTest
@ActiveProfiles("integration")
public abstract class AbstractIntegrationTest {

  @DynamicPropertySource
  static void wireInfrastructure(DynamicPropertyRegistry registry) {
    var postgres = IntegrationTestContainers.POSTGRES;
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    registry.add(
        "spring.kafka.bootstrap-servers", IntegrationTestContainers.KAFKA::getBootstrapServers);
  }
}
