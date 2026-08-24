package com.jairam.aicodeassistant.retrieval.chunk;

import java.util.List;
import java.util.UUID;

/**
 * Write side of the code-chunk vector store (Milestone 4). The indexing saga upserts embedded
 * chunks here; the query/search side arrives in M5.
 *
 * <p>Public port so the indexing module (and tests) depend on the interface, not the pgvector
 * adapter.
 */
public interface ChunkVectorStore {

  /** Persists a batch of embedded chunks. */
  void upsertAll(List<CodeChunk> chunks);

  /** Removes all chunks for a repository (called before a re-index for idempotency). */
  int deleteByRepository(UUID repositoryId);

  /** Counts stored chunks for a repository (used by tests / status). */
  long countByRepository(UUID repositoryId);
}
