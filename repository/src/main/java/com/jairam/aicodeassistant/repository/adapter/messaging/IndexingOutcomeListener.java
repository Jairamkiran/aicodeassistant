package com.jairam.aicodeassistant.repository.adapter.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairam.aicodeassistant.repository.application.RepositoryStatusService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes the worker's indexing-outcome events ({@code repository.indexed} / {@code
 * repository.indexing-failed}) and updates the repository status.
 *
 * <p>Uses local inbound DTOs (not the indexing module's event types) so the repository context
 * stays decoupled from indexing internals — integration is by message contract, not shared classes.
 * Manual ack after the (idempotent) status update; unparseable messages are acked with a loud log.
 */
@Component
class IndexingOutcomeListener {

  private static final Logger log = LoggerFactory.getLogger(IndexingOutcomeListener.class);

  private final RepositoryStatusService statusService;
  private final ObjectMapper objectMapper;

  IndexingOutcomeListener(RepositoryStatusService statusService, ObjectMapper objectMapper) {
    this.statusService = statusService;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(topics = "repository.indexed", groupId = "aicodeassistant-app")
  void onIndexed(String payload, Acknowledgment ack) {
    Indexed event = parse(payload, Indexed.class, ack);
    if (event != null) {
      statusService.markIndexed(event.repositoryId(), event.chunkCount());
      ack.acknowledge();
    }
  }

  @KafkaListener(topics = "repository.indexing-failed", groupId = "aicodeassistant-app")
  void onFailed(String payload, Acknowledgment ack) {
    Failed event = parse(payload, Failed.class, ack);
    if (event != null) {
      statusService.markFailed(event.repositoryId(), event.reason());
      ack.acknowledge();
    }
  }

  private <T> T parse(String payload, Class<T> type, Acknowledgment ack) {
    try {
      return objectMapper.readValue(payload, type);
    } catch (Exception e) {
      log.error("Discarding unparseable {} message: {}", type.getSimpleName(), e.getMessage());
      ack.acknowledge();
      return null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Indexed(UUID repositoryId, int chunkCount) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Failed(UUID repositoryId, String reason) {}
}
