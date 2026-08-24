package com.jairam.aicodeassistant.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the AI Software Engineering Assistant indexing worker (the second deployable).
 *
 * <p>This is a headless (non-web-business) Spring Boot application that consumes Kafka topics and
 * runs the long-running indexing saga. It scans its own package plus the {@code indexing} context
 * and the collaborators the saga needs — {@code ai} (embeddings) and {@code retrieval} (vector
 * store) — and nothing from the monolith's web/API contexts. The {@code platform} shared kernel
 * contributes beans via auto-configuration.
 *
 * <p>This narrow footprint mirrors the build-graph dependency (worker → indexing → ai/retrieval →
 * platform): the seam to run this as a fully separate service already exists at build and runtime.
 */
@SpringBootApplication(
    scanBasePackages = {
      "com.jairam.aicodeassistant.worker",
      "com.jairam.aicodeassistant.indexing",
      "com.jairam.aicodeassistant.ai",
      // Only the retrieval WRITE side (the chunk store) — NOT retrieval.search,
      // whose REST controller needs iam/web that the headless worker does not wire.
      "com.jairam.aicodeassistant.retrieval.chunk"
    })
@EntityScan("com.jairam.aicodeassistant.indexing")
@EnableJpaRepositories("com.jairam.aicodeassistant.indexing")
@EnableScheduling // drives the stalled-index-job reaper
public class IndexerWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(IndexerWorkerApplication.class, args);
  }
}
