package com.jairam.aicodeassistant.indexing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.jairam.aicodeassistant.ai.embedding.EmbeddingClient;
import com.jairam.aicodeassistant.ai.embedding.EmbeddingException;
import com.jairam.aicodeassistant.indexing.domain.Chunker;
import com.jairam.aicodeassistant.indexing.domain.IndexJob;
import com.jairam.aicodeassistant.indexing.domain.IndexJobStore;
import com.jairam.aicodeassistant.indexing.domain.IndexStatus;
import com.jairam.aicodeassistant.indexing.domain.RepositoryCloner;
import com.jairam.aicodeassistant.indexing.domain.SourceFile;
import com.jairam.aicodeassistant.retrieval.chunk.ChunkVectorStore;
import com.jairam.aicodeassistant.retrieval.chunk.CodeChunk;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for the indexing saga using in-memory fakes for every port — the full orchestration
 * (claim → clone → chunk → embed → upsert → mark), each step's compensation, idempotency, and the
 * not-claimed no-op — all verified WITHOUT Docker, Kafka, Postgres, JGit, or Ollama.
 */
class IndexingSagaTest {

  private static final UUID REPO = UUID.randomUUID();
  private static final UUID ORG = UUID.randomUUID();
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  private FakeJobStore jobs;
  private FakeCloner cloner;
  private FakeEmbeddingClient embedder;
  private FakeVectorStore vectors;
  private List<Object> published;
  private IndexingSaga saga;

  @BeforeEach
  void setUp() {
    jobs = new FakeJobStore();
    cloner = new FakeCloner();
    embedder = new FakeEmbeddingClient();
    vectors = new FakeVectorStore();
    published = new ArrayList<>();
    ApplicationEventPublisher events = published::add;
    saga = new IndexingSaga(jobs, cloner, Chunker.withDefaults(), embedder, vectors, events, clock);

    // A registered job exists (created on first Kafka delivery).
    jobs.save(IndexJob.register(REPO, ORG, "file:///tmp/repo", "main", clock.instant()));
    cloner.files = List.of(new SourceFile("src/A.java", "line1\nline2\nline3"));
  }

  @Test
  void happyPathIndexesAndPublishesIndexedEvent() {
    IndexingSaga.Outcome outcome = saga.run(REPO);

    assertThat(outcome).isEqualTo(IndexingSaga.Outcome.INDEXED);
    assertThat(jobs.byRepo(REPO).status()).isEqualTo(IndexStatus.INDEXED);
    assertThat(vectors.stored).isNotEmpty();
    assertThat(published).anyMatch(e -> e.getClass().getSimpleName().equals("RepositoryIndexed"));
    assertThat(cloner.closed).as("clone dir cleaned up").isTrue();
  }

  @Test
  void secondRunIsNotClaimed() {
    saga.run(REPO); // first run claims + finishes
    List<Object> firstPublished = new ArrayList<>(published);

    IndexingSaga.Outcome outcome = saga.run(REPO); // job no longer REGISTERED

    assertThat(outcome).isEqualTo(IndexingSaga.Outcome.NOT_CLAIMED);
    assertThat(published).as("no new events on non-claim").hasSameSizeAs(firstPublished);
  }

  @Test
  void cloneFailureCompensatesToFailed() {
    cloner.failWith = new RepositoryCloner.CloneFailedException("boom", null);

    IndexingSaga.Outcome outcome = saga.run(REPO);

    assertThat(outcome).isEqualTo(IndexingSaga.Outcome.FAILED);
    assertThat(jobs.byRepo(REPO).status()).isEqualTo(IndexStatus.FAILED);
    assertThat(jobs.byRepo(REPO).statusDetail()).contains("boom");
    assertThat(published)
        .anyMatch(e -> e.getClass().getSimpleName().equals("RepositoryIndexingFailed"));
  }

  @Test
  void embeddingFailureCompensatesAndCleansVectors() {
    embedder.failWith = new EmbeddingException("provider down");

    IndexingSaga.Outcome outcome = saga.run(REPO);

    assertThat(outcome).isEqualTo(IndexingSaga.Outcome.FAILED);
    assertThat(jobs.byRepo(REPO).status()).isEqualTo(IndexStatus.FAILED);
    // Compensation deleted any partial vectors for this repo.
    assertThat(vectors.stored).isEmpty();
    assertThat(vectors.deletes).as("vectors cleared on failure").isGreaterThanOrEqualTo(1);
  }

  @Test
  void reindexIsIdempotentDeletingBeforeUpsert() {
    saga.run(REPO); // first index
    int afterFirst = vectors.stored.size();

    // Reset the job to REGISTERED to simulate a re-index request.
    IndexJob job = jobs.byRepo(REPO);
    job.transitionTo(IndexStatus.REGISTERED, clock.instant());
    jobs.save(job);

    saga.run(REPO); // re-index

    assertThat(vectors.deletes).as("delete-before-upsert ran each index").isGreaterThanOrEqualTo(2);
    assertThat(vectors.stored).hasSize(afterFirst); // no duplication
  }

  @Test
  void emptyRepositoryStillIndexesWithZeroChunks() {
    cloner.files = List.of();

    IndexingSaga.Outcome outcome = saga.run(REPO);

    assertThat(outcome).isEqualTo(IndexingSaga.Outcome.INDEXED);
    assertThat(jobs.byRepo(REPO).chunkCount()).isZero();
  }

  // --- Fakes -----------------------------------------------------------------

  static final class FakeJobStore implements IndexJobStore {
    private final java.util.Map<UUID, IndexJob> byRepo = new java.util.HashMap<>();

    @Override
    public IndexJob save(IndexJob job) {
      byRepo.put(job.repositoryId(), job);
      return job;
    }

    @Override
    public Optional<IndexJob> findByRepositoryId(UUID repositoryId) {
      return Optional.ofNullable(byRepo.get(repositoryId));
    }

    @Override
    public Optional<IndexJob> claim(UUID repositoryId) {
      IndexJob job = byRepo.get(repositoryId);
      if (job == null || job.status() != IndexStatus.REGISTERED) {
        return Optional.empty();
      }
      job.transitionTo(IndexStatus.CLAIMED, Instant.parse("2026-01-01T00:00:00Z"));
      return Optional.of(job);
    }

    @Override
    public int reapStalledJobs(long staleAfterSeconds) {
      return 0; // not exercised by the saga tests
    }

    IndexJob byRepo(UUID repositoryId) {
      return byRepo.get(repositoryId);
    }
  }

  static final class FakeCloner implements RepositoryCloner {
    List<SourceFile> files = List.of();
    RuntimeException failWith;
    boolean closed;

    @Override
    public ClonedRepository clone(String cloneUrl, String branch) {
      if (failWith != null) {
        throw failWith;
      }
      return new ClonedRepository() {
        @Override
        public List<SourceFile> textFiles() {
          return files;
        }

        @Override
        public void close() {
          closed = true;
        }
      };
    }
  }

  static final class FakeEmbeddingClient implements EmbeddingClient {
    RuntimeException failWith;

    @Override
    public int dimension() {
      return 3;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
      if (failWith != null) {
        throw failWith;
      }
      List<float[]> out = new ArrayList<>();
      for (int i = 0; i < texts.size(); i++) {
        out.add(new float[] {1f, 2f, 3f});
      }
      return out;
    }
  }

  static final class FakeVectorStore implements ChunkVectorStore {
    final List<CodeChunk> stored = new ArrayList<>();
    int deletes = 0;

    @Override
    public void upsertAll(List<CodeChunk> chunks) {
      stored.addAll(chunks);
    }

    @Override
    public int deleteByRepository(UUID repositoryId) {
      int n = stored.size();
      stored.clear();
      deletes++;
      return n;
    }

    @Override
    public long countByRepository(UUID repositoryId) {
      return stored.size();
    }
  }
}
