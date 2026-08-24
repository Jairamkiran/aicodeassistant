/**
 * Indexing bounded context — the repository indexing saga (owned by the indexer-worker deployable).
 *
 * <p>Orchestrates claim → clone → parse → chunk → embed → upsert with compensation. It depends on
 * the public APIs of {@code ai} (embeddings) and {@code retrieval} (vector store), declared as
 * explicit allowed dependencies and verified by the modularity test. It owns its own {@code
 * index_jobs} table and never writes another context's tables — it emits completion events instead.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Indexing",
    allowedDependencies = {"ai :: embedding", "retrieval :: chunk", "platform"})
package com.jairam.aicodeassistant.indexing;
