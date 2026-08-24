package com.jairam.aicodeassistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.support.AbstractIntegrationTest;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * M0 infrastructure smoke test — proves the two load-bearing dependencies are genuinely wired
 * end-to-end against real containers:
 *
 * <ol>
 *   <li>Postgres: Flyway migrations applied, pgvector extension present, and the outbox table
 *       exists.
 *   <li>Kafka: a message can be produced and consumed on the live broker.
 * </ol>
 *
 * <p>This is the concrete realisation of the M0 DoD item requiring a Testcontainers integration
 * test that hits Postgres + Kafka. Requires Docker.
 */
class InfrastructureSmokeIT extends AbstractIntegrationTest {

  @Autowired private DataSource dataSource;

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Test
  void flywayAppliedBaselineAndExtensionsArePresent() throws Exception {
    try (var conn = dataSource.getConnection();
        var stmt = conn.createStatement()) {

      // Flyway history recorded the baseline migration.
      try (var rs =
          stmt.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
      }

      // pgvector extension installed by V1.
      try (var rs =
          stmt.executeQuery("SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
      }

      // Outbox table created by V1.
      try (var rs =
          stmt.executeQuery(
              "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_name = 'event_publication'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt(1)).isEqualTo(1);
      }
    }
  }

  @Test
  void kafkaRoundTripsAMessage() {
    String topic = "m0-smoke-" + UUID.randomUUID();
    String key = "k";
    String value = "hello-aicodeassistant";

    try (KafkaProducer<String, String> producer =
        new KafkaProducer<>(
            Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()))) {
      producer.send(new ProducerRecord<>(topic, key, value));
      producer.flush();
    }

    try (KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(
            Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG,
                "m0-smoke-consumer-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()))) {
      consumer.subscribe(java.util.List.of(topic));

      String received = null;
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
      while (received == null && System.nanoTime() < deadline) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
          received = record.value();
        }
      }
      assertThat(received).isEqualTo(value);
    }
  }
}
