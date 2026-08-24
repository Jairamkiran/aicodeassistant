package com.jairam.aicodeassistant.indexing.application;

import com.jairam.aicodeassistant.ai.embedding.EmbeddingClient;
import com.jairam.aicodeassistant.ai.embedding.EmbeddingException;
import com.jairam.aicodeassistant.indexing.domain.Chunk;
import com.jairam.aicodeassistant.indexing.domain.Chunker;
import com.jairam.aicodeassistant.indexing.domain.IndexJob;
import com.jairam.aicodeassistant.indexing.domain.IndexJobStore;
import com.jairam.aicodeassistant.indexing.domain.IndexStatus;
import com.jairam.aicodeassistant.indexing.domain.RepositoryCloner;
import com.jairam.aicodeassistant.indexing.domain.RepositoryIndexed;
import com.jairam.aicodeassistant.indexing.domain.RepositoryIndexingFailed;
import com.jairam.aicodeassistant.indexing.domain.SourceFile;
import com.jairam.aicodeassistant.retrieval.chunk.ChunkVectorStore;
import com.jairam.aicodeassistant.retrieval.chunk.CodeChunk;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the repository indexing saga:
 *
 * <pre>
 *   claim → clone → parse+chunk → embed → upsert → mark INDEXED
 * </pre>
 *
 * <p><b>Exactly-once:</b> the saga begins by atomically {@link IndexJobStore#claim claiming} the
 * job (REGISTERED → CLAIMED). If the claim is not won — a duplicate Kafka delivery, or a concurrent
 * worker — it returns immediately without side-effects. This replaces a distributed lock
 * (ADR-0009).
 *
 * <p><b>Compensation:</b> any failure marks the job FAILED with a reason, deletes any
 * partially-written vectors (so a retry is clean), and publishes {@link RepositoryIndexingFailed}.
 * Success publishes {@link RepositoryIndexed}. The clone's working directory is always cleaned up
 * (try-with-resources).
 *
 * <p><b>Idempotency:</b> vectors are deleted before upsert, so re-running a job (after a transient
 * failure) does not duplicate chunks.
 *
 * <p>An explicit orchestrator is used rather than a state-machine framework — the flow is linear
 * and the framework would be over-engineering here.
 */
@Service
public class IndexingSaga {

  private static final Logger log = LoggerFactory.getLogger(IndexingSaga.class);

  private final IndexJobStore jobs;
  private final RepositoryCloner cloner;
  private final Chunker chunker;
  private final EmbeddingClient embeddingClient;
  private final ChunkVectorStore vectorStore;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public IndexingSaga(
      IndexJobStore jobs,
      RepositoryCloner cloner,
      Chunker chunker,
      EmbeddingClient embeddingClient,
      ChunkVectorStore vectorStore,
      ApplicationEventPublisher events,
      Clock clock) {
    this.jobs = jobs;
    this.cloner = cloner;
    this.chunker = chunker;
    this.embeddingClient = embeddingClient;
    this.vectorStore = vectorStore;
    this.events = events;
    this.clock = clock;
  }

  /**
   * Runs the saga for a repository's index job.
   *
   * @return the terminal outcome (won-and-finished, or not-claimed)
   */
  public Outcome run(UUID repositoryId) {
    IndexJob job = jobs.claim(repositoryId).orElse(null);
    if (job == null) {
      // Already claimed/processed by another delivery or worker — no-op.
      log.debug(
          "Index job for repository {} not claimable (already in progress/done)", repositoryId);
      return Outcome.NOT_CLAIMED;
    }
    log.info("Claimed index job {} for repository {}", job.id(), repositoryId);

    // Idempotency: clear any vectors from a previous partial run before we start.
    vectorStore.deleteByRepository(repositoryId);

    try (RepositoryCloner.ClonedRepository cloned = cloneStep(job)) {
      List<Chunk> chunks = parseStep(job, cloned.textFiles());
      List<CodeChunk> embedded = embedStep(job, chunks);
      upsertStep(job, embedded);

      job.markIndexed(embedded.size(), clock.instant());
      jobs.save(job);
      events.publishEvent(RepositoryIndexed.of(repositoryId, embedded.size(), clock.instant()));
      log.info("Indexed repository {} — {} chunks", repositoryId, embedded.size());
      return Outcome.INDEXED;
    } catch (RuntimeException e) {
      compensate(job, e);
      return Outcome.FAILED;
    }
  }

  private RepositoryCloner.ClonedRepository cloneStep(IndexJob job) {
    advance(job, IndexStatus.CLONING);
    return cloner.clone(job.cloneUrl(), job.defaultBranch());
  }

  private List<Chunk> parseStep(IndexJob job, List<SourceFile> files) {
    advance(job, IndexStatus.PARSING);
    return chunker.chunkAll(files);
  }

  private List<CodeChunk> embedStep(IndexJob job, List<Chunk> chunks) {
    advance(job, IndexStatus.EMBEDDING);
    if (chunks.isEmpty()) {
      return List.of();
    }
    List<String> texts = chunks.stream().map(Chunk::content).toList();
    List<float[]> vectors = embeddingClient.embedAll(texts);
    if (vectors.size() != chunks.size()) {
      throw new EmbeddingException(
          "vector count " + vectors.size() + " != chunk count " + chunks.size());
    }
    List<CodeChunk> result = new ArrayList<>(chunks.size());
    for (int i = 0; i < chunks.size(); i++) {
      Chunk c = chunks.get(i);
      result.add(
          new CodeChunk(
              job.repositoryId(),
              job.organizationId(),
              c.filePath(),
              c.language(),
              c.startLine(),
              c.endLine(),
              c.content(),
              vectors.get(i)));
    }
    return result;
  }

  private void upsertStep(IndexJob job, List<CodeChunk> embedded) {
    advance(job, IndexStatus.UPSERTING);
    vectorStore.upsertAll(embedded);
  }

  private void advance(IndexJob job, IndexStatus status) {
    job.transitionTo(status, clock.instant());
    jobs.save(job);
  }

  private void compensate(IndexJob job, RuntimeException failure) {
    Instant now = clock.instant();
    String reason = failure.getClass().getSimpleName() + ": " + failure.getMessage();
    log.warn(
        "Indexing failed for repository {} at {}: {}", job.repositoryId(), job.status(), reason);
    try {
      // Roll back any partial vector writes so a later retry starts clean.
      vectorStore.deleteByRepository(job.repositoryId());
    } catch (RuntimeException cleanupError) {
      log.error("Compensation cleanup failed for repository {}", job.repositoryId(), cleanupError);
    }
    job.markFailed(reason, now);
    jobs.save(job);
    events.publishEvent(RepositoryIndexingFailed.of(job.repositoryId(), reason, now));
  }

  /** Terminal outcome of a saga run. */
  public enum Outcome {
    INDEXED,
    FAILED,
    NOT_CLAIMED
  }
}
