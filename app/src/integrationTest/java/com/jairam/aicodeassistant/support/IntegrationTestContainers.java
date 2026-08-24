package com.jairam.aicodeassistant.support;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Testcontainers shared across the whole integration-test suite.
 *
 * <p>Containers are expensive to start, so we use the "singleton container" pattern: they are
 * created once per JVM and reused by every {@code @SpringBootTest} (Spring's context cache keeps
 * the same context, and the containers outlive individual test classes). They are NOT stopped
 * explicitly — Testcontainers' Ryuk sidecar reaps them when the JVM exits.
 *
 * <p>Requires a running Docker daemon. On machines without Docker these tests are not part of
 * {@code ./gradlew test}; they run under {@code ./gradlew integrationTest} only.
 */
public final class IntegrationTestContainers {

  /** Postgres image with the pgvector extension preinstalled (needed for M4/M5). */
  private static final DockerImageName POSTGRES_IMAGE =
      DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

  // Modern Testcontainers Kafka module uses the native apache/kafka image
  // (KRaft mode, no Zookeeper) — matches our docker-compose broker.
  private static final DockerImageName KAFKA_IMAGE =
      DockerImageName.parse("apache/kafka-native:3.8.0");

  public static final PostgreSQLContainer<?> POSTGRES;
  public static final KafkaContainer KAFKA;

  static {
    POSTGRES =
        new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("aicodeassistant")
            .withUsername("aicodeassistant")
            .withPassword("aicodeassistant")
            .withReuse(true);
    KAFKA = new KafkaContainer(KAFKA_IMAGE).withReuse(true);

    POSTGRES.start();
    KAFKA.start();
  }

  private IntegrationTestContainers() {}
}
