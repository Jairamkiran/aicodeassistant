package com.jairam.aicodeassistant.indexing.adapter.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairam.aicodeassistant.indexing.application.IndexingSaga;
import com.jairam.aicodeassistant.indexing.domain.IndexJob;
import com.jairam.aicodeassistant.indexing.domain.IndexJobStore;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code repository.import-requested} (externalized by the repository context via the
 * outbox) and runs the indexing saga.
 *
 * <p>Reliability: manual acknowledgement — we ack only after the saga has reached a terminal state
 * (INDEXED/FAILED/NOT_CLAIMED), each of which is durably persisted, so redelivery is safe (the
 * atomic claim makes reprocessing a no-op). A malformed/poison message is acked after logging
 * rather than blocking the partition; a real DLQ topic is wired via the container factory in {@code
 * WorkerKafkaConfig} (see M4 doc "recovery").
 */
@Component
public class ImportRequestedListener {

  private static final Logger log = LoggerFactory.getLogger(ImportRequestedListener.class);

  private final IndexingSaga saga;
  private final IndexJobStore jobs;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ImportRequestedListener(
      IndexingSaga saga, IndexJobStore jobs, ObjectMapper objectMapper, Clock clock) {
    this.saga = saga;
    this.jobs = jobs;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @KafkaListener(topics = "repository.import-requested", groupId = "aicodeassistant-indexer-worker")
  public void onImportRequested(String payload, Acknowledgment ack) {
    ImportRequested event;
    try {
      event = objectMapper.readValue(payload, ImportRequested.class);
    } catch (Exception e) {
      // Poison message: cannot even parse it. Ack to avoid blocking; log loudly.
      log.error("Discarding unparseable import-requested message: {}", e.getMessage());
      ack.acknowledge();
      return;
    }

    try {
      ensureJob(event);
      IndexingSaga.Outcome outcome = saga.run(event.repositoryId());
      log.info("Indexing saga for repository {} finished: {}", event.repositoryId(), outcome);
      ack.acknowledge();
    } catch (RuntimeException e) {
      // The saga compensates internally; reaching here means an unexpected error
      // outside the saga's own handling. Do NOT ack → Kafka redelivers; the
      // atomic claim keeps redelivery safe.
      log.error("Unexpected failure handling repository {}", event.repositoryId(), e);
      throw e;
    }
  }

  /** Creates the REGISTERED index job if this is the first delivery for the repo. */
  private void ensureJob(ImportRequested event) {
    if (jobs.findByRepositoryId(event.repositoryId()).isEmpty()) {
      jobs.save(
          IndexJob.register(
              event.repositoryId(),
              event.organizationId(),
              event.cloneUrl(),
              event.defaultBranch() == null ? "main" : event.defaultBranch(),
              clock.instant()));
    }
  }

  /** Inbound payload mirror of the repository context's externalized event. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record ImportRequested(
      UUID repositoryId, UUID organizationId, String cloneUrl, String defaultBranch) {}
}
